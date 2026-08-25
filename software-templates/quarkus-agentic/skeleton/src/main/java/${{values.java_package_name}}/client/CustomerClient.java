package ${{values.java_package_name}}.client;

import ${{values.java_package_name}}.dto.request.CreateCustomerRequest;
import ${{values.java_package_name}}.dto.request.UpdateCustomerRequest;
import ${{values.java_package_name}}.dto.response.CustomerResponse;
import ${{values.java_package_name}}.dto.response.CustomerSummaryResponse;
import ${{values.java_package_name}}.dto.response.CreditScoreResponse;
import ${{values.java_package_name}}.dto.response.PageResponse;
import ${{values.java_package_name}}.enums.CustomerType;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

import java.math.BigDecimal;
import java.util.Map;

@Path("/api/v1/customers")
@RegisterRestClient(configKey = "customerclient")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CustomerClient {

    // ============================================
    // CRUD OPERATIONS
    // ============================================

    /**
     * Create a new customer
     * POST /api/v1/customers
     */
    @POST
    CustomerResponse createCustomer(CreateCustomerRequest request);

    /**
     * Get customer by ID
     * GET /api/v1/customers/{customerId}
     */
    @GET
    @Path("/{customerId}")
    CustomerResponse getCustomerById(@PathParam("customerId") Long customerId);

    /**
     * Get customer by identification (RUT, DNI, etc)
     * GET /api/v1/customers/identification/{identificacion}
     */
    @GET
    @Path("/identification/{identificacion}")
    CustomerResponse getCustomerByIdentificacion(@PathParam("identificacion") String identificacion);

    /**
     * Update customer (full update)
     * PUT /api/v1/customers/{customerId}
     */
    @PUT
    @Path("/{customerId}")
    CustomerResponse updateCustomer(
            @PathParam("customerId") Long customerId,
            UpdateCustomerRequest request
    );

    /**
     * Partial update customer (PATCH)
     * PATCH /api/v1/customers/{customerId}
     */
    @PATCH
    @Path("/{customerId}")
    CustomerResponse patchCustomer(
            @PathParam("customerId") Long customerId,
            Map<String, Object> updates
    );

    /**
     * Delete customer (soft delete)
     * DELETE /api/v1/customers/{customerId}
     */
    @DELETE
    @Path("/{customerId}")
    void deleteCustomer(@PathParam("customerId") Long customerId);

    // ============================================
    // SEARCH OPERATIONS
    // ============================================

    /**
     * Search customers with filters
     * GET /api/v1/customers?filters...
     */
    @GET
    PageResponse<CustomerResponse> searchCustomers(
            @RestQuery("page") @DefaultValue("0") int page,
            @RestQuery("size") @DefaultValue("20") int size,
            @RestQuery("search") String search,
            @RestQuery("tipoCliente") CustomerType tipoCliente,
            @RestQuery("ciudad") String ciudad,
            @RestQuery("paisId") Long paisId,
            @RestQuery("sucursalId") Long sucursalId,
            @RestQuery("ejecutivoId") Long ejecutivoId,
            @RestQuery("nivelRiesgo") String nivelRiesgo,
            @RestQuery("scoreMin") BigDecimal scoreMin,
            @RestQuery("scoreMax") BigDecimal scoreMax,
            @RestQuery("activo") Boolean activo
    );

    // ============================================
    // CREDIT SCORE OPERATIONS
    // ============================================

    /**
     * Get credit score for customer
     * GET /api/v1/customers/{customerId}/credit-score
     */
    @GET
    @Path("/{customerId}/credit-score")
    CreditScoreResponse getCreditScore(@PathParam("customerId") Long customerId);

    /**
     * Calculate/recalculate credit score
     * POST /api/v1/customers/{customerId}/credit-score/calculate
     */
    @POST
    @Path("/{customerId}/credit-score/calculate")
    CreditScoreResponse calculateCreditScore(@PathParam("customerId") Long customerId);

    // ============================================
    // CUSTOMER STATUS OPERATIONS
    // ============================================

    /**
     * Activate customer
     * POST /api/v1/customers/{customerId}/activate
     */
    @POST
    @Path("/{customerId}/activate")
    void activateCustomer(@PathParam("customerId") Long customerId);

    /**
     * Deactivate customer
     * POST /api/v1/customers/{customerId}/deactivate
     */
    @POST
    @Path("/{customerId}/deactivate")
    void deactivateCustomer(
            @PathParam("customerId") Long customerId,
            Map<String, String> body
    );

    /**
     * Block customer
     * POST /api/v1/customers/{customerId}/block
     */
    @POST
    @Path("/{customerId}/block")
    void blockCustomer(
            @PathParam("customerId") Long customerId,
            Map<String, String> body
    );

    /**
     * Unblock customer
     * POST /api/v1/customers/{customerId}/unblock
     */
    @POST
    @Path("/{customerId}/unblock")
    void unblockCustomer(@PathParam("customerId") Long customerId);

    // ============================================
    // SPECIFIC DATA UPDATE OPERATIONS
    // ============================================

    /**
     * Update customer email
     * PUT /api/v1/customers/{customerId}/email
     */
    @PUT
    @Path("/{customerId}/email")
    void updateEmail(
            @PathParam("customerId") Long customerId,
            Map<String, String> body
    );

    /**
     * Update customer phone
     * PUT /api/v1/customers/{customerId}/phone
     */
    @PUT
    @Path("/{customerId}/phone")
    void updatePhone(
            @PathParam("customerId") Long customerId,
            Map<String, String> body
    );

    /**
     * Update customer address
     * PUT /api/v1/customers/{customerId}/address
     */
    @PUT
    @Path("/{customerId}/address")
    void updateAddress(
            @PathParam("customerId") Long customerId,
            Map<String, String> body
    );

    /**
     * Update risk level
     * PUT /api/v1/customers/{customerId}/risk-level
     */
    @PUT
    @Path("/{customerId}/risk-level")
    void updateRiskLevel(
            @PathParam("customerId") Long customerId,
            Map<String, String> body
    );

    /**
     * Assign executive to customer
     * PUT /api/v1/customers/{customerId}/executive/{executiveId}
     */
    @PUT
    @Path("/{customerId}/executive/{executiveId}")
    void assignExecutive(
            @PathParam("customerId") Long customerId,
            @PathParam("executiveId") Long executiveId
    );

    // ============================================
    // METADATA OPERATIONS
    // ============================================

    /**
     * Get customer metadata
     * GET /api/v1/customers/{customerId}/metadata
     */
    @GET
    @Path("/{customerId}/metadata")
    Map<String, Object> getMetadata(@PathParam("customerId") Long customerId);

    /**
     * Update complete metadata
     * PUT /api/v1/customers/{customerId}/metadata
     */
    @PUT
    @Path("/{customerId}/metadata")
    void updateMetadata(
            @PathParam("customerId") Long customerId,
            Map<String, Object> metadata
    );

    /**
     * Update specific metadata field
     * PATCH /api/v1/customers/{customerId}/metadata/{key}
     */
    @PATCH
    @Path("/{customerId}/metadata/{key}")
    void updateMetadataField(
            @PathParam("customerId") Long customerId,
            @PathParam("key") String key,
            Object value
    );

    // ============================================
    // SUMMARY OPERATIONS
    // ============================================

    /**
     * Get complete customer summary
     * GET /api/v1/customers/{customerId}/summary
     */
    @GET
    @Path("/{customerId}/summary")
    CustomerSummaryResponse getCustomerSummary(@PathParam("customerId") Long customerId);
}