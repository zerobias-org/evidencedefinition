// settings.gradle.kts — evidencedefinition monorepo
//
// Plugin resolution order: mavenLocal (for `publishToMavenLocal` dev builds
// of build-tools) → GitHub Packages Maven → gradle plugin portal → mavenCentral.
// Never via `includeBuild` of a sibling repo path: dev iteration goes through
// `./gradlew publishToMavenLocal` from build-tools so CI and local resolve
// the artifact the same way.

pluginManagement {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/zerobias-org/util")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "zerobias-org"
                password = System.getenv("READ_TOKEN") ?: System.getenv("NPM_TOKEN") ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("zb.workspace") version "1.+"
        id("zb.base") version "1.+"
        id("zb.content") version "1.+"
    }
}

rootProject.name = "evidencedefinitions"

// Auto-discover evidence definition packages under package/ —
// package/<vendor>/<code>/ (depth 2, e.g. package/zerobias/bcd-d-bcp/).
// Project paths mirror the filesystem: package/zerobias/bcd-d-bcp →
// :zerobias:bcd-d-bcp.
val packageDir = file("package")
if (packageDir.exists()) {
    packageDir.walkTopDown()
        .filter { it.name == "build.gradle.kts" }
        .forEach { buildFile ->
            val moduleDir = buildFile.parentFile
            val relativePath = moduleDir.relativeTo(packageDir).path
            val projectPath = relativePath.replace(File.separatorChar, ':')

            include(projectPath)
            project(":$projectPath").projectDir = moduleDir
        }
}
