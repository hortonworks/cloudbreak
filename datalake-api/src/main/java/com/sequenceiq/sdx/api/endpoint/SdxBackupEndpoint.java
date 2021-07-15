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
import com.sequenceiq.sdx.api.model.SdxBackupResponse;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxBackupEndpoint {

    @POST
    @Path("{name}/backupDatalake")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "backup the datalake ", produces = MediaType.APPLICATION_JSON, nickname = "backupDatalake")
    SdxBackupResponse backupDatalakeByName(@PathParam("name") String name,
            @QueryParam("backupLocation") String backupLocation,
            @QueryParam("backupName") String backupName);

    @POST
    @Path("{name}/backupDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "backup status of the datalake ", produces = MediaType.APPLICATION_JSON, nickname = "backupDatalakeStatus")
    SdxBackupStatusResponse backupDatalakeStatusByName(@PathParam("name") String name,
            @QueryParam("backupId") String backupId,
            @QueryParam("backupName") String backupName);

    @GET
    @Path("{name}/getBackupDatalakeStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "backup status of the datalake by datalake name ", produces = MediaType.APPLICATION_JSON,
            nickname = "getBackupDatalakeStatus")
    SdxBackupStatusResponse getBackupDatalakeStatus(@ApiParam(value = "required: datalake name", required = true) @PathParam("name") String name,
            @ApiParam(value = "optional: datalake backup id", required = false) @QueryParam("backupId") String backupId,
            @ApiParam(value = "optional: datalake backup name", required = false) @QueryParam("backupName") String backupName);

    @GET
    @Path("{name}/getDatalakeBackupId")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "backup Id of the datalake backup by datalake name ", produces = MediaType.APPLICATION_JSON, nickname = "getDatalakeBackupId")
    String getDatalakeBackupId(@ApiParam(value = "required: datalake name", required = true) @PathParam("name") String name,
            @ApiParam(value = "optional: datalake backup name", required = false) @QueryParam("backupName") String backupName);

    @POST
    @Path("{name}/backupDatabase")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "backup the database backing datalake ", produces = MediaType.APPLICATION_JSON, nickname = "backupDatabase")
    SdxDatabaseBackupResponse backupDatabaseByName(@PathParam("name") String name,
            @QueryParam("backupId") String backupId, @QueryParam("backupLocation") String backupLocation);

    @GET
    @Path("{name}/backupDatabaseStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the status of datalake database backup operation", produces = MediaType.APPLICATION_JSON, nickname = "backupDatabaseStatus")
    SdxDatabaseBackupStatusResponse getBackupDatabaseStatusByName(@PathParam("name") String name,
            @QueryParam("operationId") String operationId);
}
