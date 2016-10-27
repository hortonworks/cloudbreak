package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/clustertemplates")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/clustertemplates", description = ControllerDescription.CLUSTER_TEMPLATE_DESCRIPTION, protocols = "http,https")
public interface ClusterTemplateEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES)
    ClusterTemplateResponse get(@PathParam(value = "id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES)
    void delete(@PathParam(value = "id") Long id);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES)
    ClusterTemplateResponse postPrivate(ClusterTemplateRequest clusterTemplateRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES)
    Set<ClusterTemplateResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON,
            notes = Notes.CLUSTER_TEMPLATE_NOTES)
    ClusterTemplateResponse getPrivate(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON,
            notes = Notes.CLUSTER_TEMPLATE_NOTES)
    void deletePrivate(@PathParam(value = "name") String name);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES)
    ClusterTemplateResponse postPublic(ClusterTemplateRequest clusterTemplateRequest);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES)
    Set<ClusterTemplateResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON,
            notes = Notes.CLUSTER_TEMPLATE_NOTES)
    ClusterTemplateResponse getPublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON,
            notes = Notes.CLUSTER_TEMPLATE_NOTES)
    void deletePublic(@PathParam(value = "name") String name);

}
