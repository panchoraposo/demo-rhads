package ${{values.java_package_name}};

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.text.StringEscapeUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class AgentRoute extends RouteBuilder {
    @Override
    public void configure() {
        rest("/api/v1")
            .get("/health").to("direct:health")
            .post("/agent/echo").to("direct:echo");

        from("direct:health")
            .process(exchange -> {
                Map<String, String> body = new LinkedHashMap<>();
                body.put("status", "ok");
                body.put("runtime", StringEscapeUtils.escapeJson("camel-quarkus"));
                exchange.getMessage().setBody(new Yaml().dump(body).trim());
            });

        from("direct:echo")
            .setHeader("Content-Type", constant("application/json"))
            .setBody().simple("{\"agent\":\"${{values.component_id}}\",\"echo\":\"${body}\"}");
    }
}
