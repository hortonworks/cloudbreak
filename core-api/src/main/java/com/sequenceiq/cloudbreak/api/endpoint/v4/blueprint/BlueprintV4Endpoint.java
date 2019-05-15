package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;
import static com.sequenceiq.cloudbreak.doc.Notes.BLUEPRINT_NOTES;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.BlueprintOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ConnectorOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/blueprints", description = ControllerDescription.BLUEPRINT_V4_DESCRIPTION, protocols = "http,https")
public interface BlueprintV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.LIST_BY_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "listBlueprintsByWorkspace")
    BlueprintV4ViewResponses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintInWorkspace")
    BlueprintV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.CREATE_IN_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "createBlueprintInWorkspace")
    BlueprintV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid BlueprintV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "deleteBlueprintInWorkspace")
    BlueprintV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = JSON,
            notes = BLUEPRINT_NOTES, nickname = "deleteBlueprintsInWorkspace")
    BlueprintV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_NAME, produces = JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintRequestFromName")
    BlueprintV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CUSTOM_PARAMETERS, produces = JSON,
            nickname = "getBlueprintCustomParameters")
    ParametersQueryV4Response getParameters(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "createRecommendationForWorkspace")
    RecommendationV4Response createRecommendation(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("blueprintName") String blueprintName, @QueryParam("credentialName") String credentialName,
            @QueryParam("region") String region, @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

}
