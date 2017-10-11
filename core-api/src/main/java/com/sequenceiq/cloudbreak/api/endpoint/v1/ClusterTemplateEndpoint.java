package com.sequenceiq.cloudbreak.api.endpoint.v1;

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
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterTemplateOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/clustertemplates")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/clustertemplates", description = ControllerDescription.CLUSTER_TEMPLATE_DESCRIPTION, protocols = "http,https")
public interface ClusterTemplateEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES,
            nickname = "getClusterTemplate")
    ClusterTemplateResponse get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES,
            nickname = "deleteClusterTemplate")
    void delete(@PathParam("id") Long id);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES,
            nickname = "postPrivateClusterTemplate")
    ClusterTemplateResponse postPrivate(ClusterTemplateRequest clusterTemplateRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES,
            nickname = "getPrivatesClusterTemplate")
    Set<ClusterTemplateResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON,
            notes = Notes.CLUSTER_TEMPLATE_NOTES, nickname = "getPrivateClusterTemplate")
    ClusterTemplateResponse getPrivate(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON,
            notes = Notes.CLUSTER_TEMPLATE_NOTES, nickname = "deletePrivateClusterTemplate")
    void deletePrivate(@PathParam("name") String name);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES,
            nickname = "postPublicClusterTemplate")
    ClusterTemplateResponse postPublic(ClusterTemplateRequest clusterTemplateRequest);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.CLUSTER_TEMPLATE_NOTES,
            nickname = "getPublicsClusterTemplate")
    Set<ClusterTemplateResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON,
            notes = Notes.CLUSTER_TEMPLATE_NOTES, nickname = "getPublicClusterTemplate")
    ClusterTemplateResponse getPublic(@PathParam("name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON,
            notes = Notes.CLUSTER_TEMPLATE_NOTES, nickname = "deletePublicClusterTemplate")
    void deletePublic(@PathParam("name") String name);

}
