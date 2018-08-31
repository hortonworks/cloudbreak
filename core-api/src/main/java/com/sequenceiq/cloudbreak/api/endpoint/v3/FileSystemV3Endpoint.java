package com.sequenceiq.cloudbreak.api.endpoint.v3;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueriesResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParametersQueryRequest;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/filesystems")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/filesystems", description = ControllerDescription.UTIL_DESCRIPTION, protocols = "http,https")
public interface FileSystemV3Endpoint {

    @POST
    @Path("parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.FILE_SYSTEM_PARAMETERS, produces = ContentType.JSON, nickname = "getFileSystemParametersV3")
    StructuredParameterQueriesResponse getFileSystemParameters(@PathParam("organizationId") Long organizationId,
            StructuredParametersQueryRequest structuredParametersQueryRequest);
}
