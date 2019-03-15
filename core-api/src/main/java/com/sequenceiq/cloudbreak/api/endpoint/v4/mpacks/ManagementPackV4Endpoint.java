package com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.request.ManagementPackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ManagementPackOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Set;

@Path("/v4/{workspaceId}/mpacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/mpacks", description = ControllerDescription.MANAGEMENT_PACK_V4_DESCRIPTION, protocols = "http,https")
public interface ManagementPackV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "listManagementPacksByWorkspace")
    ManagementPackV4Responses listByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "getManagementPackInWorkspace")
    ManagementPackV4Response getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "createManagementPackInWorkspace")
    ManagementPackV4Response createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid ManagementPackV4Request request);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "deleteManagementPackInWorkspace")
    ManagementPackV4Response deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "deleteManagementPacksInWorkspace")
    ManagementPackV4Responses deleteMultipleInWorkspace(@PathParam("workspaceId") Long workspaceId, Set<String> names);
}
