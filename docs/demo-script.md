# Live demo script (~30–35 min)

Shared password: `backstage`. Keep the dashboard `https://dashboard.apps.<cluster>/` open in a support tab.

## 0. Context (2 min)

Supply chain: **who** built **what**, **with which dependencies**, **signed by whom**, **promoted under which policy**. RHADS joins the portal (RHDH), signing (RHTAS), inventory (TPA), policy (Conforma), and runtime (ACS) on GitOps.

This cluster was installed from GitHub (`./install.sh`). GitLab holds the live GitOps copy; the next sandbox only sees what is on GitHub `main`.

## 1. Portal — create the app (4 min)

1. Developer Hub → Keycloak OIDC login as **`dev1`**.
2. Create → **Agentic app — Quarkus MCP** (group `developers`, Quay already filled in). Name **at most 18 characters** — leave the default `quarkus-agent`. For Miles of Smiles HITL + MaaS instead, Create → **Agentic app — Quarkus HITL** (`miles-smiles`) and paste the MaaS API key. For Camel + Kaoto supervisor (step 04, no HITL), Create → **Agentic app — Camel supervisor** (`miles-camel`) and paste the same MaaS key.
3. Wait for the scaffolder to publish two repos and the Argo CD app-of-apps (`{app}-build`, `{app}-dev`, `{app}-staging`, `{app}-prod`).
4. The bootstrap Job writes app secrets to **Vault**. External Secrets Operator syncs platform CI secrets and (for HITL/supervisor) the MaaS key into the app namespaces. The webhook Job registers the GitLab hook, creates `rhads/{app}` in Quay, and **immediately triggers** the first build (the scaffolder commit landed before the hook existed). That unsigned run loads the **first SBOM into TPA**, so it is there when Dev Spaces opens. Task `gitsign-verify` prints a WARN and still PASSes. `conforma-dev` reports `rhads_source.git_commit_signed` as a failure and continues (STRICT=false). **Do not tag this commit yet** — that is the failed-promotion beat in step 4. Optional: Vault UI (`vaultadmin` / `backstage`) → `secret/apps/{app}` and `secret/rhads/ci`.

## 2. Inner loop in Dev Spaces (6 min)

Start the workspace **before the audience is in the room**. The first start pulls the Universal Developer Image and che-code (several GB) and can take many minutes on a single-node sandbox. After that, the workspace stays running (idling is disabled) so opening it from the catalog is seconds, not minutes.

**HITL + MaaS (Miles of Smiles):** command palette → **Write .env for Red Hat MaaS** (if the API key was not set at scaffold) → **Quarkus dev (fleet UI + Dev UI on 8080)**. First start is slow (JDK 21 + Maven). Open **PORTS → fleet-ui** (public hostname). Return Civic `#7` with a collision prompt; **Approval Needed** appears in a few seconds (Java HITL, not the LLM). Cluster Fleet UI: catalog link **Fleet UI (dev)**. Each new image in **dev** Recreates the pod: in-memory H2 reloads `import.sql` (Civic `#7` is `RENTED` again; pending HITL is gone). Staging/prod stay scaled to 0 until GitLab tag/release.

**Camel supervisor + Kaoto + MaaS:** command palette → **Install JBang + Camel CLI** if needed → **Write .env for Red Hat MaaS** → **Camel JBang + MaaS (fleet UI on 8080)**. Open `integrations/03-workflow.camel.yaml` with **Kaoto** (right-click or command palette). Open **PORTS → camel-ui**. Return Civic `#7` with the collision/airbags prompt → **Pending Disposition** (no approval modal). YAML edits do not hot-reload; restart Camel JBang. Cluster Fleet UI is the same catalog link after Tekton publishes the Camel Quarkus image.

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
5. Command palette → **Configure Sigstore git commit signing (gitsign + RHTAS TUF)** (once per workspace). That installs gitsign, sets author `dev1@rhads.demo`, and initializes the **cluster TUF root** (private Fulcio is not in the public Sigstore TUF). Leave the CVE bump **unstaged** until after the unsigned tag fails in step 4, so the signed commit is the one you promote.

## 3. Build pipeline (8 min)

In RHDH (CI / Tekton tab) or OpenShift → Pipelines. Each task prints an `[SSSC]` block:

