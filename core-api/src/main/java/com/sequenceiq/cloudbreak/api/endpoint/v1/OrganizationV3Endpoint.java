package com.sequenceiq.cloudbreak.api.endpoint.v1;

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

import com.sequenceiq.cloudbreak.api.model.users.ChangeOrganizationUsersJson;
import com.sequenceiq.cloudbreak.api.model.users.OrganizationRequest;
import com.sequenceiq.cloudbreak.api.model.users.OrganizationResponse;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.OrganizationOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/organizations")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/organizations", description = ControllerDescription.ORGANIZATION_DESCRIPTION, protocols = "http,https")
public interface OrganizationV3Endpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.POST, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "createOrganization")
    OrganizationResponse create(@Valid OrganizationRequest organizationRequest);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.GET, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "getOrganizations")
    SortedSet<OrganizationResponse> getAll();

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.GET_BY_NAME, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "getOrganizationByName")
    OrganizationResponse getByName(@PathParam("name") String name);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.DELETE_BY_NAME, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "deleteOrganizationByName")
    OrganizationResponse deleteByName(@PathParam("name") String name);

    @PUT
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.CHANGE_USERS, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "changeOrganizationUsers")
    SortedSet<UserResponseJson> changeUsers(@PathParam("name") String orgName, @Valid Set<ChangeOrganizationUsersJson> changeOrganizationUsersJson);

    @PUT
    @Path("name/{name}/removeUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.REMOVE_USERS, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "removeOrganizationUsers")
    SortedSet<UserResponseJson> removeUsers(@PathParam("name") String orgName, @Valid Set<String> userIds);

    @PUT
    @Path("name/{name}/addUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.ADD_USERS, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "addOrganizationUsers")
    SortedSet<UserResponseJson> addUsers(@PathParam("name") String orgName, @Valid Set<ChangeOrganizationUsersJson> addOrganizationUsersJson);

    @PUT
    @Path("name/{name}/updateUsers")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.UPDATE_USERS, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "updateOrganizationUsers")
    SortedSet<UserResponseJson> updateUsers(@PathParam("name") String orgName, @Valid Set<ChangeOrganizationUsersJson> updateOrganizationUsersJson);

}
