import com.zerobias.buildtools.content.SchemaPrimitives

plugins {
    id("zb.workspace")
}

group = "com.zerobias.content"

// ════════════════════════════════════════════════════════════
// Evidence definition content validator — owned by this repo.
//
// Philosophy (per Chris/Kevin): the dataloader is the source of truth
// for schema rules (UUID format, code, status and category enums, tag
// ids, etc.). Re-validating those here just creates drift risk — when
// the dataloader tightens a rule, the gate gets stale. The full schema
// is exercised by testIntegrationDataloader against an ephemeral Neon
// branch during gate. See com/platform/dataloader/src/processors/
// evidencedefinition/.
//
// Package shape:
//   package/<vendor>/<code>/
//     index.yml      — the evidence definition (id, code, name, status, category, ...)
//
// This validator only enforces things the dataloader CANNOT or DOES NOT
// check:
//
//   1. Filesystem ↔ npm ↔ zerobias-block triangulation:
//        dir              = package/<vendor>/<code>/
//        npm name         = @zerobias-org/evidencedefinition-<vendor>-<code>
//        zerobias.package = <vendor>.<code>.evidencedefinition
//
//   2. Required files exist (index.yml, package.json, .npmrc).
//
//   3. Repo-wide unique `id` UUIDs across every index.yml (separate
//      :validateUniqueIds task). The dataloader processes one artifact at
//      a time, so a collision only surfaces when the second tries to
//      overwrite the first's DB row.
//
// Everything else delegated to the dataloader in testIntegrationDataloader.
// ════════════════════════════════════════════════════════════
extra["contentValidator"] = { proj: org.gradle.api.Project ->
    val projectDir = proj.projectDir
    val tag = "[evidencedefinition-validator] ${proj.path}"

    require(projectDir.resolve("index.yml").isFile)    { "$tag index.yml missing in ${projectDir.path}" }
    require(projectDir.resolve("package.json").isFile) { "$tag package.json missing in ${projectDir.path}" }
    require(projectDir.resolve(".npmrc").isFile)       { "$tag .npmrc missing in ${projectDir.path}" }

    // ── Filesystem ↔ npm ↔ zerobias-block triangulation ──
    val code = projectDir.name
    val vendor = projectDir.parentFile.name
    val pkgDoc = SchemaPrimitives.parseJson(projectDir.resolve("package.json"))
    SchemaPrimitives.requirePackageIdentity(
        pkgDoc,
        expectedNpmName = "@zerobias-org/evidencedefinition-$vendor-$code",
        expectedZerobiasPackage = "$vendor.$code.evidencedefinition",
        field = "$tag package.json",
    )
    require(SchemaPrimitives.getPath(pkgDoc, "zerobias.import-artifact") == "evidencedefinition") {
        "$tag zerobias.import-artifact must be 'evidencedefinition'"
    }

    proj.logger.lifecycle("$tag: vendor=$vendor code=$code")
}

// ════════════════════════════════════════════════════════════
// :validateUniqueIds — repo-wide cross-cut over every index.yml id UUID.
// ════════════════════════════════════════════════════════════
val validateUniqueIds by tasks.registering {
    group = "verification"
    description = "Fail if two evidence definitions share the same index.yml id UUID"

    val packageDir = layout.projectDirectory.dir("package").asFile
    inputs.files(
        fileTree(packageDir) {
            include("**/index.yml")
            exclude("**/node_modules/**")
        }
    )

    doLast {
        val byId = mutableMapOf<String, MutableList<String>>()
        if (packageDir.exists()) {
            packageDir.walkTopDown()
                .onEnter { it.name != "node_modules" }
                .filter { it.isFile && it.name == "index.yml" }
                .forEach { f ->
                    val doc = try {
                        SchemaPrimitives.parseYaml(f)
                    } catch (e: Exception) {
                        logger.warn("[validateUniqueIds] skipping unparseable ${f.relativeTo(rootDir)}: ${e.message}")
                        return@forEach
                    }
                    val id = (doc["id"] as? String)?.lowercase() ?: return@forEach
                    byId.getOrPut(id) { mutableListOf() }.add(f.relativeTo(rootDir).path)
                }
        }

        val collisions = byId.filterValues { it.size > 1 }
        if (collisions.isNotEmpty()) {
            val report = collisions.entries.joinToString("\n") { (id, paths) ->
                "  $id\n    " + paths.joinToString("\n    ")
            }
            throw GradleException("[validateUniqueIds] duplicate evidence definition ids across the repo:\n$report")
        }
        logger.lifecycle("[validateUniqueIds] ${byId.size} unique ids across ${byId.values.sumOf { it.size }} definitions")
    }
}

subprojects {
    tasks.matching { it.name == "validateContent" }.configureEach {
        dependsOn(rootProject.tasks.named("validateUniqueIds"))
    }
}

val projectPaths by tasks.registering {
    group = "info"
    description = "Output project-to-directory mappings for tooling (used by zbb CLI)"
    doLast {
        subprojects.filter { it.buildFile.exists() }.forEach { p ->
            println("${p.path}=${p.projectDir.relativeTo(rootDir)}")
        }
    }
}

val changedModules by tasks.registering {
    group = "info"
    description = "List evidence definition packages changed since last version tag"
    doLast {
        val lastTag = try {
            providers.exec { commandLine("git", "describe", "--tags", "--abbrev=0") }
                .standardOutput.asText.get().trim()
        } catch (e: Exception) {
            logger.warn("No version tags found -- listing all packages as changed")
            null
        }

        val diffArgs = if (lastTag != null) listOf("git", "diff", "--name-only", lastTag, "HEAD")
                       else listOf("git", "ls-files")

        val result = providers.exec { commandLine(diffArgs) }.standardOutput.asText.get()

        val packageDir = rootDir.resolve("package")
        val changed = mutableSetOf<String>()
        result.lines()
            .filter { it.startsWith("package/") }
            .forEach { line ->
                var dir = rootDir.resolve(line).parentFile
                while (dir != null && dir != packageDir && dir.startsWith(packageDir)) {
                    if (dir.resolve("build.gradle.kts").isFile) {
                        changed.add(dir.relativeTo(packageDir).path)
                        break
                    }
                    dir = dir.parentFile
                }
            }
        changed.forEach { println(it) }
    }
}
