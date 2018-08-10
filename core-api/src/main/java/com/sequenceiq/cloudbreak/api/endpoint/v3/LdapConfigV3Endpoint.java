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

import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.LdapConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/ldapconfigs", description = ControllerDescription.LDAP_V3_CONFIG_DESCRIPTION, protocols = "http,https")
public interface LdapConfigV3Endpoint {

    @GET
    @Path("{organizationId}/ldapconfigs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "listLdapsByOrganization")
    Set<LdapConfigResponse> listConfigsByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("{organizationId}/ldapconfigs/{ldapConfigName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "getLdapConfigInOrganization")
    LdapConfigResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("ldapConfigName") String ldapConfigName);

    @POST
    @Path("{organizationId}/ldapconfigs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "createLdapConfigsInOrganization")
    LdapConfigResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid LdapConfigRequest request);

    @DELETE
    @Path("{organizationId}/ldapconfigs/{ldapConfigName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "deleteLdapConfigsInOrganization")
    LdapConfigResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("ldapConfigName") String ldapConfigName);
}
