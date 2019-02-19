package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.FileSystemOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/file_systems")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/file_systems", description = ControllerDescription.FILESYSTEMS_V4_DESCRIPTION, protocols = "http,https")
public interface FileSystemV4Endpoint {

    @GET
    @Path("parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FileSystemOpDescription.FILE_SYSTEM_PARAMETERS, produces = ContentType.JSON, nickname = "getFileSystemParameters")
    FileSystemParameterV4Responses getFileSystemParameters(@PathParam("workspaceId") Long workspaceId,
            @NotNull @QueryParam("clusterDefinitionName") String clusterDefinitionName, @NotNull @QueryParam("clusterName") String clusterName,
            @QueryParam("accountName") String accountName, @NotNull @QueryParam("storageName") String storageName,
            @NotNull @QueryParam("fileSystemType") String fileSystemType, @QueryParam("attachedCluster") @DefaultValue("false") Boolean attachedCluster);
}
