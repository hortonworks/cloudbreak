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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxRestoreEndpoint {

    @POST
    @Path("{name}/restoreDatalake")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore the datalake ", operationId ="restoreDatalake")
    SdxRestoreResponse restoreDatalakeByName(@PathParam("name") String name,
            @QueryParam("backupId") String backupId,
            @QueryParam("backupLocationOverride") String backupLocationOverride,
            @QueryParam("skipValidation") boolean skipValidation,
            @QueryParam("skipAtlasMetadata") boolean skipAtlasMetadata,
            @QueryParam("skipRangerAudits") boolean skipRangerAudits,
            @QueryParam("skipRangerMetadata") boolean skipRangerMetadata);

    @POST
    @Path("{name}/restoreDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore status of the datalake ", operationId ="restoreDatalakeStatus")
    SdxRestoreStatusResponse getRestoreDatalakeStatusByName(@PathParam("name") String name, @QueryParam("restoreId") String restoreId);

    @GET
    @Path("{name}/getRestoreDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore status of the datalake by datalake name ", operationId ="getRestoreDatalakeStatus")
    SdxRestoreStatusResponse getRestoreDatalakeStatus(@Parameter(description = "required: datalake name", required = true) @PathParam("name") String name, @Parameter(description = "optional: datalake restore id") @QueryParam("restoreId") String restoreId, @Parameter(description = "optional: datalake backup name") @QueryParam("backupName") String backupName);

    @GET
    @Path("{name}/getDatalakeRestoreId")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore Id of the datalake restore by datalake name ", operationId ="getDatalakeRestoreId")
    String getDatalakeRestoreId(@Parameter(description = "required: datalake name", required = true) @PathParam("name") String name, @Parameter(description = "optional: datalake backup name") @QueryParam("backupName") String backupName);

    @POST
    @Path("{name}/restoreDatabase")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "restore the database backing datalake ", operationId ="restoreDatabase")
    SdxDatabaseRestoreResponse restoreDatabaseByName(@PathParam("name") String name, @QueryParam("backupId") String backupId, @QueryParam("restoreId") String restoreId, @QueryParam("backupLocation") String backupLocation);

    @GET
    @Path("{name}/restoreDatabaseStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the status of datalake database restore operation", operationId ="restoreDatabaseStatus")
    SdxDatabaseRestoreStatusResponse getRestoreDatabaseStatusByName(@PathParam("name") String name, @QueryParam("operationId") String operationId);
}
