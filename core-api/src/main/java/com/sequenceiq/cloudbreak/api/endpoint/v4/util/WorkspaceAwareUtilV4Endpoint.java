package com.sequenceiq.cloudbreak.api.endpoint.v4.util;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryingRestClient
@Path("/v4/{workspaceId}")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}", description = ControllerDescription.WORKSPACE_UTIL_V4_DESCRIPTION, protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface WorkspaceAwareUtilV4Endpoint {

    @GET
    @Path("knox_services")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.KNOX_SERVICES, produces = MediaType.APPLICATION_JSON, nickname = "getKnoxServices")
    ExposedServiceV4Responses getKnoxServices(@PathParam("workspaceId") Long workspaceId, @QueryParam("blueprintName") String blueprintName);
}
