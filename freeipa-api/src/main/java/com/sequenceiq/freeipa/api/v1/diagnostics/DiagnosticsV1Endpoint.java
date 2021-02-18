package com.sequenceiq.freeipa.api.v1.diagnostics;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.diagnostics.docs.DiagnosticsOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/diagnostics")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/diagnostics", description = "Diagnostics in FreeIPA", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DiagnosticsV1Endpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.COLLECT_FREEIPA_DIAGNOSTICS, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "collectFreeIpaDiagnosticsV1")
    FlowIdentifier collectDiagnostics(@Valid DiagnosticsCollectionRequest request);

    @GET
    @Path("logs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.GET_VM_LOG_PATHS, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "getFreeIpaVmLogsV1")
    VmLogsResponse getVmLogs();

    @GET
    @Path("{environmentCrn}/collections")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.LIST_DIAGNOSTICS_COLLECTIONS, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "listDiagnosticsCollectionsV1")
    ListDiagnosticsCollectionResponse listDiagnosticsCollections(@PathParam("environmentCrn") String environmentCrn);

    @POST
    @Path("{environmentCrn}/collections/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DiagnosticsOperationDescriptions.CANCEL_DIAGNOSTICS_COLLECTIONS, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "cancelDiagnosticsCollectionsV1")
    void cancelCollections(@PathParam("environmentCrn") String environmentCrn);
}
