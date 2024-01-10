package com.sequenceiq.sdx.api.endpoint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.sdx.api.model.SdxBackupResponse;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxBackupEndpoint {

    @POST
    @Path("{name}/backupDatalake")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup the datalake ", operationId = "backupDatalake",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    @SuppressWarnings("ParameterNumber")
    SdxBackupResponse backupDatalakeByName(@PathParam("name") String name,
            @QueryParam("backupLocation") String backupLocation,
            @QueryParam("backupName") String backupName,
            @QueryParam("skipValidation") boolean skipValidation,
            @QueryParam("skipAtlasMetadata") boolean skipAtlasMetadata,
            @QueryParam("skipRangerAudits") boolean skipRangerAudits,
            @QueryParam("skipRangerMetadata") boolean skipRangerMetadata,
            @QueryParam("fullDrMaxDurationInMin") int fullDrMaxDurationInMin);

    @POST
    @Path("{name}/backupDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup status of the datalake ", operationId = "backupDatalakeStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxBackupStatusResponse backupDatalakeStatusByName(@PathParam("name") String name, @QueryParam("backupId") String backupId,
            @QueryParam("backupName") String backupName);

    @GET
    @Path("{name}/getBackupDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup status of the datalake by datalake name ", operationId = "getBackupDatalakeStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxBackupStatusResponse getBackupDatalakeStatus(@Parameter(description = "required: datalake name", required = true) @PathParam("name") String name,
            @Parameter(description = "optional: datalake backup id", required = false) @QueryParam("backupId") String backupId,
            @Parameter(description = "optional: datalake backup name", required = false) @QueryParam("backupName") String backupName);

    @GET
    @Path("{name}/getDatalakeBackupId")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup Id of the datalake backup by datalake name ", operationId = "getDatalakeBackupId",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    String getDatalakeBackupId(@Parameter(description = "required: datalake name", required = true) @PathParam("name") String name,
            @Parameter(description = "optional: datalake backup name", required = false) @QueryParam("backupName") String backupName);

    @POST
    @Path("{name}/backupDatabase")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup the database backing datalake ", operationId = "backupDatabase",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxDatabaseBackupResponse backupDatabaseByName(@PathParam("name") String name, @QueryParam("backupId") String backupId,
            @QueryParam("backupLocation") String backupLocation);

    @POST
    @Path("{name}/backupDatabaseInternal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup the database with the option of closing or not closing connections to the database ", operationId = "backupDatabaseInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxDatabaseBackupResponse backupDatabaseByNameInternal(@PathParam("name") String name, @Valid @NotNull SdxDatabaseBackupRequest backupRequest);

    @GET
    @Path("{name}/backupDatabaseStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the status of datalake database backup operation", operationId = "backupDatabaseStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SdxDatabaseBackupStatusResponse getBackupDatabaseStatusByName(@PathParam("name") String name, @QueryParam("operationId") String operationId);
}
