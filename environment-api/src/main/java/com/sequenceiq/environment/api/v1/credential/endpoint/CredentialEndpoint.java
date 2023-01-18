package com.sequenceiq.environment.api.v1.credential.endpoint;

import static com.sequenceiq.environment.api.doc.credential.CredentialDescriptor.CREDENTIAL_DESCRIPTION;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialOpDescription;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.EditCredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;
import com.sequenceiq.environment.api.v1.credential.model.response.InteractiveCredentialResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/credentials", description = CREDENTIAL_DESCRIPTION)
public interface CredentialEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.LIST, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "listCredentialsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponses list();

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.GET_BY_NAME,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getCredentialByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse getByName(@PathParam("name") String credentialName);

    @GET
    @Path("environment/crn/{environmentCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.GET_BY_ENVIRONMENT_CRN,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getCredentialByEnvironmentCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse getByEnvironmentCrn(@PathParam("environmentCrn") String environmentCrn);

    @GET
    @Path("environment/name/{environmentName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.GET_BY_ENVIRONMENT_NAME,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getCredentialByEnvironmentNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse getByEnvironmentName(@PathParam("environmentName") String environmentName);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.GET_BY_CRN,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getCredentialByResourceCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse getByResourceCrn(@PathParam("crn") String credentialCrn);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.CREATE, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "createCredentialV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse create(@Valid CredentialRequest request);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.DELETE_BY_NAME,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "deleteCredentialByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse deleteByName(@PathParam("name") String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.DELETE_BY_CRN,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "deleteCredentialByResourceCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse deleteByResourceCrn(@PathParam("crn") String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.DELETE_MULTIPLE_BY_NAME,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "deleteCredentialsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponses deleteMultiple(Set<String> names);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.PUT, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "putCredentialV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse modify(@Valid EditCredentialRequest credentialRequest);

    @POST
    @Path("interactive_login")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.INTERACTIVE_LOGIN, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "interactiveLoginCredentialV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    InteractiveCredentialResponse interactiveLogin(@Valid CredentialRequest credentialRequest);

    @GET
    @Path("prerequisites/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.GET_PREREQUISTIES_BY_CLOUD_PROVIDER,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getPrerequisitesForCloudPlatform",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(@PathParam("cloudPlatform") String platform,
            @QueryParam("govCloud") boolean govCloud,
            @QueryParam("deploymentAddress") String deploymentAddress);

    @POST
    @Path("code_grant_flow/init")
    @Operation(summary = CredentialOpDescription.INIT_CODE_GRANT_FLOW, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "codeGrantFlowBasedCredentialV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Response initCodeGrantFlow(@Valid CredentialRequest credentialRequest);

    @GET
    @Path("code_grant_flow/init/{name}")
    @Operation(summary = CredentialOpDescription.INIT_CODE_GRANT_FLOW_ON_EXISTING, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "codeGrantFlowOnExistingCredentialV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Response initCodeGrantFlowOnExisting(@PathParam("name") String name);

    @GET
    @Path("code_grant_flow/authorization/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.AUTHORIZE_CODE_GRANT_FLOW,
            operationId = "authorizeCodeGrantFlowBasedCredentialV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = "Authorize code grant flow based credential creation.")
    CredentialResponse authorizeCodeGrantFlow(@PathParam("cloudPlatform") String platform,
            @QueryParam("code") String code, @QueryParam("state") String state);

    @GET
    @Path("/name/{name}/verify")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.VERIFY_BR_NAME, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "verifyCredentialByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse verifyByName(@PathParam("name") String name);

    @GET
    @Path("/crn/{crn}/verify")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.VERIFY_BR_CRN, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "verifyCredentialByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse verifyByCrn(@PathParam("crn") String crn);

    @POST
    @Path("cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CredentialOpDescription.CLI_COMMAND, description = CredentialDescriptor.CREDENTIAL_NOTES,
            operationId = "getCreateCredentialForCli",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Object getCreateCredentialForCli(@NotNull @Valid CredentialRequest recipeV4Request);
}
