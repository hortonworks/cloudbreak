package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace;

import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.ChangeWorkspaceUsersV4Requests;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.UserIds;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.WorkspaceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.WorkspaceOpDescription;

import io.swagger.annotations.ApiOperation;

//@Path("/v4/workspaces")
//@Consumes(MediaType.APPLICATION_JSON)
//@Api(value = "/v4/workspaces", description = ControllerDescription.WORKSPACE_V4_DESCRIPTION, protocols = "http,https")
public interface WorkspaceV4Endpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.POST, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "createWorkspace")
    WorkspaceV4Response post(@Valid WorkspaceV4Request workspaceV4Request);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.GET, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "getWorkspaces")
    WorkspaceV4Responses list();

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.GET_BY_NAME, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "getWorkspaceByName")
    WorkspaceV4Response get(@PathParam("name") String name);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.DELETE_BY_NAME, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "deleteWorkspaceByName")
    WorkspaceV4Response delete(@PathParam("name") String name);

    @PUT
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.CHANGE_USERS, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "changeWorkspaceUsers")
    UserV4Responses changeUsers(@PathParam("name") String workspaceName, @Valid ChangeWorkspaceUsersV4Requests changeWorkspaceUsersV4Requests);

    @PUT
    @Path("{name}/remove_users")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.REMOVE_USERS, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "removeWorkspaceUsers")
    UserV4Responses removeUsers(@PathParam("name") String workspaceName, @Valid UserIds userIds);

    @PUT
    @Path("{name}/add_users")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.ADD_USERS, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "addWorkspaceUsers")
    UserV4Responses addUsers(@PathParam("name") String workspaceName, @Valid ChangeWorkspaceUsersV4Requests addWorkspaceUsers);

    @PUT
    @Path("{name}/update_users")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.UPDATE_USERS, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "updateWorkspaceUsers")
    UserV4Responses updateUsers(@PathParam("name") String workspaceName, @Valid ChangeWorkspaceUsersV4Requests updateWorkspaceUsers);

}
