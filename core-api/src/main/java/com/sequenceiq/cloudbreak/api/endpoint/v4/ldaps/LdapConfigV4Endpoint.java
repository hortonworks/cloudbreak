package com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapTestV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapTestV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.LdapConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Set;

@Path("/v4/{workspaceId}/ldaps")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/ldaps", description = ControllerDescription.LDAP_V4_CONFIG_DESCRIPTION, protocols = "http,https")
public interface LdapConfigV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "listLdapsByWorkspace")
    LdapV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") Boolean attachGlobal);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "getLdapConfigInWorkspace")
    LdapV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String ldapConfigName);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "createLdapConfigsInWorkspace")
    LdapV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid LdapV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "deleteLdapConfigInWorkspace")
    LdapV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String ldapConfigName);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "deleteLdapConfigsInWorkspace")
    LdapV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> ldapConfigNames);

    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.POST_CONNECTION_TEST, produces = ContentType.JSON,
            nickname = "postLdapConnectionTestInWorkspace")
    LdapTestV4Response test(@PathParam("workspaceId") Long workspaceId, @Valid LdapTestV4Request ldapValidationRequest);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_REQUEST, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "getLdapRequestByNameAndWorkspaceId")
    LdapV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "attachLdapResourceToEnvironments")
    LdapV4Response attach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
        @Valid @NotNull EnvironmentNames environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "detachLdapResourceFromEnvironments")
    LdapV4Response detach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
        @Valid @NotNull EnvironmentNames environmentNames);
}
