# Live demo script (~25–30 min)

Shared password: `backstage`. Keep the dashboard `https://dashboard.apps.<cluster>/` open in a support tab.

## 0. Context (2 min)

Supply chain: **who** built **what**, **with which dependencies**, **signed by whom**, **promoted under which policy**. RHADS joins the portal (RHDH), signing (RHTAS), inventory (TPA), policy (Conforma), and runtime (ACS) on GitOps.

This cluster was installed from GitHub (`./install.sh`). GitLab holds the live GitOps copy; the next sandbox only sees what is on GitHub `main`.

## 1. Portal — create the app (4 min)

1. Developer Hub → Keycloak OIDC login as **`dev1`**.
2. Create → **Agentic app — Quarkus MCP** (group `developers`, Quay already filled in). Name **at most 18 characters** — leave the default `quarkus-agent`. For Miles of Smiles HITL + MaaS instead, Create → **Agentic app — Quarkus HITL** (`miles-smiles`) and paste the MaaS API key.
3. Wait for the scaffolder to publish two repos and the Argo CD app-of-apps (`{app}-build`, `{app}-dev`, `{app}-staging`, `{app}-prod`).
4. The GitOps webhook Job registers the GitLab hook, creates `rhads/{app}` in Quay, and **immediately triggers** the first build (the scaffolder commit landed before the hook existed). That unsigned run loads the **first SBOM into TPA**, so it is there when Dev Spaces opens. Task `gitsign-verify` prints a WARN and still PASSes.

## 2. Inner loop in Dev Spaces (6 min)

Start the workspace **before the audience is in the room**. The first start pulls the Universal Developer Image and che-code (several GB) and can take many minutes on a single-node sandbox. After that, the workspace stays running (idling is disabled) so opening it from the catalog is seconds, not minutes.

**HITL + MaaS (Miles of Smiles):** command palette → **Write .env for Red Hat MaaS** (if the API key was not set at scaffold) → **Quarkus dev (fleet UI + Dev UI on 8080)**. Open the workspace endpoints **fleet-ui** and **quarkus-dev-ui** (`/q/dev-ui`). Return Civic `#7` (or a Mercedes) with a collision prompt; **Approval Needed** appears when estimated value is above $15,000. Ford Focus `#5` usually skips HITL.

1. On the catalog component, **OpenShift Dev Spaces (VS Code)** — reopen the workspace that is already Running.
2. Open the manifest → command palette → **Red Hat Dependency Analytics**. Templates use **previous** Red Hat runtimes so TPA/RHDA have findings: Quarkus/Camel **3.20.6.redhat-00004** (n-1 of 3.27/3.33), OpenJDK **ubi8/openjdk-17:1.16**, Node.js **ubi9/nodejs-18:1-108**, plus **commons-text 1.9** / **snakeyaml 1.33** / **express 4.18.2** / **lodash 4.17.20**. RHDA talks to the **in-cluster TPA**, not Red Hat SaaS.
3. In another tab, **Trusted Profile Analyzer** (catalog link) and show the SBOM from the first build: those packages and CVEs.
4. Do **not** delete the intentional CVE deps — `CustomerTools` imports `org.apache.commons.text.StringEscapeUtils`. In `pom.xml` **bump** them so TPA/RHDA findings drop and Maven still compiles:

```xml
        <!-- Fixed: CVE-2022-42889 (Text4Shell) and CVE-2022-1471. -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>1.13.1</version>
        </dependency>
        <dependency>
            <groupId>org.yaml</groupId>
            <artifactId>snakeyaml</artifactId>
            <version>2.3</version>
        </dependency>
```

Nexus already proxies those artifacts (`1.9`/`1.33` stay for the first SBOM).
5. Command palette → **Configure Sigstore git commit signing (gitsign + RHTAS TUF)** (once per workspace). That installs gitsign, sets author `dev1@rhads.demo`, and initializes the **cluster TUF root** (private Fulcio is not in the public Sigstore TUF).
6. In the terminal: `git add -A && git commit -m "demo: signed change from Dev Spaces"`. Dev Spaces has no `xdg-open` — copy the printed URL, log in as **`dev1` / `backstage`**, paste the verification code. The Fulcio certificate identity is the Keycloak email, not `dev1@rhads.com`.
7. Command palette → **Verify Sigstore-signed HEAD (gitsign + RHTAS TUF)**. Then `git push origin main`.
8. The GitLab webhook starts the pipeline on that signed commit. `gitsign-verify` now PASSes for real.

