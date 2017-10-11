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

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.TemplateOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/templates")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/templates", description = ControllerDescription.TEMPLATE_DESCRIPTION, protocols = "http,https")
public interface TemplateEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "postPrivateTemplate")
    TemplateResponse postPrivate(@Valid TemplateRequest templateRequest);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "postPublicTemplate")
    TemplateResponse postPublic(@Valid TemplateRequest templateRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "getPrivatesTemplate")
    Set<TemplateResponse> getPrivates();

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "getPublicsTemplate")
    Set<TemplateResponse> getPublics();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "getPrivateTemplate")
    TemplateResponse getPrivate(@PathParam("name") String name);

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "getPublicTemplate")
    TemplateResponse getPublic(@PathParam("name") String name);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "getTemplate")
    TemplateResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "deleteTemplate")
    void delete(@PathParam("id") Long id);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "deletePublicTemplate")
    void deletePublic(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TemplateOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES,
            nickname = "deletePrivateTemplate")
    void deletePrivate(@PathParam("name") String name);
}
