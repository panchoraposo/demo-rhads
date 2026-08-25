package ${{values.java_package_name}}.tools;

import ${{values.java_package_name}}.client.CustomerClient;
import ${{values.java_package_name}}.dto.request.CreateCustomerRequest;
import ${{values.java_package_name}}.dto.request.UpdateCustomerRequest;
import ${{values.java_package_name}}.dto.response.CustomerResponse;
import ${{values.java_package_name}}.dto.response.CustomerSummaryResponse;
import ${{values.java_package_name}}.dto.response.CreditScoreResponse;
import ${{values.java_package_name}}.dto.response.PageResponse;
import ${{values.java_package_name}}.enums.CustomerType;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class CustomerTools {

    @RestClient
    CustomerClient customerClient;

    // ============================================
    // CUSTOMER CREATION & RETRIEVAL
    // ============================================

    @Tool(description = "Create a new customer in NeuralBank system. Returns the complete customer information including generated ID.")
    public String createCustomer(
            @ToolArg(description = "Customer identification number (RUT for Chile, DNI for other countries). Example: 12345678-9") String identificacion,
            @ToolArg(description = "Type of identification document: RUT, DNI, PASSPORT, SSN, RFC, CURP, CPF, CUIT, RUN, NINO, CIF, EIN, TAX_ID") String tipoIdentificacion,
            @ToolArg(description = "Customer first name") String nombre,
            @ToolArg(description = "Customer last name") String apellido,
            @ToolArg(description = "Customer email address") String email,
            @ToolArg(description = "Customer phone number with country code. Example: +56912345678") String telefono,
            @ToolArg(description = "City where customer resides") String ciudad,
            @ToolArg(description = "Country ID (1 for Chile, 2 for Argentina, etc). Can be passed as a number (e.g., 1) or string (e.g., \"1\")") String paisId,
            @ToolArg(description = "Customer type: PERSONAL, EMPRESARIAL, or CORPORATIVO", required = false) String tipoCliente,
            @ToolArg(description = "Initial credit score (0-1000). Can be passed as a number (e.g., 750.5) or string (e.g., \"750.5\")", required = false) String scoreCrediticio,
            @ToolArg(description = "Customer address", required = false) String direccion,
            @ToolArg(description = "Postal code", required = false) String codigoPostal,
            @ToolArg(description = "State or province", required = false) String estadoProvincia
    ) {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.identificacion = identificacion;
        request.tipoIdentificacion = tipoIdentificacion;
        request.nombre = nombre;
        request.apellido = apellido;
        request.email = email;
        request.telefono = telefono;
        try {
            request.ciudad = ciudad;
            request.paisId = parseLong(paisId, "paisId");
            request.direccion = direccion;
            request.codigoPostal = codigoPostal;
            request.estadoProvincia = estadoProvincia;
            
            if (tipoCliente != null) {
                request.tipoCliente = CustomerType.valueOf(tipoCliente.toUpperCase());
            }
            
            if (scoreCrediticio != null) {
                Double scoreValue = parseDouble(scoreCrediticio, "scoreCrediticio");
                request.scoreCrediticio = BigDecimal.valueOf(scoreValue);
            }
            
            CustomerResponse response = customerClient.createCustomer(request);
            return formatCustomerResponse(response);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get complete customer information by their unique ID. Accepts customer ID as a number (e.g., 2) or string (e.g., \"2\").")
    public String getCustomer(
            @ToolArg(description = "Customer unique ID in the system (can be provided as number or string)") String customerId
    ) {
        try {
            Long customerIdLong = parseLong(customerId, "customerId");
            CustomerResponse response = customerClient.getCustomerById(customerIdLong);
            return formatCustomerResponse(response);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Find a customer by their identification number (RUT, DNI, passport, etc).")
    public String getCustomerByIdentification(
            @ToolArg(description = "Customer identification number. Example: 12345678-9 for Chilean RUT") String identificacion
    ) {
        CustomerResponse response = customerClient.getCustomerByIdentificacion(identificacion);
        return formatCustomerResponse(response);
    }

    // ============================================
    // CUSTOMER SEARCH
    // ============================================

    @Tool(description = "Search for customers using various filters. Returns a paginated list of customers matching the criteria.")
    public String searchCustomers(
            @ToolArg(description = "Page number (0-based). Can be passed as a number (e.g., 0) or string (e.g., \"0\")", required = false) String page,
            @ToolArg(description = "Number of results per page (default: 20). Can be passed as a number (e.g., 20) or string (e.g., \"20\")", required = false) String size,
            @ToolArg(description = "Search term to find in customer name, email, or identification", required = false) String search,
            @ToolArg(description = "Filter by customer type: PERSONAL, EMPRESARIAL, CORPORATIVO", required = false) String tipoCliente,
            @ToolArg(description = "Filter by city", required = false) String ciudad,
            @ToolArg(description = "Filter by country ID. Can be passed as a number (e.g., 1) or string (e.g., \"1\")", required = false) String paisId,
            @ToolArg(description = "Filter by branch office ID. Can be passed as a number (e.g., 5) or string (e.g., \"5\")", required = false) String sucursalId,
            @ToolArg(description = "Filter by assigned executive ID. Can be passed as a number (e.g., 10) or string (e.g., \"10\")", required = false) String ejecutivoId,
            @ToolArg(description = "Filter by risk level: Bajo, Medio, Alto, Muy Alto", required = false) String nivelRiesgo,
            @ToolArg(description = "Minimum credit score filter. Can be passed as a number (e.g., 600.0) or string (e.g., \"600.0\")", required = false) String scoreMin,
            @ToolArg(description = "Maximum credit score filter. Can be passed as a number (e.g., 900.0) or string (e.g., \"900.0\")", required = false) String scoreMax,
            @ToolArg(description = "Filter by active status (true for active customers only). Can be passed as boolean (e.g., true) or string (e.g., \"true\", \"1\", \"yes\", \"on\" for true; \"false\", \"0\", \"no\", \"off\" for false)", required = false) String activo
    ) {
        try {
            CustomerType customerType = tipoCliente != null ? CustomerType.valueOf(tipoCliente.toUpperCase()) : null;
            BigDecimal scoreMinDecimal = scoreMin != null ? BigDecimal.valueOf(parseDouble(scoreMin, "scoreMin")) : null;
            BigDecimal scoreMaxDecimal = scoreMax != null ? BigDecimal.valueOf(parseDouble(scoreMax, "scoreMax")) : null;
            
            PageResponse<CustomerResponse> response = customerClient.searchCustomers(
                    page != null ? parseInt(page, "page") : 0,
                    size != null ? parseInt(size, "size") : 20,
                    search,
                    customerType,
                    ciudad,
                    parseLong(paisId, "paisId"),
                    parseLong(sucursalId, "sucursalId"),
                    parseLong(ejecutivoId, "ejecutivoId"),
                    nivelRiesgo,
                    scoreMinDecimal,
                    scoreMaxDecimal,
                    parseBoolean(activo, "activo")
            );
            
            return formatPageResponse(response);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    // ============================================
    // CUSTOMER UPDATE
    // ============================================

    @Tool(description = "Update customer information. Only provided fields will be updated, others remain unchanged.")
    public String updateCustomer(
            @ToolArg(description = "Customer ID to update. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId,
            @ToolArg(description = "New first name", required = false) String nombre,
            @ToolArg(description = "New last name", required = false) String apellido,
            @ToolArg(description = "New email address", required = false) String email,
            @ToolArg(description = "New phone number", required = false) String telefono,
            @ToolArg(description = "New address", required = false) String direccion,
            @ToolArg(description = "New city", required = false) String ciudad,
            @ToolArg(description = "New state or province", required = false) String estadoProvincia,
            @ToolArg(description = "New postal code", required = false) String codigoPostal,
            @ToolArg(description = "New country ID. Can be passed as a number (e.g., 1) or string (e.g., \"1\")", required = false) String paisId,
            @ToolArg(description = "New customer type: PERSONAL, EMPRESARIAL, CORPORATIVO", required = false) String tipoCliente
    ) {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.nombre = nombre;
        request.apellido = apellido;
        request.email = email;
        request.telefono = telefono;
        request.direccion = direccion;
        request.ciudad = ciudad;
        request.estadoProvincia = estadoProvincia;
        try {
            request.codigoPostal = codigoPostal;
            request.paisId = parseLong(paisId, "paisId");
            
            if (tipoCliente != null) {
                request.tipoCliente = CustomerType.valueOf(tipoCliente.toUpperCase());
            }
            
            Long customerIdLong = parseLong(customerId, "customerId");
            CustomerResponse response = customerClient.updateCustomer(customerIdLong, request);
            return formatCustomerResponse(response);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    // ============================================
    // CREDIT SCORE OPERATIONS
    // ============================================

    @Tool(description = "Get the current credit score and risk evaluation for a customer.")
    public String getCreditScore(
            @ToolArg(description = "Customer ID to get credit score for. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId
    ) {
        try {
            Long customerIdLong = parseLong(customerId, "customerId");
            CreditScoreResponse response = customerClient.getCreditScore(customerIdLong);
            return formatCreditScoreResponse(response);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Recalculate the credit score for a customer based on current financial data and payment history.")
    public String calculateCreditScore(
            @ToolArg(description = "Customer ID to recalculate credit score for. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId
    ) {
        try {
            Long customerIdLong = parseLong(customerId, "customerId");
            CreditScoreResponse response = customerClient.calculateCreditScore(customerIdLong);
            return formatCreditScoreResponse(response);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    /* 
    @Tool(description = "Update the risk level for a customer with justification.")
    public String updateRiskLevel(
            @ToolArg(description = "Customer ID. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId,
            @ToolArg(description = "New risk level: Bajo, Medio, Alto, Muy Alto") String nivelRiesgo,
            @ToolArg(description = "Justification for the risk level change") String justificacion
    ) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("nivelRiesgo", nivelRiesgo);
            body.put("justificacion", justificacion);
            
            Long customerIdLong = parseLong(customerId, "customerId");
            customerClient.updateRiskLevel(customerIdLong, body);
            return "Risk level updated successfully to: " + nivelRiesgo;
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }
    */

    // ============================================
    // CUSTOMER STATUS MANAGEMENT
    // ============================================

    @Tool(description = "Activate a customer account, allowing them to perform transactions and use banking services.")
    public String activateCustomer(
            @ToolArg(description = "Customer ID to activate. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId
    ) {
        try {
            Long customerIdLong = parseLong(customerId, "customerId");
            customerClient.activateCustomer(customerIdLong);
            return "Customer " + customerId + " has been activated successfully.";
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Deactivate a customer account, preventing them from performing new transactions.")
    public String deactivateCustomer(
            @ToolArg(description = "Customer ID to deactivate. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId,
            @ToolArg(description = "Reason for deactivation") String motivo
    ) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("motivo", motivo);
            
            Long customerIdLong = parseLong(customerId, "customerId");
            customerClient.deactivateCustomer(customerIdLong, body);
            return "Customer " + customerId + " has been deactivated. Reason: " + motivo;
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Block a customer account due to suspicious activity, fraud, or security concerns.")
    public String blockCustomer(
            @ToolArg(description = "Customer ID to block. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId,
            @ToolArg(description = "Reason for blocking") String motivo,
            @ToolArg(description = "Additional comments about the blocking", required = false) String comentarios
    ) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("motivo", motivo);
            if (comentarios != null) {
                body.put("comentarios", comentarios);
            }
            
            Long customerIdLong = parseLong(customerId, "customerId");
            customerClient.blockCustomer(customerIdLong, body);
            return "Customer " + customerId + " has been blocked. Reason: " + motivo;
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Unblock a previously blocked customer account.")
    public String unblockCustomer(
            @ToolArg(description = "Customer ID to unblock. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId
    ) {
        try {
            Long customerIdLong = parseLong(customerId, "customerId");
            customerClient.unblockCustomer(customerIdLong);
            return "Customer " + customerId + " has been unblocked successfully.";
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    // ============================================
    // CUSTOMER SUMMARY
    // ============================================

    @Tool(description = "Get a comprehensive summary of customer information including personal data, credit score, accounts, and recent activity.")
    public String getCustomerSummary(
            @ToolArg(description = "Customer ID to get summary for. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId
    ) {
        try {
            Long customerIdLong = parseLong(customerId, "customerId");
            CustomerSummaryResponse response = customerClient.getCustomerSummary(customerIdLong);
            return formatCustomerSummary(response);
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    // ============================================
    // EXECUTIVE ASSIGNMENT
    // ============================================

    @Tool(description = "Assign or change the account executive responsible for managing this customer relationship.")
    public String assignExecutive(
            @ToolArg(description = "Customer ID. Can be passed as a number (e.g., 2) or string (e.g., \"2\")") String customerId,
            @ToolArg(description = "Executive ID to assign. Can be passed as a number (e.g., 10) or string (e.g., \"10\")") String executiveId
    ) {
        try {
            Long customerIdLong = parseLong(customerId, "customerId");
            Long executiveIdLong = parseLong(executiveId, "executiveId");
            customerClient.assignExecutive(customerIdLong, executiveIdLong);
            return "Executive " + executiveId + " has been assigned to customer " + customerId + " successfully.";
        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    // ============================================
    // HELPER PARSING METHODS
    // ============================================

    private Long parseLong(String value, String paramName) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + paramName + ": must be a valid number. Provided value: " + value);
        }
    }

    private Integer parseInt(String value, String paramName) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + paramName + ": must be a valid number. Provided value: " + value);
        }
    }

    private Double parseDouble(String value, String paramName) {
        if (value == null) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + paramName + ": must be a valid number. Provided value: " + value);
        }
    }

    private Boolean parseBoolean(String value, String paramName) {
        if (value == null) return null;
        String strValue = value.trim().toLowerCase();
        if (strValue.equals("true") || strValue.equals("1") || strValue.equals("yes") || strValue.equals("on")) {
            return true;
        } else if (strValue.equals("false") || strValue.equals("0") || strValue.equals("no") || strValue.equals("off")) {
            return false;
        } else {
            throw new IllegalArgumentException("Invalid " + paramName + ": must be a valid boolean value (true/false, 1/0, yes/no, on/off). Provided value: " + value);
        }
    }

    // ============================================
    // HELPER FORMATTING METHODS
    // ============================================

    private String formatCustomerResponse(CustomerResponse customer) {
        StringBuilder sb = new StringBuilder();
        sb.append("Customer Information:\n");
        sb.append("===================\n");
        sb.append("ID: ").append(customer.id).append("\n");
        sb.append("Name: ").append(customer.nombre).append(" ").append(customer.apellido).append("\n");
        sb.append("Identification: ").append(customer.identificacion).append(" (").append(customer.tipoIdentificacion).append(")\n");
        sb.append("Email: ").append(customer.email).append("\n");
        sb.append("Phone: ").append(customer.telefono).append("\n");
        
        if (customer.ciudad != null) {
            sb.append("City: ").append(customer.ciudad);
            if (customer.estadoProvincia != null) {
                sb.append(", ").append(customer.estadoProvincia);
            }
            sb.append("\n");
        }
        
        sb.append("Customer Type: ").append(customer.tipoCliente).append("\n");
        sb.append("Active: ").append(customer.activo ? "Yes" : "No").append("\n");
        
        if (customer.scoreCrediticio != null) {
            sb.append("Credit Score: ").append(customer.scoreCrediticio).append("\n");
        }
        
        if (customer.nivelRiesgo != null) {
            sb.append("Risk Level: ").append(customer.nivelRiesgo).append("\n");
        }
        
        if (customer.ejecutivoId != null) {
            sb.append("Executive ID: ").append(customer.ejecutivoId).append("\n");
        }
        
        sb.append("Registration Date: ").append(customer.fechaRegistro).append("\n");
        
        return sb.toString();
    }

    private String formatCreditScoreResponse(CreditScoreResponse score) {
        StringBuilder sb = new StringBuilder();
        sb.append("Credit Score Information:\n");
        sb.append("=======================\n");
        sb.append("Customer ID: ").append(score.customerId).append("\n");
        sb.append("Score: ").append(score.score).append("\n");
        sb.append("Risk Level: ").append(score.nivelRiesgo).append("\n");
        sb.append("Evaluation: ").append(score.evaluacion).append("\n");
        sb.append("Calculated At: ").append(score.calculatedAt).append("\n");
        return sb.toString();
    }

    private String formatPageResponse(PageResponse<CustomerResponse> page) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search Results:\n");
        sb.append("==============\n");
        sb.append("Total Customers: ").append(page.totalElements).append("\n");
        sb.append("Page: ").append(page.currentPage + 1).append(" of ").append(page.totalPages).append("\n");
        sb.append("Customers on this page: ").append(page.numberOfElements).append("\n\n");
        
        for (int i = 0; i < page.content.size(); i++) {
            CustomerResponse customer = page.content.get(i);
            sb.append((i + 1)).append(". ");
            sb.append(customer.nombre).append(" ").append(customer.apellido);
            sb.append(" (ID: ").append(customer.id).append(")");
            sb.append(" - ").append(customer.identificacion);
            sb.append(" - ").append(customer.email);
            if (customer.scoreCrediticio != null) {
                sb.append(" - Score: ").append(customer.scoreCrediticio);
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }

    private String formatCustomerSummary(CustomerSummaryResponse summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Customer Summary:\n");
        sb.append("================\n");
        // Note: The actual structure depends on CustomerSummaryResponse implementation
        // This is a placeholder that should be adjusted based on the actual DTO
        sb.append(summary.toString());
        return sb.toString();
    }
}