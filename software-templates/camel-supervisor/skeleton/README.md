# ${{values.component_id}}

${{values.description}}

Miles of Smiles **step 04** from [demo-camel-agentic](https://github.com/panchoraposo/demo-camel-agentic): a **supervisor** (`recipientList`) over pricing, disposition, maintenance, and cleaning. Inner loop is **Camel JBang 4.18** + **Kaoto**. Cluster image is **Camel Quarkus** on the same YAML routes so RHADS pipelines (Nexus, Builds, SBOM, cosign, ACS, Conforma) still apply.

## Inner loop (Dev Spaces + MaaS + Kaoto)

1. Catalog → **OpenShift Dev Spaces (VS Code)**. First start installs JBang + Camel CLI (`postStart`).
2. Command palette → **Write .env for Red Hat MaaS** if `MAAS_API_KEY` is empty.
3. Command palette → **Camel JBang + MaaS (fleet UI on 8080)**.
4. Open **PORTS → fleet-ui**. Open `integrations/03-workflow.camel.yaml` with **Kaoto** (right-click or command palette).
5. Return Civic `#7` (`RENTED`) with:

```text
The car was in a serious collision. Front end is completely destroyed and airbags deployed.
```

Expected: **Pending Disposition**. Logs: `▶` start, `🧠` agents, `🚗` / `🔧` / `📋` tools, `✅` outcome. `POST /car-management/return/{id}` returns `{ car, workflow }`.

YAML not hot-reloaded from Kaoto until you restart Camel JBang.

## Cluster

The first Tekton run publishes an image; GitOps **dev** Recreates the pod with MaaS env from the Helm secret. Staging/prod stay at 0 replicas until a GitLab tag/release.

Fleet UI (dev): `https://${{values.component_id}}-${{values.component_id}}-dev.<apps-domain>/`

## Supply chain

Signed `git commit` / `git push` → Nexus, OpenShift Builds, SBOM, cosign, TPA, Conforma. GitLab **tag** promotes to staging; **release** promotes to production.
