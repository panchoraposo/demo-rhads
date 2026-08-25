# Live demo script (~25–30 min)

Shared password: `backstage`. Keep the dashboard `https://dashboard.apps.<cluster>/` open in a support tab.

## 0. Context (2 min)

Supply chain: **who** built **what**, **with which dependencies**, **signed by whom**, **promoted under which policy**. RHADS joins the portal (RHDH), signing (RHTAS), inventory (TPA), policy (Conforma), and runtime (ACS) on GitOps.

## 1. Portal — create the app (4 min)

1. Developer Hub → Keycloak OIDC login as **`dev1`**.
2. Create → **Agentic app — Quarkus MCP** (group `developers`, Quay already filled in).
3. Wait for the scaffolder to publish two repos and the Argo CD app-of-apps (`{app}-build`, `{app}-dev`, `{app}-staging`, `{app}-prod`).
4. The initial push starts the build pipeline (commit **without** gitsign). That loads the **first SBOM into TPA**.

## 2. Inner loop in Dev Spaces (6 min)

1. On the catalog component, **OpenShift Dev Spaces (VS Code)**.
2. Open `pom.xml` → command palette → **Red Hat Dependency Analytics** (Maven CVEs; libraries come from Nexus).
3. In another tab, **Trusted Profile Analyzer** (catalog link) and show the SBOM from the first build.
4. Change a string in a `@Tool` or in `index.html`.
5. `git commit` — the workspace is configured with **gitsign** (same Fulcio/Rekor trust root as **cosign**). Complete the OIDC login. `git push` to `main`.
6. The GitLab webhook starts the pipeline on that signed commit.

## 3. Build pipeline (8 min)

In RHDH (Tekton tab) or OpenShift → Pipelines. Each task prints an `[SSSC]` block:

| Task | Security control |
| --- | --- |
| git-clone | source from GitLab |
| gitsign-verify | Sigstore commit signature (Fulcio + Rekor) |
| build-source | Maven/npm **only** against Nexus (preloaded / proxied libs) |
| openshift-build | image with **OpenShift Builds** (Docker BuildConfig) → Quay |
| generate-sbom | Syft CycloneDX + SPDX inventory |
| sign-image | cosign + Rekor |
| attest-sbom | SBOM bound to the digest (`.att`) |
| acs-scan / acs-check | ACS CVEs and policies |
| upload-tpa | searchable SBOM in TPA |
| conforma-dev | Enterprise Contract (report; not STRICT) |
| update-gitops-dev | GitOps `dev` |
| chains-status (finally) | Tekton Chains / SLSA provenance |

Open Quay: SHA tag, `.sig`, SBOM. Rekor: UUID. OpenShift → Builds: the `{app}-img` BuildConfig.

## 4. Promote to staging (3 min)

GitLab → Repository → Tags → `v1.0.0` on the commit **already built** (the GitLab tag starts the pipeline; the image stays tagged with the SHA).

`{app}-promote` overlay `staging`: ACS → **Conforma STRICT** → GitOps → comment on the GitLab commit.

## 5. Promote to production (3 min)

GitLab → Deployments → Releases → Release `v1.0.0`.

Same pipeline, overlay `prod`, Conforma STRICT. Prod route and Topology in RHDH.

## 6. Wrap-up (2 min)

Nexus (`admin` / `admin123`) shows the Maven cache. Chains is in `TektonConfig/config` (`spec.chain.disabled: false`). Platform engineers evolve templates and policies; developers never touch signing or ACS YAML.
