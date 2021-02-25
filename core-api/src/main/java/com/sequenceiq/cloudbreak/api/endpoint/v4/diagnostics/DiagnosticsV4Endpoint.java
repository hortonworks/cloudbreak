package com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.docs.DiagnosticsOperationDescriptions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.node.status.response.NodeStatusResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/diagnostics")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/diagnostics", description = "Diagnostics in Stacks", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DiagnosticsV4Endpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.COLLECT_DIAGNOSTICS, produces = MediaType.APPLICATION_JSON,
            nickname = "collectStackCmDiagnostics")
    FlowIdentifier collectDiagnostics(@Valid DiagnosticsCollectionRequest request);

    @GET
    @Path("logs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_VM_LOG_PATHS, produces = MediaType.APPLICATION_JSON,
            nickname = "getStackCmVmLogs")
    VmLogsResponse getVmLogs();

    @GET
    @Path("{crn}/collections")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.LIST_COLLECTIONS, produces = MediaType.APPLICATION_JSON,
            nickname = "listStackDiagnosticsCollections")
    ListDiagnosticsCollectionResponse listCollections(@PathParam("crn") String crn);

    @POST
    @Path("{crn}/collections/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.CANCEL_COLLECTIONS, produces = MediaType.APPLICATION_JSON,
            nickname = "cancelStackDiagnosticsCollections")
    void cancelCollections(@PathParam("crn") String crn);

    @POST
    @Path("cm")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.COLLECT_CM_DIAGNOSTICS, produces = MediaType.APPLICATION_JSON,
            nickname = "collectCmDiagnostics")
    FlowIdentifier collectCmDiagnostics(@Valid CmDiagnosticsCollectionRequest request);

    @GET
    @Path("cm/{stackCrn}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_CM_ROLES, produces = MediaType.APPLICATION_JSON,
            nickname = "getCmRoles")
    List<String> getCmRoles(@PathParam("stackCrn") String stackCrn);

    @GET
    @Path("report/{stackCrn}/metering")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_METERING_REPORT, produces = MediaType.APPLICATION_JSON, nickname = "getMeteringReport")
    NodeStatusResponse getMeteringReport(@PathParam("stackCrn") String stackCrn);

    @GET
    @Path("report/{stackCrn}/network")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_NETWORK_REPORT, produces = MediaType.APPLICATION_JSON, nickname = "getNetworkReport")
    NodeStatusResponse getNetworkReport(@PathParam("stackCrn") String stackCrn);

    @GET
    @Path("report/{stackCrn}/services")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_SERVICES_REPORT, produces = MediaType.APPLICATION_JSON, nickname = "getServicesReport")
    NodeStatusResponse getServicesReport(@PathParam("stackCrn") String stackCrn);
}
