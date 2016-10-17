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
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/securitygroups")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/securitygroups", description = ControllerDescription.SECURITY_GROUPS_DESCRIPTION)
public interface SecurityGroupEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    IdJson postPrivate(@Valid SecurityGroupJson securityGroupJson);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    IdJson postPublic(@Valid SecurityGroupJson securityGroupJson);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    Set<SecurityGroupJson> getPrivates();

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    Set<SecurityGroupJson> getPublics();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    SecurityGroupJson getPrivate(@PathParam(value = "name") String name);

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    SecurityGroupJson getPublic(@PathParam(value = "name") String name);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    SecurityGroupJson get(@PathParam(value = "id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    void delete(@PathParam(value = "id") Long id);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.DELETE_PUBLIC_BY_NAME,
            produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    void deletePublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.SecurityGroupOpDescription.DELETE_PRIVATE_BY_NAME,
            produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES)
    void deletePrivate(@PathParam(value = "name") String name);
}
