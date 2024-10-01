package com.sequenceiq.cloudbreak.api.endpoint.v4.encryption;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.EncryptionOpDescription.GET_ENCRYPTION_KEYS;

import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.encryption.response.StackEncryptionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/encryption")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "v4/encryption")
public interface EncryptionV4Endpoint {

    @GET
    @Path("keys")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_ENCRYPTION_KEYS, operationId = "getEncryptionKeysV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackEncryptionResponse getEncryptionKeys(@QueryParam("crn") @NotEmpty String crn);
}
