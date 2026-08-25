const express = require("express");
const app = express();
app.use(express.json());

app.get("/q/health/live", (_req, res) => res.json({ status: "ok" }));
app.get("/q/health/ready", (_req, res) => res.json({ status: "ok" }));
app.get("/api/v1/health", (_req, res) =>
  res.json({ status: "ok", runtime: "nodejs", agent: "${{values.component_id}}" })
);
app.post("/api/v1/agent/echo", (req, res) =>
  res.json({ agent: "${{values.component_id}}", echo: req.body })
);

const port = process.env.PORT || 8080;
app.listen(port, () => console.log(`agent listening on ${port}`));
