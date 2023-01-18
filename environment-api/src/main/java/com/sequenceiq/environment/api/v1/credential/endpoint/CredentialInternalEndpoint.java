package com.sequenceiq.environment.api.v1.credential.endpoint;

import static com.sequenceiq.environment.api.doc.credential.CredentialDescriptor.CREDENTIAL_DESCRIPTION;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

}
