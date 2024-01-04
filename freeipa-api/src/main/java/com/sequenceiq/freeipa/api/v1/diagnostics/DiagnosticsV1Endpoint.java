package com.sequenceiq.freeipa.api.v1.diagnostics;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.diagnostics.docs.DiagnosticsOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/diagnostics")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/diagnostics", description = "Diagnostics in FreeIPA")
public interface DiagnosticsV1Endpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.COLLECT_FREEIPA_DIAGNOSTICS,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "collectFreeIpaDiagnosticsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier collectDiagnostics(@Valid DiagnosticsCollectionRequest request);

    @GET
    @Path("logs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.GET_VM_LOG_PATHS,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "getFreeIpaVmLogsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    VmLogsResponse getVmLogs();

    @GET
    @Path("{environmentCrn}/collections")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.LIST_DIAGNOSTICS_COLLECTIONS,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "listDiagnosticsCollectionsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ListDiagnosticsCollectionResponse listDiagnosticsCollections(@PathParam("environmentCrn") String environmentCrn);

    @POST
    @Path("{environmentCrn}/collections/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DiagnosticsOperationDescriptions.CANCEL_DIAGNOSTICS_COLLECTIONS,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "cancelDiagnosticsCollectionsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void cancelCollections(@PathParam("environmentCrn") String environmentCrn);
}
