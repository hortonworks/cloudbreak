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

import com.sequenceiq.cloudbreak.api.model.SecurityGroupRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.SecurityGroupOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/securitygroups")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/securitygroups", description = ControllerDescription.SECURITY_GROUPS_DESCRIPTION, protocols = "http,https")
public interface SecurityGroupEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES,
            nickname = "postPrivateSecurityGroup")
    SecurityGroupResponse postPrivate(@Valid SecurityGroupRequest securityGroupRequest);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES,
            nickname = "postPublicSecurityGroup")
    SecurityGroupResponse postPublic(@Valid SecurityGroupRequest securityGroupRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES,
            nickname = "getPrivatesSecurityGroup")
    Set<SecurityGroupResponse> getPrivates();

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES,
            nickname = "getPublicsSecurityGroup")
    Set<SecurityGroupResponse> getPublics();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES,
            nickname = "getPrivateSecurityGroup")
    SecurityGroupResponse getPrivate(@PathParam("name") String name);

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES,
            nickname = "getPublicSecurityGroup")
    SecurityGroupResponse getPublic(@PathParam("name") String name);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES,
            nickname = "getSecurityGroup")
    SecurityGroupResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES,
            nickname = "deleteSecurityGroup")
    void delete(@PathParam("id") Long id);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.DELETE_PUBLIC_BY_NAME,
            produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES, nickname = "deletePublicSecurityGroup")
    void deletePublic(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SecurityGroupOpDescription.DELETE_PRIVATE_BY_NAME,
            produces = ContentType.JSON, notes = Notes.SECURITY_GROUP_NOTES, nickname = "deletePrivateSecurityGroup")
    void deletePrivate(@PathParam("name") String name);
}
