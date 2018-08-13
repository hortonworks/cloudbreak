package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/stack", description = ControllerDescription.STACK_V3_DESCRIPTION, protocols = "http,https")
public interface StackV3Endpoint {

    @GET
    @Path("{organizationId}/stack")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "listStacksByOrganization")
    Set<StackResponse> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("{organizationId}/stack/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getStackInOrganization")
    StackResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("{organizationId}/stack")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "createStackInOrganization")
    StackResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid StackV2Request request);

    @DELETE
    @Path("{organizationId}/stack/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "deleteStackInOrganization")
    StackResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

}
