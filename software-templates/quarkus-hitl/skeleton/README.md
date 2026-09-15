# ${{values.component_id}}

${{values.description}}

Miles of Smiles **step 05** from [quarkus-langchain-agentic](https://github.com/panchoraposo/quarkus-langchain-agentic): a supervisor workflow with **human-in-the-loop** for high-value write-offs. Runtime is **Red Hat build of Quarkus 3.33** and **JDK 21**. Application CI/CD is **OpenShift Pipelines**; GitLab CI only builds TechDocs.

## Inner loop (Dev Spaces + MaaS)

1. Catalog → **OpenShift Dev Spaces (VS Code)**.
2. Command palette → **Write .env for Red Hat MaaS** if `MAAS_API_KEY` is empty (OpenShift AI → Gen AI studio → API keys).
3. Command palette → **Quarkus dev (fleet UI + Dev UI on 8080)**. That step selects **JDK 21** (the UDI default is 17; compiling `release` 21 with 17 fails). The first run is slow (Temurin 21 if missing, Maven deps, Quarkus augmentation). Later runs skip the JDK download.
4. Open **PORTS** (or **Endpoints**) → **fleet-ui**, not only the in-IDE Simple Browser. The Simple Browser URL looks like `/admin/<workspace>/8080/` and used to miss CSS/API because those were rooted at `/`. The UI now prefixes that path; a full browser tab on **fleet-ui** is still the reliable demo URL.
   - **fleet-ui** → car grid (`/`)
   - **quarkus-dev-ui** → `/q/dev-ui`
   - **swagger-ui** → `/q/swagger-ui`

Return a Mercedes / BMW / Audi / Civic (`RENTED`) with a totaled/collision prompt. Agents (intake, pricing, proposal) still run on MaaS; Java only **forces** the human pause. **Approval Needed** shows the LLM recommendation. Keep or dispose from that button.

The cluster **Fleet UI (dev)** is `https://miles-smiles-miles-smiles-dev.<apps-domain>/`. Staging/prod stay at 0 replicas until a GitLab tag/release promote; they must not pull `:pending-promotion`. A new image in **dev** uses Deployment strategy **Recreate** so the in-memory H2 database always reloads `import.sql` (fleet status and in-JVM HITL approvals reset).

Logs follow the Miles of Smiles convention: `>>> START`, `[ai]` agents, `[tool]` tools, `<<< DONE`. `POST /car-management/return/{id}` returns `{ car, workflow }`.

```text
The car was in a serious collision. Front end is completely destroyed and airbags deployed.
```

A 12-year-old Ford Focus (`#5`) with the same prompt still pauses for a human; the card shows a much lower book value.

## Supply chain

Signed `git commit` / `git push` → Nexus, OpenShift Builds, SBOM, cosign, TPA, Conforma. GitLab **tag** promotes to staging; **release** promotes to production.
