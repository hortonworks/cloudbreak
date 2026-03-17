package com.sequenceiq.environment.api.v1.credential.endpoint;

import static com.sequenceiq.environment.api.doc.credential.CredentialDescriptor.CREDENTIAL_DESCRIPTION;
import static com.sequenceiq.environment.api.doc.credential.CredentialOpDescription.GET_PREREQUISITES_INTERNAL_BY_CLOUD_PROVIDER;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/internal/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/internal/credentials", description = CREDENTIAL_DESCRIPTION)
public interface CredentialInternalEndpoint {

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get internal Credential (includes the deleted credentials as well)",
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getCredentialInternalByResourceCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse getByResourceCrn(@PathParam("crn") String credentialCrn);

    @GET
    @Path("prerequisites/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_PREREQUISITES_INTERNAL_BY_CLOUD_PROVIDER,
            description = CredentialDescriptor.CREDENTIAL_NOTES, operationId = "getPrerequisitesInternalForCloudPlatform",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(@PathParam("cloudPlatform") String platform,
            @QueryParam("govCloud") boolean govCloud);

}
