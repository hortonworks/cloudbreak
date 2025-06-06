package com.sequenceiq.environment.api.v1.credential.endpoint;

import static com.sequenceiq.environment.api.doc.credential.CredentialDescriptor.CREDENTIAL_DESCRIPTION;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialOpDescription;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.EditCredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/credentials/audit")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/credentials/audit", description = CREDENTIAL_DESCRIPTION)
public interface AuditCredentialEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.LIST, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "listAuditCredentialsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponses list();

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.GET_BY_CRN,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getAuditCredentialByResourceCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse getByResourceCrn(@PathParam("crn") String credentialCrn);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.GET_BY_NAME,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getAuditCredentialByResourceNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse getByResourceName(@PathParam("name") String credentialName, @QueryParam("accountId") String accountId);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.CREATE, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "createAuditCredentialV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse post(@Valid CredentialRequest request);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.PUT, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "putAuditCredentialV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse put(@Valid EditCredentialRequest credentialRequest);

    @GET
    @Path("prerequisites/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.GET_PREREQUISTIES_BY_CLOUD_PROVIDER,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getAuditPrerequisitesForCloudPlatform",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(@PathParam("cloudPlatform") String platform,
        @QueryParam("govCloud") boolean govCloud,
        @QueryParam("deploymentAddress") String deploymentAddress);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.DELETE_BY_NAME,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "deleteAuditCredentialByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse deleteByName(@PathParam("name") String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.DELETE_BY_CRN,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "deleteAuditCredentialByResourceCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse deleteByResourceCrn(@PathParam("crn") String crn);
}
