# RHADS — Software supply chain security demo

**Ansible + OpenShift GitOps** installer for [Red Hat Advanced Developer Suite](https://docs.redhat.com/en/documentation/red_hat_advanced_developer_suite_-_software_supply_chain/1.9) on OpenShift 4.20+. Built for sandboxes that are recreated every few days.

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
| Conforma | CLI in the promotion pipeline |

Patterns taken from [rh1-demo-rhoai3](https://github.com/panchoraposo/rh1-demo-rhoai3) (GitLab, RHDH, users, templates) and [demo-spring](https://github.com/panchoraposo/demo-spring) (RHTAS, TPA, ACS, signing, and SBOM).

## Architecture

```
Developer Hub  ──template──► GitLab (source + *-gitops)
      │                         │
      └── Dev Spaces            ├── push  → Nexus + OpenShift Builds + SBOM + cosign + Chains → dev
         RHDA / gitsign         ├── tag   → Conforma STRICT + GitOps → staging
                                └── release → Conforma STRICT + GitOps → production
```

Details: [docs/architecture.md](docs/architecture.md). Live script: [docs/demo-script.md](docs/demo-script.md).

## Installation

```bash
oc login --server=https://api.<cluster> --token=...
./install.sh
```

`install.sh` uses the current `oc` context. Select the target cluster before running it.

The playbook:

1. Discovers the cluster domain (`apps.<baseDomain>`).
2. Installs OpenShift GitOps and applies RHADS operators with kustomize.
3. Deploys GitLab CE, Keycloak, **ODF (MCG + MCO)**, Quay, RHTAS, TPA, ACS, Pipelines, **Nexus**, Dev Spaces, and Developer Hub.
4. Creates **developers** (`dev1-3`) and **platform-engineers** (`pe1-3`) groups, password `backstage`.
5. Publishes this repository to GitLab (`platform-engineers/rhads-platform`) and points Argo CD at it.
6. Publishes a dashboard with URLs and credentials: `https://dashboard.apps.<cluster>/`.

Typical time on a single-node sandbox (16 CPU / 64 Gi): **45–70 minutes**.

Object storage: **ODF Multicloud Object Gateway** (NooBaa) with **ODF Multicluster Orchestrator (MCO)**. Block: `gp3-csi`. Clair and ACS autoscaling are disabled on purpose.

## Users

| Who | Username | Password | Where |
| --- | --- | --- | --- |
| Developer | `dev1` / `dev2` / `dev3` | `backstage` | Keycloak → RHDH and GitLab |
| Platform engineer | `pe1` / `pe2` / `pe3` | `backstage` | Keycloak → RHDH (admin RBAC) |
| GitLab root | `root` | `backstage` | GitLab |
| Quay | `quayadmin` | `backstage` | Quay |

## Demo flow

1. Sign in to Developer Hub as `dev1`.
2. Create → **Agentic app — Quarkus MCP** (Camel and Node.js are also available).
3. The template publishes source plus a GitOps repo (app of apps: build, dev, staging, prod). The first pipeline uploads the SBOM to TPA.
4. From the catalog, **OpenShift Dev Spaces**. Run RHDA on `pom.xml`. Open the SBOM in TPA. Change code.
5. Dev Spaces command palette: **Configure Sigstore git commit signing (gitsign + RHTAS TUF)**, signed `git commit`, **Verify Sigstore-signed HEAD**, then `git push` → pipeline.
6. Pipeline: Nexus, OpenShift Builds, SBOM, cosign+Rekor, attest, ACS, TPA, Conforma, GitOps `dev`, Tekton Chains.
7. In GitLab, **tag** `v1.0.0` → staging (Conforma STRICT + commit comment).
8. **Release** on that tag → production (Conforma STRICT).

## Uninstall

```bash
./venv/bin/ansible-playbook -i ansible/inventory ansible/playbooks/uninstall.yaml
```
