package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.ldap.LDAPTestRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapTestResult;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.LdapConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/ldapconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/ldapconfigs", description = ControllerDescription.LDAP_V3_CONFIG_DESCRIPTION, protocols = "http,https")
public interface LdapConfigV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "listLdapsByWorkspace")
    Set<LdapConfigResponse> listConfigsByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "getLdapConfigInWorkspace")
    LdapConfigResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String ldapConfigName);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "createLdapConfigsInWorkspace")
    LdapConfigResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid LdapConfigRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "deleteLdapConfigsInWorkspace")
    LdapConfigResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String ldapConfigName);

    @POST
    @Path("testconnect")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.POST_CONNECTION_TEST, produces = ContentType.JSON, nickname = "postLdapConnectionTestInWorkspace")
    LdapTestResult testLdapConnection(@PathParam("workspaceId") Long workspaceId, @Valid LDAPTestRequest ldapValidationRequest);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_REQUEST, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "getLdapRequestByNameAndWorkspaceId")
    LdapConfigRequest getRequestFromName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "attachLdapResourceToEnvironments")
    LdapConfigResponse attachToEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @NotEmpty Set<String> environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "detachLdapResourceFromEnvironments")
    LdapConfigResponse detachFromEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotEmpty Set<String> environmentNames);
}
