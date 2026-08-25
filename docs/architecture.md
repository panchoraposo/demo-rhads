# Architecture — RHADS software supply chain demo

## Goal

A repeatable platform, installed with Ansible and reconciled by OpenShift GitOps, to demonstrate **software supply chain security** with Red Hat Advanced Developer Suite on OpenShift 4.20+.

## Components

```mermaid
flowchart LR
  subgraph idp [Identity]
    RHBK[Keycloak RHBK 26.6]
  end
  subgraph portal [Portal]
    RHDH[Developer Hub 1.10]
    DS[Dev Spaces 3.29]
  end
  subgraph scm [SCM]
    GL[GitLab CE]
  end
  subgraph gitops [GitOps]
    ACD[OpenShift GitOps 1.21]
  end
  subgraph cicd [CI/CD]
    PIPE[Pipelines 1.23 + Chains]
    BUILD[OpenShift Builds]
    NEXUS[Nexus]
    CONF[Conforma]
  end
  subgraph rhads [RHADS]
    TAS[RHTAS 1.4 Fulcio Rekor]
    TPA[TPA 3.1]
    ACS[ACS 4.11]
    QUAY[Quay 3.17]
  end
  RHBK --> RHDH
  RHBK --> GL
  RHBK --> TAS
  RHBK --> TPA
  RHDH --> GL
  RHDH --> DS
  GL -->|push| PIPE
  GL -->|tag| PIPE
  GL -->|release| PIPE
  PIPE --> BUILD
  BUILD --> QUAY
  PIPE --> NEXUS
  PIPE --> TAS
  PIPE --> TPA
  PIPE --> ACS
  PIPE --> CONF
  PIPE --> ACD
  ACD --> DEV[ns app-dev]
  ACD --> STG[ns app-staging]
  ACD --> PRD[ns app-prod]
```

## Promotion

| GitLab event | Pipeline | Environment | Conforma |
| --- | --- | --- | --- |
| `push` to `main` | `{app}-build` | `{app}-dev` | report, non-blocking |
| `tag_push` | `{app}-promote` overlay `staging` | `{app}-staging` | STRICT |
| `release` create | `{app}-promote` overlay `prod` | `{app}-prod` | STRICT |

Each build pipeline:

1. `git-clone` + `gitsign verify` (RHTAS; Dev Spaces commits use gitsign, same trust root as cosign)
2. Maven / npm against **Nexus** (`maven-public` / `npm-group`)
3. **OpenShift Builds** (BuildConfig Docker strategy, binary `--from-dir`) → Quay
4. Syft CycloneDX + SPDX
5. `cosign sign` with the demo key and **upload to Rekor**
6. `cosign attest` SBOM + `cosign attach sbom`
7. `roxctl image scan` and `image check` (ACS)
8. Upload SBOM to Trusted Profile Analyzer
9. `ec validate image` (Conforma; STRICT on staging/prod)
10. Commit the digest to the GitOps overlay + GitLab comment
11. Tekton Chains signs the PipelineRun (in-toto / SLSA)

## Software templates

Three Developer Hub templates, all app-of-apps:

- `quarkus-agentic` — Quarkus MCP server (Red Hat build of Quarkus)
- `camel-agentic` — Camel Quarkus REST agent
- `nodejs-agentic` — Express agent

The scaffolder creates:

1. A source repo in the GitLab `developers` group
2. An `{app}-gitops` repo with `argocd/applications.yaml` (build + dev + staging + prod)
3. An Argo CD bootstrap Application on `argocd/`

## Resources (single-node sandbox)

Clair disabled, ACS scanner at 1 replica, TPA without tracing/metrics, GitLab all-in-one. Block storage: `gp3-csi`. Object storage: **OpenShift Data Foundation** Multicloud Object Gateway (NooBaa) with the **ODF Multicluster Orchestrator (MCO)** operator; Quay and TPA use ObjectBucketClaims.
