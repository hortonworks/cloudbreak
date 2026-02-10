package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.VM_DATALAKE;

import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/sdx/encryption_profile")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx/encryption_profile")
public interface SdxEncryptionProfileEndpoint {

    @PUT
    @Path("{name}/enable")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Enable encryption profile by cluster name",
            operationId = "enableEncryptionProfileByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier enableEncryptionProfileByName(@NotEmpty @PathParam("name") String name,
            @QueryParam("encryptionProfileNameOrCrn") String encryptionProfileNameOrCrn);

    @PUT
    @Path("crn/{crn}/enable")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Enable encryption profile by cluster CRN",
            operationId = "enableEncryptionProfileByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier enableEncryptionProfileByCrn(@NotEmpty @ValidCrn(resource = {VM_DATALAKE}) @PathParam("crn") String crn,
            @QueryParam("encryptionProfileNameOrCrn") String encryptionProfileNameOrCrn);

    @PUT
    @Path("crn/{crn}/disable")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Disable encryption profile in the given cluster by crn",
            operationId = "disableEncryptionProfileByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier disableEncryptionProfileByCrn(@NotEmpty @PathParam("crn") String crn);

    @PUT
    @Path("{name}/disable")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Disable encryption profile in the given cluster by name",
            operationId = "disableEncryptionProfileByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier disableEncryptionProfileByName(@NotEmpty @PathParam("name") String name);

}
