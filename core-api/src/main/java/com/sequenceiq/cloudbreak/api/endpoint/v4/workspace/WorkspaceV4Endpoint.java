package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.WorkspaceOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryingRestClient
@Path("/v4/workspaces")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/workspaces", description = ControllerDescription.WORKSPACE_V4_DESCRIPTION, protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface WorkspaceV4Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.GET, produces = MediaType.APPLICATION_JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "getWorkspaces")
    WorkspaceV4Responses list();

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "getWorkspaceByName")
    WorkspaceV4Response get(@PathParam("name") String name);

}
