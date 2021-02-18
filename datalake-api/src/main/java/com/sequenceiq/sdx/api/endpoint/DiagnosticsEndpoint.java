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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/diagnostics")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/diagnostics", description = "Diagnostics for SDX", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DiagnosticsEndpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.COLLECT_DIAGNOSTICS, produces = MediaType.APPLICATION_JSON,
            nickname = "collectSdxCmDiagnostics")
    FlowIdentifier collectDiagnostics(@Valid DiagnosticsCollectionRequest request);

    @GET
    @Path("logs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_VM_LOG_PATHS, produces = MediaType.APPLICATION_JSON,
            nickname = "getSdxCmVmLogs")
    VmLogsResponse getVmLogs();

    @GET
    @Path("{crn}/collections")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.LIST_COLLECTIONS, produces = MediaType.APPLICATION_JSON,
            nickname = "listSdxDiagnosticsCollections")
    ListDiagnosticsCollectionResponse listCollections(@PathParam("crn") String crn);

    @POST
    @Path("{crn}/collections/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.CANCEL_COLLECTIONS, produces = MediaType.APPLICATION_JSON,
            nickname = "cancelSdxDiagnosticsCollections")
    void cancelCollections(@PathParam("crn") String crn);

    @POST
    @Path("cm")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.COLLECT_CM_DIAGNOSTICS,
            produces = MediaType.APPLICATION_JSON, nickname = "collectSdxCmBasedDiagnostics")
    FlowIdentifier collectCmDiagnostics(@Valid CmDiagnosticsCollectionRequest request);

    @GET
    @Path("cm/{stackCrn}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_CM_ROLES,
            produces = MediaType.APPLICATION_JSON, nickname = "getSdxCmRoles")
    List<String> getCmRoles(@PathParam("stackCrn") String stackCrn);
}
