package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;
import static com.sequenceiq.cloudbreak.doc.Notes.BLUEPRINT_NOTES;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/blueprints", description = ControllerDescription.BLUEPRINT_V4_DESCRIPTION, protocols = "http,https")
public interface BlueprintV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.BlueprintOpDescription.LIST_BY_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "listBlueprintsByWorkspace")
    Set<BlueprintViewResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId);

}
