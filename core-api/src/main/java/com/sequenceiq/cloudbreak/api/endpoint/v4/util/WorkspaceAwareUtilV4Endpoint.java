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
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/{workspaceId}")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/{workspaceId}", description = ControllerDescription.WORKSPACE_UTIL_V4_DESCRIPTION)
public interface WorkspaceAwareUtilV4Endpoint {

    @GET
    @Path("knox_services")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UtilityOpDescription.KNOX_SERVICES, operationId = "getKnoxServices",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ExposedServiceV4Responses getKnoxServices(@PathParam("workspaceId") Long workspaceId, @QueryParam("blueprintName") String blueprintName);
}
