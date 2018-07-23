package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.Set;

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

import com.sequenceiq.cloudbreak.api.model.ChangeOrganizationUsersJson;
import com.sequenceiq.cloudbreak.api.model.OrganizationRequest;
import com.sequenceiq.cloudbreak.api.model.OrganizationResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.OrganizationOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/organizations")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/organizations", description = ControllerDescription.ORGANIZATION_DESCRIPTION, protocols = "http,https")
public interface OrganizationEndpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.POST, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "postOrganization")
    OrganizationResponse post(@Valid OrganizationRequest organizationRequest);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.GET, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "getOrganizations")
    Set<OrganizationResponse> getAll();

    @GET
    @Path("t/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.GET_BY_NAME, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "getOrganizationByName")
    OrganizationResponse getByName(@PathParam("name") String name);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "getOrganization")
    OrganizationResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "deleteOrganization")
    void delete(@PathParam("id") Long id);

    @DELETE
    @Path("t/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.DELETE_BY_NAME, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "deletePrivateOrganization")
    void deleteByName(@PathParam("name") String name);

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OrganizationOpDescription.CHANGE_USERS, produces = ContentType.JSON, notes = Notes.ORGANIZATION_NOTES,
            nickname = "changeOrganizationUsers")
    void changeUsers(@PathParam("id") Long id, @Valid Set<ChangeOrganizationUsersJson> changeOrganizationUsersJson);

}
