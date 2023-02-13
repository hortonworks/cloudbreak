package com.sequenceiq.sdx.api.endpoint;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import com.sequenceiq.sdx.api.model.SdxBackupResponse;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxBackupEndpoint {

    @POST
    @Path("{name}/backupDatalake")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup the datalake ", operationId ="backupDatalake")
    SdxBackupResponse backupDatalakeByName(@PathParam("name") String name,
            @QueryParam("backupLocation") String backupLocation,
            @QueryParam("backupName") String backupName,
            @QueryParam("skipValidation") boolean skipValidation,
            @QueryParam("skipAtlasMetadata") boolean skipAtlasMetadata,
            @QueryParam("skipRangerAudits") boolean skipRangerAudits,
            @QueryParam("skipRangerMetadata") boolean skipRangerMetadata);

    @POST
    @Path("{name}/backupDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup status of the datalake ", operationId ="backupDatalakeStatus")
    SdxBackupStatusResponse backupDatalakeStatusByName(@PathParam("name") String name, @QueryParam("backupId") String backupId, @QueryParam("backupName") String backupName);

    @GET
    @Path("{name}/getBackupDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup status of the datalake by datalake name ", operationId ="getBackupDatalakeStatus")
    SdxBackupStatusResponse getBackupDatalakeStatus(@Parameter(description = "required: datalake name", required = true) @PathParam("name") String name, @Parameter(description = "optional: datalake backup id", required = false) @QueryParam("backupId") String backupId, @Parameter(description = "optional: datalake backup name", required = false) @QueryParam("backupName") String backupName);

    @GET
    @Path("{name}/getDatalakeBackupId")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup Id of the datalake backup by datalake name ", operationId ="getDatalakeBackupId")
    String getDatalakeBackupId(@Parameter(description = "required: datalake name", required = true) @PathParam("name") String name, @Parameter(description = "optional: datalake backup name", required = false) @QueryParam("backupName") String backupName);

    @POST
    @Path("{name}/backupDatabase")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup the database backing datalake ", operationId ="backupDatabase")
    SdxDatabaseBackupResponse backupDatabaseByName(@PathParam("name") String name, @QueryParam("backupId") String backupId, @QueryParam("backupLocation") String backupLocation);

    @POST
    @Path("{name}/backupDatabaseInternal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "backup the database with the option of closing or not closing connections to the database ", operationId ="backupDatabaseInternal")
    SdxDatabaseBackupResponse backupDatabaseByNameInternal(@PathParam("name") String name, @Valid @NotNull SdxDatabaseBackupRequest backupRequest);

    @GET
    @Path("{name}/backupDatabaseStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the status of datalake database backup operation", operationId ="backupDatabaseStatus")
    SdxDatabaseBackupStatusResponse getBackupDatabaseStatusByName(@PathParam("name") String name, @QueryParam("operationId") String operationId);
}