## 3. Build pipeline (8 min)

In RHDH (CI / Tekton tab) or OpenShift → Pipelines. Each task prints an `[SSSC]` block:

| Task | Security control |
| --- | --- |
| git-clone | source from GitLab |
| gitsign-verify | Sigstore commit signature (Fulcio + Rekor + TUF). WARN on the unsigned scaffold commit; PASS after Dev Spaces. |
| build-source | Maven/npm **only** against Nexus (preloaded / proxied libs), Maven cache PVC |
| openshift-build | image with **OpenShift Builds** (Docker BuildConfig) → Quay |
| generate-sbom | Syft CycloneDX + SPDX inventory |
| sign-image | cosign **demo key** + Rekor |
| attest-sbom | SBOM bound to the digest (`.att`) |
| acs-scan / acs-check | ACS CVEs and policies |
| upload-tpa | searchable SBOM in TPA |
| conforma-dev | Enterprise Contract `@redhat` + `@slsa3` (report; not STRICT) |
| update-gitops-dev | GitOps `dev` |
| chains-status (finally) | Dumps Tekton Chains annotations. Does **not** sign. Chains (`tekton-chains-controller`) signs TaskRuns **keyless** (Fulcio + Rekor) asynchronously. |

Open Quay: SHA tag, `.sig`, SBOM. Rekor Search UI (`https://rekor-search-ui-trusted-artifact-signer.apps.<cluster>/`): UUID from the pipeline, email, or digest. OpenShift → Builds: the `{app}-img` BuildConfig.

On the catalog entity, **Topology** already shows `{app}-dev` (and pipeline `affinity-assistant-*` pods in that namespace). Staging and prod appear after the next two steps, not before those namespaces have Running workloads.

## 4. Promote to staging (3 min)

GitLab → Repository → Tags → `v1.0.0` on the commit **already built** (the GitLab tag starts the pipeline; the image stays tagged with the SHA).

`{app}-promote` overlay `staging`: ACS → **Conforma STRICT** → GitOps → comment on the GitLab commit.

ACS policy **Fixable Severity at least Important** is **report-only** on this demo (UBI n-1 images always have fixable RHSAs). If it still has `FAIL_BUILD`, the promote task fails even after bumping app libraries.

To actually shrink those ACS findings in Dev Spaces (optional inner-loop), bump **both**:

1. `pom.xml` platform (Quarkus + Netty), for example `3.20.6.SP2-redhat-00001` or `3.27.0.redhat-00002`
2. `src/main/docker/Dockerfile.jvm` base image off `ubi8/openjdk-17:1.16`, for example `registry.access.redhat.com/ubi8/openjdk-17:1.23`

Quarkus alone does **not** clear the gate: most BREAKS BUILD hits are RHSA on the UBI8 JDK image (`glibc`, `openjdk`, `python3`, …).

## 5. Promote to production (3 min)

GitLab → Deployments → Releases → Release `v1.0.0`.

Same pipeline, overlay `prod`, Conforma STRICT. Prod route. In Hub, open **Topology** again: `{app}-dev`, `{app}-staging`, and `{app}-prod` share `backstage.io/kubernetes-id` (the catalog does not pin `kubernetes-namespace` to `-dev`).

## 6. Wrap-up (2 min)

Nexus (`admin` / `admin123`) shows the Maven cache. Chains is in `TektonConfig/config`: `signers.x509.fulcio.enabled` plus Rekor `transparency.url` (no long-lived Chains key). Conforma evaluates the documented Red Hat and SLSA collections; Konflux-only hermetic rules are excluded. Platform engineers evolve templates and policies; developers never touch signing or ACS YAML.

If you change the platform during the week, push it to **GitHub** so the next `./install.sh` on a new sandbox includes it.
