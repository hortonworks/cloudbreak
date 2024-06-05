package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RENEW_CERTIFICATE_INTERNAL;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxInternalClusterRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/internal/sdx")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/internal/sdx")
public interface SdxInternalEndpoint {

    @POST
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "create internal SDX cluster", operationId = "createInternalSdx",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxClusterResponse create(@PathParam("name") String name, @Valid SdxInternalClusterRequest createSdxClusterRequest);

    @POST
    @Path("crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RENEW_CERTIFICATE_INTERNAL, description = Notes.RENEW_CERTIFICATE_NOTES, operationId = "renewInternalSdxCertificate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier renewCertificate(@PathParam("crn") String crn);

    @PUT
    @Path("crn/{crn}/updateDbEngineVersion")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "update the db engine version in the service db", operationId = "updateDbEngineVersion",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void updateDbEngineVersion(@PathParam("crn") String crn,
            @Pattern(regexp = POSTGRES_VERSION_REGEX, message = "Not a valid database major version") @QueryParam("dbEngineVersion") String dbEngineVersion);

    @PUT
    @Path("crn/{crn}/modify_proxy")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Initiates the modification of the proxy config", operationId = "modifyInternalSdxProxyConfig",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier modifyProxy(@ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @PathParam("crn") String crn,
            @ValidCrn(resource = CrnResourceDescriptor.PROXY) @QueryParam("previousProxy") String previousProxyCrn,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);
}
