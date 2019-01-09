package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.FileSystemParametersV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParametersV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/file_systems")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/file_systems", description = ControllerDescription.FILESYSTEMS_V4_DESCRIPTION, protocols = "http,https")
public interface FileSystemV4Endpoint {

    @GET
    @Path("parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.FileSystemOpDescription.FILE_SYSTEM_PARAMETERS, produces = ContentType.JSON, nickname = "getFileSystemParametersV4")
    FileSystemParametersV4Response getFileSystemParameters(@PathParam("workspaceId") Long workspaceId,
            @BeanParam FileSystemParametersV4Filter fileSystemParametersV4Filter);
}
