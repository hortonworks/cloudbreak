package com.sequenceiq.cloudbreak.api.endpoint;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/cluster", description = ControllerDescription.CLUSTER_DESCRIPTION, position = 4)
public interface ClusterEndpoint {

    @POST
    @Path("stacks/{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.POST_FOR_STACK, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    Response post(@PathParam(value = "id") Long id, @Valid ClusterRequest request);

    @GET
    @Path("stacks/{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.GET_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    ClusterResponse get(@PathParam(value = "id") Long id);

    @GET
    @Path("account/stacks/{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    ClusterResponse getPublic(@PathParam(value = "name") String name);

    @GET
    @Path("user/stacks/{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    ClusterResponse getPrivate(@PathParam(value = "name") String name);

    @DELETE
    @Path("stacks/{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.DELETE_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    void delete(@PathParam(value = "id") Long stackId) throws Exception;

    @PUT
    @Path("stacks/{id}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterOpDescription.PUT_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    Response put(@PathParam(value = "id") Long stackId, @Valid UpdateClusterJson updateJson) throws Exception;

}
