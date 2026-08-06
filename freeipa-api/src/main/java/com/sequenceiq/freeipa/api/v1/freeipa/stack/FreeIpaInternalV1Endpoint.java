package com.sequenceiq.freeipa.api.v1.freeipa.stack;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.UpdateInstanceTypeRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/internal/freeipa")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/internal/freeipa")
public interface FreeIpaInternalV1Endpoint {

    @GET
    @Path("/default_outbound")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.INTERNAL_GET_OUTBOUND_TYPE_NO_ACTOR,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "internalGetDefaultOutboundByEnvironmentV2",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OutboundType getOutboundType(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn);

    @PUT
    @Path("/update_instance_type")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update instance type for FreeIPA cluster in FMS DB", operationId = "internalUpdateInstanceTypeInDatabase",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void updateInstanceTypeInDatabase(@Valid @NotNull UpdateInstanceTypeRequest request);
}
