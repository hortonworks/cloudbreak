package com.sequenceiq.environment.api.v1.encryptionprofile.endpoint;

import static com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileDescriptor.ENCRYPTION_PROFILE_DESCRIPTION;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileDescriptor;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileOpDescription;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponses;

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

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EncryptionProfileOpDescription.GET_BY_NAME,
            description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES, operationId = "getEncryptionProfileByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EncryptionProfileResponse getByName(@PathParam("name") String encryptionProfileName);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EncryptionProfileOpDescription.GET_BY_CRN,
            description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES, operationId = "getEncryptionProfileByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EncryptionProfileResponse getByCrn(@PathParam("crn") String encryptionProfileCrn);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EncryptionProfileOpDescription.LIST, description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES,
            operationId = "listEncryptionProfiles",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EncryptionProfileResponses list();

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EncryptionProfileOpDescription.DELETE_BY_NAME,
            description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES, operationId = "deleteEncryptionProfileByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EncryptionProfileResponse deleteByName(@PathParam("name") String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EncryptionProfileOpDescription.DELETE_BY_CRN,
            description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES, operationId = "deleteEncryptionProfileByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EncryptionProfileResponse deleteByCrn(@PathParam("crn") String crn);
}
