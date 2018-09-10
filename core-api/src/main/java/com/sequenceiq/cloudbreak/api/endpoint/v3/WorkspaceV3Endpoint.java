package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;
import java.util.SortedSet;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.users.ChangeWorkspaceUsersJson;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceRequest;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResponse;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.WorkspaceOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/workspaces")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/workspaces", description = ControllerDescription.WORKSPACE_DESCRIPTION, protocols = "http,https")
public interface WorkspaceV3Endpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.POST, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "createWorkspace")
    WorkspaceResponse create(@Valid WorkspaceRequest workspaceRequest);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.GET, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "getWorkspaces")
    SortedSet<WorkspaceResponse> getAll();

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.GET_BY_NAME, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "getWorkspaceByName")
    WorkspaceResponse getByName(@PathParam("name") String name);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.DELETE_BY_NAME, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "deleteWorkspaceByName")
    WorkspaceResponse deleteByName(@PathParam("name") String name);

    @PUT
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.CHANGE_USERS, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "changeWorkspaceUsers")
    SortedSet<UserResponseJson> changeUsers(@PathParam("name") String orgName, @Valid Set<ChangeWorkspaceUsersJson> changeWorkspaceUsersJson);

    @PUT
    @Path("name/{name}/removeUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.REMOVE_USERS, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "removeWorkspaceUsers")
    SortedSet<UserResponseJson> removeUsers(@PathParam("name") String orgName, @Valid Set<String> userIds);

    @PUT
    @Path("name/{name}/addUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.ADD_USERS, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "addWorkspaceUsers")
    SortedSet<UserResponseJson> addUsers(@PathParam("name") String orgName, @Valid Set<ChangeWorkspaceUsersJson> addWorkspaceUsersJson);

    @PUT
    @Path("name/{name}/updateUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = WorkspaceOpDescription.UPDATE_USERS, produces = ContentType.JSON, notes = Notes.WORKSPACE_NOTES,
            nickname = "updateWorkspaceUsers")
    SortedSet<UserResponseJson> updateUsers(@PathParam("name") String orgName, @Valid Set<ChangeWorkspaceUsersJson> updateWorkspaceUsersJson);

}
