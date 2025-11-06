package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_CRNS_INTERNAL;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_CRN_INSTANCES_INTERNAL;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_BY_CRN_INTERNAL;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.LIST;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.RENEW_CERTIFICATE_INTERNAL;

import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StatusCrnsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackInstancesV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackOutboundTypeValidationV4Response;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/internal/distrox")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/internal/distrox")
public interface DistroXInternalV1Endpoint {

    @GET
    @Path("crn/{crn}")
    @Operation(summary = GET_BY_CRN_INTERNAL, description = Notes.STACK_NOTES,
            operationId = "getDistroXInternalV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackViewV4Response getByCrn(@PathParam("crn") String crn);

    @GET
    @Path("crn/{crn}/instances")
    @Operation(summary = GET_BY_CRN_INSTANCES_INTERNAL, description = Notes.STACK_NOTES,
            operationId = "getDistroXInstancesInternalV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackInstancesV4Responses getInstancesByCrn(@ValidCrn(resource = {CrnResourceDescriptor.DATAHUB}) @PathParam("crn") String crn);

    @POST
    @Path("crn/status")
    @Operation(summary = GET_BY_CRNS_INTERNAL, description = Notes.STACK_NOTES,
            operationId = "getDistroXStatusInternalV1ByCrns",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackStatusV4Responses getStatusByCrns(StatusCrnsV4Request request);

    @GET
    @Path("list")
    @Operation(summary = LIST, description = Notes.STACK_NOTES,
            operationId = "listDistroXInternalV1ByEnvCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackViewV4Responses list(@QueryParam("environmentCrn") String environmentCrn);

    @POST
    @Path("crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RENEW_CERTIFICATE_INTERNAL, description = Notes.RENEW_CERTIFICATE_NOTES,
            operationId = "renewInternalDistroXCertificate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier renewCertificate(@PathParam("crn") String crn);

    @POST
    @Path("internal/{envCrn}/validate_outbound")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Validates outbound type for all the stacks in an environment CRN",
            operationId = "validateStackDefaultOutboundInternalWithNoUserCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackOutboundTypeValidationV4Response validateStackOutboundTypes(
            @PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("envCrn") String envCrn);
}