| Task | Security control |
| --- | --- |
| git-clone | source from GitLab |
| gitsign-verify | Sigstore commit signature (Fulcio + Rekor + TUF). WARN on the unsigned scaffold commit; PASS after Dev Spaces. |
| build-source | Maven/npm **only** against Nexus. First run extracts a preloaded `m2-seed` tarball onto the app PVC, then `mvn -o`. |
| openshift-build | image with **OpenShift Builds** (Docker BuildConfig) → Quay |
| generate-sbom | Syft CycloneDX + SPDX inventory |
| sign-image | cosign **demo key** + Rekor |
| attest-sbom | SBOM bound to the digest (`.att`) |
| acs-scan / acs-check | ACS CVEs and policies |
| upload-tpa | searchable SBOM in TPA |
| conforma-dev | Enterprise Contract `@redhat` + `@slsa3` + `rhads_source.git_commit_signed` (report; not STRICT). Unsigned scaffold is a visible failure that does not block dev. |
| update-gitops-dev | GitOps `dev` |
| chains-status (finally) | Dumps Tekton Chains annotations. Does **not** sign. Chains (`tekton-chains-controller`) signs TaskRuns **keyless** (Fulcio + Rekor) asynchronously. |

Open Quay: SHA tag, `.sig`, SBOM. Rekor Search UI (`https://rekor-search-ui-trusted-artifact-signer.apps.<cluster>/`): UUID from the pipeline, email, or digest. OpenShift → Builds: the `{app}-img` BuildConfig.

On the catalog entity, **Topology** already shows `{app}-dev` (and pipeline `affinity-assistant-*` pods in that namespace). Walk this table against the **unsigned** first PipelineRun. Staging appears only after the signed tag in step 6.

## 4. Promote unsigned → Conforma deny (3 min)

GitLab → Repository → Tags → `v1.0.0` on the **unsigned scaffold** commit (the first build SHA). The GitLab `tag_push` starts `{app}-promote` overlay `staging`. The image stays tagged with the SHA.

ACS image-check is **report-only** (UBI n-1 images always have fixable RHSAs). **Conforma STRICT** clones that commit, runs `gitsign verify`, and denies **`rhads_source.git_commit_signed`**. `update-gitops` does not run; `{app}-staging` stays empty. Open the Conforma task logs: `[SSSC] Conforma / Enterprise Contract (staging)` then `FAIL: rhads_source.git_commit_signed`.

## 5. Signed Dev Spaces change → new build (4 min)

Back in the workspace that is already Running:

1. Stage the CVE bump from step 2 (or a one-line README / tool-description edit).
2. Terminal: `git add -A && git commit -m "demo: signed change from Dev Spaces"`. Dev Spaces has no `xdg-open` — copy the printed URL, log in as **`dev1` / `backstage`**, paste the verification code. The Fulcio certificate identity is the Keycloak email (`dev1@rhads.demo`), not `dev1@rhads.com`.
3. Command palette → **Verify Sigstore-signed HEAD (gitsign + RHTAS TUF)**. GitLab shows the commit as **Verified**.
4. `git push origin main`. The webhook starts a **new** `{app}-build` on that SHA. `gitsign-verify` PASSes. `conforma-dev` reports `rhads_source.git_commit_signed` as satisfied (still STRICT=false).

Wait until this PipelineRun succeeds (Quay has the new SHA tag) before the next tag. Do not start a second build against the same RWO `{app}-build-cache` PVC.

## 6. Promote signed → staging (3 min)

GitLab → Repository → Tags → `v1.0.1` on the **signed** commit (not `v1.0.0`). Same `{app}-promote` pipeline, overlay `staging`, Conforma STRICT.

`rhads_source.git_commit_signed` PASSes together with `@redhat` / `@slsa3`. GitOps updates staging. Catalog **Topology** shows `{app}-staging`. A comment lands on the GitLab commit.

To actually shrink those ACS findings in Dev Spaces (optional inner-loop), bump **both**:

1. `pom.xml` platform (Quarkus + Netty), for example `3.20.6.SP2-redhat-00001` or `3.27.0.redhat-00002`
2. `src/main/docker/Dockerfile.jvm` base image off `ubi8/openjdk-17:1.16`, for example `registry.access.redhat.com/ubi8/openjdk-17:1.23`

Quarkus alone does **not** clear the gate: most BREAKS BUILD hits are RHSA on the UBI8 JDK image (`glibc`, `openjdk`, `python3`, …).

## 7. Promote to production (3 min)

GitLab → Deployments → Releases → Release `v1.0.1` (the **signed** tag, not `v1.0.0`).

Same pipeline, overlay `prod`, Conforma STRICT. Prod route. In Hub, open **Topology** again: `{app}-dev`, `{app}-staging`, and `{app}-prod` share `backstage.io/kubernetes-id` (the catalog does not pin `kubernetes-namespace` to `-dev`).

## 8. Wrap-up (2 min)

Nexus (`admin` / `admin123`) shows the Maven cache. Chains is in `TektonConfig/config`: `signers.x509.fulcio.enabled` plus Rekor `transparency.url` (no long-lived Chains key). Conforma evaluates `@redhat` + `@slsa3` plus `rhads_source.git_commit_signed`; Konflux-only hermetic rules are excluded. Platform engineers evolve templates and policies; developers never touch signing or ACS YAML.

If you change the platform during the week, push it to **GitHub** so the next `./install.sh` on a new sandbox includes it.
