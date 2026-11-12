# Evidence Definition mono-repo

Monorepo of open-source ZeroBias evidence definitions, published under the `@zerobias-org` npm scope.

This repo starts empty. Packages are added one at a time after a licensing check.

Open pull requests against the `dev` branch.

## Package layout

Each package lives at `package/<vendor>/<code>/` and needs `index.yml`, `package.json`, `.npmrc` and a one-line `build.gradle.kts` containing `plugins { id("zb.content") }`. Start from the files in `templates/`.

The npm name is `@zerobias-org/evidencedefinition-<vendor>-<code>` and `zerobias.package` is `<vendor>.<code>.evidencedefinition`.

Definitions generated from a versioned source live at `package/<vendor>/<version>/<code>/`, with npm name `@zerobias-org/evidencedefinition-<vendor>-<version>-<code>` and `zerobias.package` `<vendor>.<version, dots as underscores>.<code>.evidencedefinition`. SCF reuses evidence codes for different evidence across releases, so every SCF version gets its own packages and ids.

## SCF evidence definitions

`package/scf/<version>/` is generated from the "Evidence Request List" sheet of the SCF workbook. Do not edit it by hand:

```bash
cd package/scf/update && npm install
node generate.mjs <path/to/secure-controls-framework-scf-<version>.xlsx>
```

Ids are uuidv5 of `scf:<version>:<CODE>`, so reruns are stable. A rerun rewrites `index.yml` but never an existing `package.json`.

## Build and publish

Commit the package first, since the gate stamp hashes committed files. Then run `./gradlew :<vendor>:<code>:gate` and commit the resulting `gate-stamp.json` with the package. Pushes to `main`, `uat`, `qa` and `dev` publish changed packages through `.github/workflows/publish.yml`.
