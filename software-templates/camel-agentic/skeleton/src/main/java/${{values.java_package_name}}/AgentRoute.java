package ${{values.java_package_name}};

import org.apache.camel.builder.RouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AgentRoute extends RouteBuilder {
    @Override
    public void configure() {
        rest("/api/v1")
            .get("/health").to("direct:health")
            .post("/agent/echo").to("direct:echo");

        from("direct:health")
            .setBody().constant("{\"status\":\"ok\",\"runtime\":\"camel-quarkus\"}");

        from("direct:echo")
            .setHeader("Content-Type", constant("application/json"))
            .setBody().simple("{\"agent\":\"${{values.component_id}}\",\"echo\":\"${body}\"}");
    }
}
