# ${{values.component_id}}

${{values.description}}

Miles of Smiles **step 05** ([quarkus-langchain-agentic](https://github.com/panchoraposo/quarkus-langchain-agentic)): a supervisor workflow with **human-in-the-loop** for high-value write-offs. Application CI/CD runs on **OpenShift Pipelines**. GitLab CI only builds TechDocs.

## Inner loop (Dev Spaces + MaaS)

1. Catalog → **OpenShift Dev Spaces (VS Code)**.
2. Command palette → **Write .env for Red Hat MaaS** (or edit `.env` with `MAAS_API_KEY` from OpenShift AI → Gen AI studio → API keys).
3. Command palette → **Quarkus dev (fleet UI + Dev UI on 8080)**. The command installs **JDK 21** if the UDI still defaults to 17 (`release version 21 not supported`).
4. Open the workspace endpoints:
   - **fleet-ui** → car grid (`/`)
   - **quarkus-dev-ui** → `/q/dev-ui`
   - **swagger-ui** → `/q/swagger-ui`
5. Return a Mercedes / BMW / Audi / Civic (`RENTED`) with a totaled/collision prompt. If estimated value is **> $15,000**, use **Approval Needed**.

```text
The car was in a serious collision. Front end is completely destroyed and airbags deployed.
```

A 12-year-old Ford Focus with the same prompt usually skips HITL.

## Supply chain

Signed `git commit` / `git push` → Nexus, OpenShift Builds, SBOM, cosign, TPA, Conforma. GitLab **tag** promotes to staging; **release** promotes to production.
