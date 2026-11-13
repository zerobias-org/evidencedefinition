# Content Attribution Notices

Some evidence definition packages in this repository reproduce content whose
source material is owned by third parties and used under their published
licenses. This file records the attribution those licenses require. It travels
with the repository; the same notices apply to the npm packages published
from it.

Each such package declares its content's license in the standard `"license"`
field of `package.json` (for example `CC-BY-ND-4.0` on the SCF packages).
**The sections below are authoritative for every package, whatever its
`package.json` currently says.**

---

## Secure Controls Framework (SCF)

**Applies to:** every package under `package/scf/`.

These packages reproduce the **Secure Controls Framework (SCF)** Evidence
Request List: evidence request identifiers, artifact names and artifact
descriptions, published by the Secure Controls Framework Council, LLC.

- **Source:** [Secure Controls Framework](https://securecontrolsframework.com/),
  the "Evidence Request List" sheet of the officially distributed SCF spreadsheet
  ([securecontrolsframework/securecontrolsframework](https://github.com/securecontrolsframework/securecontrolsframework))
- **Copyright:** © Secure Controls Framework Council, LLC
- **License:** [Creative Commons Attribution-NoDerivatives 4.0 International (CC BY-ND 4.0)](https://creativecommons.org/licenses/by-nd/4.0/)
- **Changes:** each Evidence Request List row has been converted from the
  officially distributed spreadsheet format into a YAML evidence definition for
  machine consumption. The SCF content itself is reproduced verbatim and
  unmodified. No evidence requests have been added, removed, or edited.

SCF content is used in this solution. This repository and the ZeroBias platform
are not endorsed by, affiliated with, or sponsored by the Secure Controls
Framework Council, LLC.

Per the CC BY-ND 4.0 license, this content may be redistributed verbatim with
this attribution, but modified or derivative versions of the SCF content may
not be distributed. Do not edit the content of these packages by hand;
regenerate them from the officially published SCF spreadsheet with
`package/scf/update/generate.mjs` instead.
