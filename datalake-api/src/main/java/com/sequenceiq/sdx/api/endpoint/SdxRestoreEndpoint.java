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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxRestoreEndpoint {

    @POST
    @Path("{name}/restoreDatalake")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "restore the datalake ", produces = MediaType.APPLICATION_JSON, nickname = "restoreDatalake")
    SdxRestoreResponse restoreDatalakeByName(@PathParam("name") String name,
            @QueryParam("backupId") String backupId, @QueryParam("backupLocationOverride") String backupLocationOverride);

    @POST
    @Path("{name}/restoreDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "restore status of the datalake ", produces = MediaType.APPLICATION_JSON, nickname = "restoreDatalakeStatus")
    SdxRestoreStatusResponse getRestoreDatalakeStatusByName(@PathParam("name") String name,
            @QueryParam("restoreId") String restoreId);

    @GET
    @Path("{name}/getRestoreDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "restore status of the datalake by datalake name ", produces = MediaType.APPLICATION_JSON,
            nickname = "getRestoreDatalakeStatus")
    SdxRestoreStatusResponse getRestoreDatalakeStatus(@ApiParam(value = "required: datalake name", required = true) @PathParam("name") String name,
            @ApiParam(value = "optional: datalake restore id", required = false) @QueryParam("restoreId") String restoreId,
            @ApiParam(value = "optional: datalake backup name", required = false) @QueryParam("backupName") String backupName);

    @GET
    @Path("{name}/getDatalakeRestoreId")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "restore Id of the datalake restore by datalake name ", produces = MediaType.APPLICATION_JSON, nickname = "getDatalakeRestoreId")
    String getDatalakeRestoreId(@ApiParam(value = "required: datalake name", required = true) @PathParam("name") String name,
            @ApiParam(value = "optional: datalake backup name", required = false) @QueryParam("backupName") String backupName);

    @POST
    @Path("{name}/restoreDatabase")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "restore the database backing datalake ", produces = MediaType.APPLICATION_JSON, nickname = "restoreDatabase")
    SdxDatabaseRestoreResponse restoreDatabaseByName(@PathParam("name") String name,
            @QueryParam("backupId") String backupId, @QueryParam("restoreId") String restoreId,
            @QueryParam("backupLocation") String backupLocation);

    @GET
    @Path("{name}/restoreDatabaseStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the status of datalake database restore operation", produces = MediaType.APPLICATION_JSON, nickname = "restoreDatabaseStatus")
    SdxDatabaseRestoreStatusResponse getRestoreDatabaseStatusByName(@PathParam("name") String name,
            @QueryParam("operationId") String operationId);
}
