# Evidence Definition mono-repo

Monorepo of open-source ZeroBias evidence definitions, published under the `@zerobias-org` npm scope.

This repo starts empty. Packages are added one at a time after a licensing check.

Open pull requests against the `dev` branch.

## Package layout

Each package lives at `package/<vendor>/<code>/` and needs `index.yml`, `package.json`, `.npmrc` and a one-line `build.gradle.kts` containing `plugins { id("zb.content") }`. Start from the files in `templates/`.

The npm name is `@zerobias-org/evidencedefinition-<vendor>-<code>` and `zerobias.package` is `<vendor>.<code>.evidencedefinition`.

## Build and publish

Run `./gradlew :<vendor>:<code>:gate` and commit the resulting `gate-stamp.json` with the package. Pushes to `main`, `uat`, `qa` and `dev` publish changed packages through `.github/workflows/publish.yml`.
