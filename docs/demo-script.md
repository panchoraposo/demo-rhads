# Live demo script (~25–30 min)

Shared password: `backstage`. Keep the dashboard `https://dashboard.apps.<cluster>/` open in a support tab.

## 0. Context (2 min)

Supply chain: **who** built **what**, **with which dependencies**, **signed by whom**, **promoted under which policy**. RHADS joins the portal (RHDH), signing (RHTAS), inventory (TPA), policy (Conforma), and runtime (ACS) on GitOps.

## 1. Portal — create the app (4 min)

1. Developer Hub → Keycloak OIDC login as **`dev1`**.
2. Create → **Agentic app — Quarkus MCP** (group `developers`, Quay already filled in).
3. Wait for the scaffolder to publish two repos and the Argo CD app-of-apps (`{app}-build`, `{app}-dev`, `{app}-staging`, `{app}-prod`).
4. The GitOps webhook job registers the GitLab hook **and immediately triggers** the first build (the scaffolder commit landed before the hook existed). That unsigned run loads the **first SBOM into TPA**, so it is there when Dev Spaces opens.

## 2. Inner loop in Dev Spaces (6 min)

Start the workspace **before the audience is in the room**. The first start pulls the Universal Developer Image and che-code (several GB) and can take many minutes on a single-node sandbox. After that, the workspace stays running (idling is disabled) so opening it from the catalog is seconds, not minutes.

1. On the catalog component, **OpenShift Dev Spaces (VS Code)** — reopen the workspace that is already Running.
2. Open the manifest → command palette → **Red Hat Dependency Analytics**. Templates use **previous** Red Hat runtimes so TPA/RHDA have findings: Quarkus/Camel **3.20.6.redhat-00004** (n-1 of 3.27/3.33), OpenJDK **ubi8/openjdk-17:1.16**, Node.js **ubi9/nodejs-18:1-108**, plus **commons-text 1.9** / **snakeyaml 1.33** / **express 4.18.2** / **lodash 4.17.20**.
3. In another tab, **Trusted Profile Analyzer** (catalog link) and show the SBOM from the first build: those packages and CVEs.
4. Change a string in a `@Tool` or in `index.html`.
5. Command palette → **Configure Sigstore git commit signing (gitsign + RHTAS TUF)** (once per workspace). That installs gitsign, sets `dev1@rhads.demo`, and initializes the cluster TUF root.
6. In the terminal: `git add -A && git commit -m "demo: signed change from Dev Spaces"`. Dev Spaces has no `xdg-open` — copy the printed URL, log in as **`dev1` / `backstage`**, paste the verification code.
7. Command palette → **Verify Sigstore-signed HEAD (gitsign + RHTAS TUF)**. Then `git push origin main`.
8. The GitLab webhook starts the pipeline on that signed commit.

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
| conforma-dev | Enterprise Contract `@redhat` + `@slsa3` (report; not STRICT) |
| update-gitops-dev | GitOps `dev` |
| chains-status (finally) | Tekton Chains keyless SLSA (Fulcio + Rekor) |

Open Quay: SHA tag, `.sig`, SBOM. Rekor Search UI (`https://rekor-search-ui-trusted-artifact-signer.apps.<cluster>/`): UUID from the pipeline, email, or digest. OpenShift → Builds: the `{app}-img` BuildConfig.

## 4. Promote to staging (3 min)

GitLab → Repository → Tags → `v1.0.0` on the commit **already built** (the GitLab tag starts the pipeline; the image stays tagged with the SHA).

`{app}-promote` overlay `staging`: ACS → **Conforma STRICT** → GitOps → comment on the GitLab commit.

## 5. Promote to production (3 min)

GitLab → Deployments → Releases → Release `v1.0.0`.

Same pipeline, overlay `prod`, Conforma STRICT. Prod route and Topology in RHDH.

## 6. Wrap-up (2 min)

Nexus (`admin` / `admin123`) shows the Maven cache. Chains is in `TektonConfig/config`: `signers.x509.fulcio.enabled` plus Rekor `transparency.url` (no long-lived signing key). Conforma evaluates the documented Red Hat and SLSA collections. Platform engineers evolve templates and policies; developers never touch signing or ACS YAML.
