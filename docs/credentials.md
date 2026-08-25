# Demo credentials

Default values (override in `ansible/vars/demo.yaml`). After install, use the cluster dashboard; Keycloak admin and Argo CD passwords are generated.

| System | Username | Password |
| --- | --- | --- |
| Developer Hub / GitLab / Keycloak realm `backstage` | `dev1` `dev2` `dev3` | `backstage` |
| Developer Hub (RBAC admin) | `pe1` `pe2` `pe3` | `backstage` |
| GitLab | `root` | `backstage` |
| Quay | `quayadmin` | `backstage` |
| TPA (realm `trustify`) | `tpa-admin` | `backstage` |
| Keycloak master | `temp-admin` | secret `keycloak/keycloak-initial-admin` |
| Argo CD | `admin` | secret `openshift-gitops/openshift-gitops-cluster` |
| ACS | `admin` | secret `stackrox/central-htpasswd` |
