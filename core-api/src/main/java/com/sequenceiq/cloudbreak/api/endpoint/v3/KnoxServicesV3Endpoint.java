package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/knoxservices")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/knoxservices", description = ControllerDescription.KNOX_SERVICES_V3_DESCRIPTION, protocols = "http,https")
public interface KnoxServicesV3Endpoint {

    @GET
    @Path("{blueprintName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.KnoxServicesOpDescription.LIST_IN_WORKSPACE_FOR_BLUEPRINT,
            produces = ContentType.JSON,
            nickname = "listByWorkspaceAndBlueprint")
    Collection<ExposedServiceV4Response> listByWorkspaceAndBlueprint(@PathParam("workspaceId") Long workspaceId,
                                                                     @PathParam("blueprintName") String blueprintName);
}
