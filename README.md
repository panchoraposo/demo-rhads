# RHADS — Demo de seguridad en la cadena de suministro

Instalador **Ansible + OpenShift GitOps** de [Red Hat Advanced Developer Suite](https://docs.redhat.com/en/documentation/red_hat_advanced_developer_suite_-_software_supply_chain/1.9) sobre OpenShift 4.20+. Pensado para sandboxes que se recrean cada pocos días.

Stack validado contra el catálogo del cluster (context `devopsdays`, OCP **4.20.33**):

| Producto | Canal / versión |
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
| Conforma | CLI en el pipeline de promoción |

Patrones tomados de [rh1-demo-rhoai3](https://github.com/panchoraposo/rh1-demo-rhoai3) (GitLab, RHDH, usuarios, templates) y [demo-spring](https://github.com/panchoraposo/demo-spring) (RHTAS, TPA, ACS, firma y SBOM).

## Arquitectura

```
Developer Hub  ──template──► GitLab (source + *-gitops)
      │                         │
      └── Dev Spaces            ├── push  → Nexus + OpenShift Builds + SBOM + cosign + Chains → dev
         RHDA / gitsign         ├── tag   → Conforma STRICT + GitOps → staging
                                └── release → Conforma STRICT + GitOps → production
```

Detalle: [docs/architecture.md](docs/architecture.md). Guion en vivo: [docs/demo-script.md](docs/demo-script.md).

## Instalación

```bash
oc config use-context devopsdays
./install.sh
```

El playbook:

1. Descubre el dominio del cluster (`apps.<baseDomain>`).
2. Instala OpenShift GitOps y aplica operadores RHADS con kustomize.
3. Despliega GitLab CE, Keycloak, **ODF (MCG + MCO)**, Quay, RHTAS, TPA, ACS, Pipelines, **Nexus**, Dev Spaces y Developer Hub.
4. Crea grupos **developers** (`dev1-3`) y **platform-engineers** (`pe1-3`), password `backstage`.
5. Publica este repositorio en GitLab (`platform-engineers/rhads-platform`) y deja Argo CD apuntando a él.
6. Publica un dashboard con URLs y credenciales: `https://dashboard.apps.<cluster>/`.

Tiempo típico en un sandbox de un nodo (16 CPU / 64 Gi): **45–70 minutos**.

## Usuarios

| Quién | Usuario | Password | Dónde |
| --- | --- | --- | --- |
| Developer | `dev1` / `dev2` / `dev3` | `backstage` | Keycloak → RHDH y GitLab |
| Platform engineer | `pe1` / `pe2` / `pe3` | `backstage` | Keycloak → RHDH (admin RBAC) |
| GitLab root | `root` | `backstage` | GitLab |
| Quay | `quayadmin` | `backstage` | Quay |

## Flujo de demo

1. Login en Developer Hub como `dev1`.
2. Create → **Agentic app — Quarkus MCP** (también Camel y Node.js).
3. El template publica código + repo GitOps (app of apps: build, dev, staging, prod). El primer pipeline sube el SBOM a TPA.
4. Desde el catálogo, **OpenShift Dev Spaces**. RHDA sobre `pom.xml`. Ver el SBOM en TPA. Cambiar código.
5. Commit **firmado con gitsign** (Fulcio/Rekor, misma raíz que cosign) y push → arranca el pipeline.
6. Pipeline: Nexus, OpenShift Builds, SBOM, cosign+Rekor, attest, ACS, TPA, Conforma, GitOps `dev`, Tekton Chains.
7. En GitLab, **tag** `v1.0.0` → staging (Conforma STRICT + comentario en el commit).
8. **Release** sobre ese tag → production (Conforma STRICT).

## Repetir en un cluster nuevo

```bash
oc login --server=https://api.<nuevo> --token=...
oc config rename-context $(oc config current-context) devopsdays   # o edita ansible/vars/demo.yaml
./install.sh
```

Object storage: **ODF Multicloud Object Gateway** (NooBaa) con **ODF Multicluster Orchestrator (MCO)**. Bloques: `gp3-csi`. Clair y autoscaling de ACS van desactivados a propósito.

## Desinstalación

```bash
./venv/bin/ansible-playbook -i ansible/inventory ansible/playbooks/uninstall.yaml
```
