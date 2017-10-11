package com.sequenceiq.cloudbreak.api.endpoint.v1;

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

import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.LdapConfigResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.LdapConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/ldap")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/ldap", description = ControllerDescription.LDAP_CONFIG_DESCRIPTION, protocols = "http,https")
public interface LdapConfigEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
        nickname = "postPrivateLdap")
    LdapConfigResponse postPrivate(@Valid LdapConfigRequest ldapConfigRequest);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "postPublicLdap")
    LdapConfigResponse postPublic(@Valid LdapConfigRequest ldapConfigRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "getPrivatesLdap")
    Set<LdapConfigResponse> getPrivates();

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
        nickname = "getPublicsLdap")
    Set<LdapConfigResponse> getPublics();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "getPrivateLdap")
    LdapConfigResponse getPrivate(@PathParam("name") String name);

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "getPublicLdap")
    LdapConfigResponse getPublic(@PathParam("name") String name);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "getLdap")
    LdapConfigResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "deleteLdap")
    void delete(@PathParam("id") Long id);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "deletePublicLdap")
    void deletePublic(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES,
            nickname = "deletePrivateLdap")
    void deletePrivate(@PathParam("name") String name);
}
