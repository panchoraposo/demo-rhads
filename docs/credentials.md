# Demo credentials

Default values (override in `ansible/vars/demo.yaml`). After install, use the cluster dashboard; Keycloak admin and Argo CD passwords are generated.

Developer and platform users authenticate through Keycloak realm **`backstage`**. Certificate identity for gitsign is the Keycloak **email** (`dev1@rhads.demo`), not a `@rhads.com` address.

| System | Username | Password |
| --- | --- | --- |
| Developer Hub / GitLab / Keycloak realm `backstage` | `dev1` `dev2` `dev3` | `backstage` |
| Developer Hub (RBAC admin) | `pe1` `pe2` `pe3` | `backstage` |
| GitLab | `root` | `backstage` |
| Quay | `quayadmin` | `backstage` |
| Nexus | `admin` | `admin123` |
| Vault UI | `vaultadmin` | `backstage` |
| TPA (realm `trustify`) | `tpa-admin` | `backstage` |
| Keycloak master | `temp-admin` | secret `keycloak/keycloak-initial-admin` |
| Argo CD | `admin` | secret `openshift-gitops/openshift-gitops-cluster` |
| ACS | `admin` | secret `stackrox/central-htpasswd` |
| Vault root token | — | secret `vault/vault-init` |

TPA API clients used by the pipeline and RHDA backend: Keycloak realm `trustify`, client `cli` (seeded at install). Do not treat demo passwords as production secrets.
