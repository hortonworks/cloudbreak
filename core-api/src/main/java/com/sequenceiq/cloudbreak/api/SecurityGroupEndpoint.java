package com.sequenceiq.cloudbreak.api;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.model.IdJson;
import com.sequenceiq.cloudbreak.model.SecurityGroupJson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/securitygroups", description = ControllerDescription.SECURITY_GROUPS_DESCRIPTION, position = 9)
public interface SecurityGroupEndpoint {

    @POST
    @Path("user/securitygroups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    IdJson postPrivate(SecurityGroupJson securityGroupJson);

    @POST
    @Path("account/securitygroups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    IdJson postPublic(SecurityGroupJson securityGroupJson);

    @GET
    @Path("user/securitygroups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    Set<SecurityGroupJson> getPrivates();

    @GET
    @Path("account/securitygroups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    Set<SecurityGroupJson> getPublics();

    @GET
    @Path("user/securitygroups/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    SecurityGroupJson getPrivate(@PathParam(value = "name") String name);

    @GET
    @Path("account/securitygroups/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    SecurityGroupJson getPublic(@PathParam(value = "name") String name);

    @GET
    @Path("securitygroups/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    SecurityGroupJson get(@PathParam(value = "id") Long id);

    @DELETE
    @Path("securitygroups/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    void delete(@PathParam(value = "id") Long id);

    @DELETE
    @Path("account/securitygroups/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.DELETE_PUBLIC_BY_NAME,
            produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    void deletePublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/securitygroups/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.DELETE_PRIVATE_BY_NAME,
            produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    void deletePrivate(@PathParam(value = "name") String name);
}
