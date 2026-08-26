# ${{values.component_id}}

${{values.description}}

Application CI/CD (build, sign, SBOM, ACS, Conforma) runs on **OpenShift Pipelines**.
GitLab CI on this repository only builds TechDocs for Developer Hub.

## Demo path

1. OpenShift Dev Spaces: Red Hat Dependency Analytics on `pom.xml`, then **Configure Sigstore git commit signing**.
2. Signed `git commit` / `git push` starts the build pipeline (Nexus, OpenShift Builds, cosign, TPA, Conforma).
3. GitLab **tag** promotes to staging; **release** promotes to production.
