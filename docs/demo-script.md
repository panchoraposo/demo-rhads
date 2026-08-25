# Guion de demo en vivo (~25–30 min)

Password común: `backstage`. Abre el dashboard `https://dashboard.apps.<cluster>/` en una pestaña de apoyo.

## 0. Contexto (2 min)

Cadena de suministro: **quién** construyó **qué**, **con qué dependencias**, **firmado por quién**, **promovido con qué política**. RHADS une portal (RHDH), firma (RHTAS), inventario (TPA), políticas (Conforma) y runtime (ACS) sobre GitOps.

## 1. Portal — crear la app (4 min)

1. Developer Hub → login OIDC Keycloak como **`dev1`**.
2. Create → **Agentic app — Quarkus MCP** (group `developers`, Quay ya rellenado).
3. Esperar a que el scaffolder publique dos repos y el app-of-apps de Argo CD (`{app}-build`, `{app}-dev`, `{app}-staging`, `{app}-prod`).
4. El push inicial dispara el pipeline de build (commit **sin** gitsign). Eso carga el **primer SBOM en TPA**.

## 2. Inner loop en Dev Spaces (6 min)

1. En el componente del catálogo, **OpenShift Dev Spaces (VS Code)**.
2. Abrir `pom.xml` → paleta → **Red Hat Dependency Analytics** (CVEs de Maven; las libs salen de Nexus).
3. En otra pestaña, **Trusted Profile Analyzer** (link del catálogo) y mostrar el SBOM del primer build.
4. Cambiar un string en un `@Tool` o en `index.html`.
5. `git commit` — el workspace está configurado con **gitsign** (misma raíz de confianza Fulcio/Rekor que **cosign**). Completar el login OIDC. `git push` a `main`.
6. El webhook de GitLab arranca el pipeline sobre ese commit firmado.

## 3. Pipeline de build (8 min)

En RHDH (pestaña Tekton) o OpenShift → Pipelines. Cada task imprime un bloque `[SSSC]`:

| Task | Control de seguridad |
| --- | --- |
| git-clone | fuente desde GitLab |
| gitsign-verify | firma Sigstore del commit (Fulcio + Rekor) |
| build-source | Maven/npm **solo** contra Nexus (libs precargadas / proxy) |
| openshift-build | imagen con **OpenShift Builds** (BuildConfig Docker) → Quay |
| generate-sbom | inventario Syft CycloneDX + SPDX |
| sign-image | cosign + Rekor |
| attest-sbom | SBOM atado al digest (`.att`) |
| acs-scan / acs-check | CVEs y políticas ACS |
| upload-tpa | SBOM searchable en TPA |
| conforma-dev | Enterprise Contract (informe; no STRICT) |
| update-gitops-dev | GitOps `dev` |
| chains-status (finally) | Tekton Chains / provenance SLSA |

Abrir Quay: tag SHA, `.sig`, SBOM. Rekor: UUID. OpenShift → Builds: el BuildConfig `{app}-img`.

## 4. Promoción a staging (3 min)

GitLab → Repository → Tags → `v1.0.0` sobre el commit **ya construido** (el tag GitLab dispara el pipeline; la imagen sigue etiquetada con el SHA).

`{app}-promote` overlay `staging`: ACS → **Conforma STRICT** → GitOps → comentario en el commit de GitLab.

## 5. Promoción a producción (3 min)

GitLab → Deployments → Releases → Release `v1.0.0`.

Mismo pipeline, overlay `prod`, Conforma STRICT. Route de prod y Topology en RHDH.

## 6. Cierre (2 min)

Nexus (`admin` / `admin123`) muestra el caché Maven. Chains está en `TektonConfig/config` (`spec.chain.disabled: false`). Platform engineers evolucionan templates y políticas; developers no tocan YAML de firma ni de ACS.
