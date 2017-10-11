package com.sequenceiq.cloudbreak.api.endpoint.v2;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.UpdateClusterJsonV2;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v2/stacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/cluster", description = ControllerDescription.CLUSTER_DESCRIPTION, protocols = "http,https")
public interface ClusterV2Endpoint {

    @PUT
    @Path("{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterOpDescription.PUT_BY_STACK_NAME, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "putClusterV2")
    Response put(@PathParam("name") String name, @Valid UpdateClusterJsonV2 updateJson) throws Exception;
}
