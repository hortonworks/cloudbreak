package com.sequenceiq.cloudbreak.api.endpoint;

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

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.LdapConfigResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/ldap")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/ldap", description = ControllerDescription.LDAP_CONFIG_DESCRIPTION)
public interface LdapConfigEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    IdJson postPrivate(@Valid LdapConfigRequest ldapConfigRequest);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    IdJson postPublic(@Valid LdapConfigRequest ldapConfigRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    Set<LdapConfigResponse> getPrivates();

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    Set<LdapConfigResponse> getPublics();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    LdapConfigResponse getPrivate(@PathParam(value = "name") String name);

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    LdapConfigResponse getPublic(@PathParam(value = "name") String name);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    LdapConfigResponse get(@PathParam(value = "id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    void delete(@PathParam(value = "id") Long id);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    void deletePublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.LdapConfigOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.LDAP_CONFIG_NOTES)
    void deletePrivate(@PathParam(value = "name") String name);
}
