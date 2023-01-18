package com.sequenceiq.sdx.api.endpoint;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.diagnostics.docs.DiagnosticsOperationDescriptions;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Path("/diagnostics")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/diagnostics", description = "Diagnostics for SDX")
public interface DiagnosticsEndpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.COLLECT_DIAGNOSTICS, operationId ="collectSdxCmDiagnostics")
    FlowIdentifier collectDiagnostics(@Valid DiagnosticsCollectionRequest request);

    @GET
    @Path("logs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.GET_VM_LOG_PATHS, operationId ="getSdxCmVmLogs")
    VmLogsResponse getVmLogs();

    @GET
    @Path("{crn}/collections")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.LIST_COLLECTIONS, operationId ="listSdxDiagnosticsCollections")
    ListDiagnosticsCollectionResponse listCollections(@PathParam("crn") String crn);

    @POST
    @Path("{crn}/collections/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.CANCEL_COLLECTIONS, operationId ="cancelSdxDiagnosticsCollections")
    void cancelCollections(@PathParam("crn") String crn);

    @POST
    @Path("cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.COLLECT_CM_DIAGNOSTICS, operationId ="collectSdxCmBasedDiagnostics")
    FlowIdentifier collectCmDiagnostics(@Valid CmDiagnosticsCollectionRequest request);

    @GET
    @Path("cm/{stackCrn}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.GET_CM_ROLES, operationId ="getSdxCmRoles")
    List<String> getCmRoles(@PathParam("stackCrn") String stackCrn);
}
