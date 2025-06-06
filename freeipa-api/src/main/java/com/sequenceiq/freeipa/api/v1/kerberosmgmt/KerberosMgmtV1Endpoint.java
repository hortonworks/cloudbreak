package com.sequenceiq.freeipa.api.v1.kerberosmgmt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabModelNotes;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.doc.KeytabOperationsDescription;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabResponse;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServicePrincipalRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.VaultCleanupRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/kerberosmgmt")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/kerberosmgmt")
public interface KerberosMgmtV1Endpoint {

    @POST
    @Path("servicekeytab")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KeytabOperationsDescription.DESCRIBE_GENERATE_SERVICE_KEYTAB,
            description = KeytabModelNotes.GENERATE_SERVICE_KEYTAB_NOTES, operationId = "generateServiceKeytabV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ServiceKeytabResponse generateServiceKeytab(@Valid ServiceKeytabRequest request, @QueryParam("accountId") String accountIdForInternalUsage);

    @GET
    @Path("servicekeytab")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KeytabOperationsDescription.DESCRIBE_SERVICE_KEYTAB,
            description = KeytabModelNotes.GET_SERVICE_KEYTAB_NOTES, operationId = "getServiceKeytabV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ServiceKeytabResponse getServiceKeytab(@Valid ServiceKeytabRequest request) throws Exception;

    @POST
    @Path("hostkeytab")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KeytabOperationsDescription.DESCRIBE_GENERATE_HOST_KEYTAB,
            description = KeytabModelNotes.GENERATE_HOST_KEYTAB_NOTES,
            operationId = "generateHostKeytabV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    HostKeytabResponse generateHostKeytab(@Valid HostKeytabRequest request) throws Exception;

    @GET
    @Path("hostkeytab")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KeytabOperationsDescription.DESCRIBE_HOST_KEYTAB,
            description = KeytabModelNotes.GET_HOST_KEYTAB_NOTES, operationId = "getHostKeytabV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    HostKeytabResponse getHostKeytab(@Valid HostKeytabRequest request) throws Exception;

    @GET
    @Path("userkeytab")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KeytabOperationsDescription.DESCRIBE_USER_KEYTAB,
            description = KeytabModelNotes.GET_USER_KEYTAB_NOTES, operationId = "getUserKeytabV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    String getUserKeytab(@NotEmpty @QueryParam("environmentCrn") String environmentCrn, @NotEmpty @QueryParam("userCrn") String userCrn);

    @DELETE
    @Path("serviceprincipal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KeytabOperationsDescription.DESCRIBE_DELETE_SERVICE_PRINCIPAL,
            description = KeytabModelNotes.DELETE_SERVICE_PRINCIPAL_NOTES,
            operationId = "deleteServicePrinciapalV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteServicePrincipal(@Valid ServicePrincipalRequest request);

    @DELETE
    @Path("host")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KeytabOperationsDescription.DESCRIBE_DELETE_HOST, description = KeytabModelNotes.DELETE_HOST_NOTES,
            operationId = "deleteHostV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteHost(@Valid HostRequest request) throws Exception;

    @DELETE
    @Path("cleanupClusterSecrets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KeytabOperationsDescription.DESCRIBE_CLUSTER_CLEANUP, description = KeytabModelNotes.CLEANUP_NOTES,
            operationId = "cleanupClusterSecretsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void cleanupClusterSecrets(@Valid VaultCleanupRequest request);

    @DELETE
    @Path("cleanupEnvironmentSecrets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KeytabOperationsDescription.DESCRIBE_ENVIRONMENT_CLEANUP,
            description = KeytabModelNotes.CLEANUP_NOTES, operationId = "cleanupEnvironmentSecretsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void cleanupEnvironmentSecrets(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);
}
