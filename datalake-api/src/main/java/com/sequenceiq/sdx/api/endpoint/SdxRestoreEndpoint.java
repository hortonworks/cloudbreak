package com.sequenceiq.sdx.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreStatusResponse;
import com.sequenceiq.sdx.api.model.SdxRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxRestoreStatusResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxRestoreEndpoint {

    @POST
    @Path("{name}/restoreDatalake")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore the datalake ", operationId = "restoreDatalake",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    @SuppressWarnings("ParameterNumber")
    SdxRestoreResponse restoreDatalakeByName(@PathParam("name") String name,
            @QueryParam("backupId") String backupId,
            @QueryParam("backupLocationOverride") String backupLocationOverride,
            @QueryParam("skipValidation") boolean skipValidation,
            @QueryParam("skipAtlasMetadata") boolean skipAtlasMetadata,
            @QueryParam("skipRangerAudits") boolean skipRangerAudits,
            @QueryParam("skipRangerMetadata") boolean skipRangerMetadata,
            @QueryParam("fullDrMaxDurationInMin") int fullDrMaxDurationInMin);

    @POST
    @Path("{name}/restoreDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore status of the datalake ", operationId = "restoreDatalakeStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxRestoreStatusResponse getRestoreDatalakeStatusByName(@PathParam("name") String name, @QueryParam("restoreId") String restoreId);

    @GET
    @Path("{name}/getRestoreDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore status of the datalake by datalake name ", operationId = "getRestoreDatalakeStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxRestoreStatusResponse getRestoreDatalakeStatus(@Parameter(description = "required: datalake name", required = true) @PathParam("name") String name,
            @Parameter(description = "optional: datalake restore id") @QueryParam("restoreId") String restoreId,
            @Parameter(description = "optional: datalake backup name") @QueryParam("backupName") String backupName);

    @GET
    @Path("{name}/getDatalakeRestoreId")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore Id of the datalake restore by datalake name ", operationId = "getDatalakeRestoreId",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    String getDatalakeRestoreId(@Parameter(description = "required: datalake name", required = true) @PathParam("name") String name,
            @Parameter(description = "optional: datalake backup name") @QueryParam("backupName") String backupName);

    @POST
    @Path("{name}/restoreDatabase")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore the database backing datalake ", operationId = "restoreDatabase",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxDatabaseRestoreResponse restoreDatabaseByName(@PathParam("name") String name,
            @QueryParam("backupId") String backupId,
            @QueryParam("restoreId") String restoreId,
            @QueryParam("backupLocation") String backupLocation,
            @QueryParam("databaseMaxDurationInMin") int databaseMaxDurationInMin);

    @GET
    @Path("{name}/restoreDatabaseStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the status of datalake database restore operation", operationId = "restoreDatabaseStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxDatabaseRestoreStatusResponse getRestoreDatabaseStatusByName(@PathParam("name") String name, @QueryParam("operationId") String operationId);
}
