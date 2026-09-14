# RHADS — Software supply chain security demo

**Ansible + OpenShift GitOps** installer for [Red Hat Advanced Developer Suite](https://docs.redhat.com/en/documentation/red_hat_advanced_developer_suite_-_software_supply_chain/1.9) on OpenShift 4.20+. Built for sandboxes that are recreated every few days.

This GitHub repository is the **source of truth for a repeatable install**. Clone it, log in to a cluster, run `./install.sh`. Ansible publishes the working tree to in-cluster GitLab and points Argo CD at that copy. Changes that live only on a sandbox GitLab (or only locally) are **not** on the next cluster.

Stack validated against the OpenShift catalog (OCP **4.20.33**):

| Product | Channel / version |
| --- | --- |
| OpenShift GitOps | `latest` → 1.21.3 |
| OpenShift Pipelines | `latest` → 1.23.1 |
| Red Hat Developer Hub | `fast` → 1.10.3 |
| OpenShift Dev Spaces | `stable` → 3.29.1 |
| Trusted Artifact Signer | `stable` → 1.4.3 (Fulcio, Rekor, TSA, TUF) |
| Trusted Profile Analyzer | `stable-v3` → 3.1.0 |
| Advanced Cluster Security | `stable` → 4.11.2 |
| Quay | `stable-3.17` → 3.17.4 |
| Red Hat build of Keycloak | `stable-v26.6` → 26.6.6 |
| GitLab Community | `docker.io/gitlab/gitlab-ce:19.3.0-ce.0` (omnibus) |
| OpenShift Data Foundation | MCG (NooBaa) + Multicluster Orchestrator |
| Conforma | CLI in the promotion pipeline |

Patterns taken from [rh1-demo-rhoai3](https://github.com/panchoraposo/rh1-demo-rhoai3) (GitLab, RHDH, users, templates) and [demo-spring](https://github.com/panchoraposo/demo-spring) (RHTAS, TPA, ACS, signing, and SBOM).

## Architecture

```
GitHub (this repo)  ──install.sh──► cluster (operators + ODF MCG)
                         │
                         └── GitLab platform-engineers/rhads-platform  ◄── Argo CD

Developer Hub  ──template──► GitLab (source + *-gitops)
      │                         │
      └── Dev Spaces            ├── push  → Nexus + OpenShift Builds + SBOM + cosign + Chains → dev
         RHDA / gitsign         ├── tag   → Conforma STRICT + GitOps → staging
                                └── release → Conforma STRICT + GitOps → production
```

Details: [docs/architecture.md](docs/architecture.md). Live script: [docs/demo-script.md](docs/demo-script.md). Credentials: [docs/credentials.md](docs/credentials.md).

## Prerequisites

- OpenShift **4.20+** with `cluster-admin` (typical sandbox: **16 CPU / 64 Gi**).
- Storage class **`gp3-csi`**. Object storage is **ODF Multicloud Object Gateway** (NooBaa), not MinIO.
- `oc` logged in to the target cluster. `python3`. Helm 3 is installed by `install.sh` if missing.
- Cluster pull access to `registry.redhat.io` (usual OpenShift pull secret).

## Installation

```bash
git clone https://github.com/panchoraposo/demo-rhads.git
cd demo-rhads
oc login --server=https://api.<cluster> --token=...
./install.sh
```

`install.sh` uses the current `oc` context. Select the target cluster before running it.

The playbook:

1. Discovers the cluster domain (`apps.<baseDomain>`).
2. Installs OpenShift GitOps and applies RHADS operators with kustomize (including ODF + MCO).
3. Deploys GitLab CE, Keycloak, **ODF (MCG + MCO)**, Quay, RHTAS, TPA, ACS, Pipelines, **Nexus**, Dev Spaces, and Developer Hub.
4. Creates **developers** (`dev1-3`) and **platform-engineers** (`pe1-3`) groups, password `backstage`.
5. Publishes **this working tree** to GitLab (`platform-engineers/rhads-platform`) and points Argo CD at it. Software templates get the live GitLab/Quay hostnames.
6. Seeds TPA with a small OSV advisory set so RHDA/SBOMs show CVEs without waiting for full feed importers.
7. Publishes a dashboard with URLs and credentials: `https://dashboard.apps.<cluster>/`.

Typical time on a single-node sandbox: **45–70 minutes**.

Clair and ACS autoscaling are disabled on purpose. Block storage: `gp3-csi`.

After install, GitOps self-heals from **GitLab**, not from GitHub. To change the platform on a live cluster, update this checkout and re-run the installer (or at least the GitLab publish + Hub roles). To make the next sandbox pick up a change, **commit and push it to GitHub `main` first**.

If GitLab omnibus is rebuilt but the rest of the cluster is intact:

```bash
./venv/bin/ansible-playbook -i ansible/inventory ansible/playbooks/refresh-gitlab-hub.yaml
```

## Users

| Who | Username | Password | Where |
| --- | --- | --- | --- |
| Developer | `dev1` / `dev2` / `dev3` | `backstage` | Keycloak emails `*@rhads.demo` → RHDH, GitLab, gitsign |
| Platform engineer | `pe1` / `pe2` / `pe3` | `backstage` | Keycloak → RHDH (admin RBAC) |
| GitLab root | `root` | `backstage` | GitLab |
| Quay | `quayadmin` | `backstage` | Quay |
| Nexus | `admin` | `admin123` | Nexus |
| TPA | `tpa-admin` | `backstage` | realm `trustify` |

Generated secrets (Keycloak master, Argo CD, ACS): [docs/credentials.md](docs/credentials.md).

## Demo flow

1. Sign in to Developer Hub as `dev1`.
2. Create → **Agentic app — Quarkus MCP** (Camel, Node.js, **Quarkus HITL**, and **Camel supervisor / Miles of Smiles** are also available). Component **name max 18 characters** (`quarkus-agent` is the default). For HITL + MaaS, pick **Agentic app — Quarkus HITL** (`miles-smiles`). For Kaoto + Camel JBang + MaaS (supervisor, no HITL), pick **Agentic app — Camel supervisor** (`miles-camel`) and paste a MaaS API key.
3. The template publishes source plus a GitOps repo (app of apps: build, dev, staging, prod). A Job registers the GitLab webhook, creates the Quay repo, and **starts the first PipelineRun** (the scaffolder commit landed before the hook existed). That unsigned run loads the **first SBOM into TPA**.
4. From the catalog, **OpenShift Dev Spaces**. Run RHDA on `pom.xml`. Open the SBOM in TPA. Change code.
5. Dev Spaces command palette: **Configure Sigstore git commit signing (gitsign + RHTAS TUF)**, signed `git commit` (copy the OIDC URL; Dev Spaces has no browser helper), **Verify Sigstore-signed HEAD**, then `git push` → pipeline.
6. Pipeline: Nexus, OpenShift Builds, SBOM, cosign+Rekor, attest, ACS, TPA, Conforma (report in `dev`), GitOps `dev`, Tekton Chains.
7. In GitLab, **tag** `v1.0.0` → staging (Conforma STRICT + commit comment).
8. **Release** on that tag → production (Conforma STRICT). Hub **Topology** lists the same workload in `{app}-dev`, `{app}-staging`, and `{app}-prod` (catalog entities do not pin a single namespace).

## Uninstall

```bash
./venv/bin/ansible-playbook -i ansible/inventory ansible/playbooks/uninstall.yaml
```
