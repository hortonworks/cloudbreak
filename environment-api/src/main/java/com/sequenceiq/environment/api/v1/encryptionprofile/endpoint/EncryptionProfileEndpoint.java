package com.sequenceiq.environment.api.v1.encryptionprofile.endpoint;

import static com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileDescriptor.ENCRYPTION_PROFILE_DESCRIPTION;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileDescriptor;
import com.sequenceiq.environment.api.doc.encryptionprofile.EncryptionProfileOpDescription;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.CipherSuitesByTlsVersionResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponses;
import com.sequenceiq.flow.api.model.FlowIdentifier;

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
    @Path("default")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EncryptionProfileOpDescription.GET_DEFAULT,
            description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES, operationId = "getDefaultEncryptionProfile",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EncryptionProfileResponse getDefaultEncryptionProfile();

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

    @GET
    @Path("list_ciphers_by_tls")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EncryptionProfileOpDescription.LIST_CIPHERS_BY_TLS, description = EncryptionProfileDescriptor.ENCRYPTION_PROFILE_NOTES,
            operationId = "listCiphersByTls",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CipherSuitesByTlsVersionResponse listCiphersByTlsVersion();

    @PUT
    @Path("crn/{encryptionProfileCrn}/enable_encryption_profile")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Enable encryption profile in the given environment by crn",
            operationId = "enableEncryptionProfileByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier enableEncryptionProfileByCrn(@NotEmpty @ValidCrn(resource = CrnResourceDescriptor.ENCYRPTION_PROFILE)
            @PathParam("encryptionProfileCrn") String encryptionProfileCrn, @QueryParam("envNameOrCrn") String envNameOrCrn);

    @PUT
    @Path("name/{encryptionProfileName}/enable_encryption_profile")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Enable encryption profile in the given environment by name",
            operationId = "enableEncryptionProfileByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier enableEncryptionProfileByName(@NotEmpty @PathParam("encryptionProfileName") String encryptionProfileName,
            @QueryParam("envNameOrCrn") String envNameOrCrn);

    @PUT
    @Path("disable_encryption_profile_by_crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Disable encryption profile in the given environment by crn",
            operationId = "disableEncryptionProfileByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier disableEncryptionProfileByCrn(@NotEmpty @QueryParam("envCrn") String envCrn);

    @PUT
    @Path("disable_encryption_profile_by_name")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Disable encryption profile in the given environment by name",
            operationId = "disableEncryptionProfileByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier disableEncryptionProfileByName(@NotEmpty @QueryParam("envName") String envName);
}
