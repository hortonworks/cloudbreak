package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint;

import static com.sequenceiq.cloudbreak.doc.Notes.BLUEPRINT_NOTES;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.cloudera.cdp.datahub.model.CreateClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.BlueprintOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryingRestClient
@Path("/v4/{workspaceId}/blueprints")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/blueprints", description = ControllerDescription.BLUEPRINT_V4_DESCRIPTION, protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface BlueprintV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.LIST_BY_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = BLUEPRINT_NOTES,
            nickname = "listBlueprintsByWorkspace")
    BlueprintV4ViewResponses list(@PathParam("workspaceId") Long workspaceId, @DefaultValue("false") @QueryParam("withSdx") Boolean withSdx);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintInWorkspace")
    BlueprintV4Response getByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") @NotNull String name);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_CRN_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintByCrn")
    BlueprintV4Response getByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") @NotNull String crn);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.CREATE_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = BLUEPRINT_NOTES,
            nickname = "createBlueprintInWorkspace")
    BlueprintV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid BlueprintV4Request request);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = BLUEPRINT_NOTES,
            nickname = "deleteBlueprintInWorkspace")
    BlueprintV4Response deleteByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") @NotNull String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_BY_CRN_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = BLUEPRINT_NOTES,
            nickname = "deleteBlueprintByCrn")
    BlueprintV4Response deleteByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") @NotNull String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON,
            notes = BLUEPRINT_NOTES, nickname = "deleteBlueprintsInWorkspace")
    BlueprintV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = BLUEPRINT_NOTES,
            nickname = "getBlueprintRequestFromName")
    BlueprintV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.CUSTOM_PARAMETERS, produces = MediaType.APPLICATION_JSON,
            nickname = "getBlueprintCustomParameters")
    ParametersQueryV4Response getParameters(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = BlueprintOpDescription.CLI_COMMAND, produces = MediaType.APPLICATION_JSON, notes = BLUEPRINT_NOTES,
            nickname = "getCreateClusterTemplateRequestForCli")
    CreateClusterTemplateRequest getCreateClusterTemplateRequestForCli(@PathParam("workspaceId") Long workspaceId,
            @NotNull @Valid BlueprintV4Request blueprintV4Request);
}
