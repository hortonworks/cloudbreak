package com.sequenceiq.environment.api.v1.encryptionprofile.endpoint;

import static com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileDescriptor.ENCRYPTION_PROFILE_DESCRIPTION;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileDescriptor;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileOpDescription;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/encryption_profile")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/encryption_profile", description = ENCRYPTION_PROFILE_DESCRIPTION)
public interface EncryptionProfileEndpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EncryptionProfileOpDescription.CREATE, description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES,
            operationId = "createEncryptionProfile",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EncryptionProfileResponse create(@Valid EncryptionProfileRequest request);
}
