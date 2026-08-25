# Credenciales de la demo

Valores por defecto (override en `ansible/vars/demo.yaml`). Tras instalar, usa el dashboard del cluster; las passwords de Keycloak admin y Argo CD se generan.

| Sistema | Usuario | Password |
| --- | --- | --- |
| Developer Hub / GitLab / Keycloak realm `backstage` | `dev1` `dev2` `dev3` | `backstage` |
| Developer Hub (RBAC admin) | `pe1` `pe2` `pe3` | `backstage` |
| GitLab | `root` | `backstage` |
| Quay | `quayadmin` | `backstage` |
| TPA (realm `trustify`) | `tpa-admin` | `backstage` |
| Keycloak master | `temp-admin` | secret `keycloak/keycloak-initial-admin` |
| Argo CD | `admin` | secret `openshift-gitops/openshift-gitops-cluster` |
| ACS | `admin` | secret `stackrox/central-htpasswd` |
