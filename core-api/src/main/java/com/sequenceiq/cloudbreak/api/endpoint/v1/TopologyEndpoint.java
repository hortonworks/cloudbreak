package com.sequenceiq.cloudbreak.api.endpoint.v1;


import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.TopologyRequest;
import com.sequenceiq.cloudbreak.api.model.TopologyResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.TopologyOpDesctiption;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/topologies")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/topologies", description = ControllerDescription.TOPOLOGY_DESCRIPTION, protocols = "http,https")
public interface TopologyEndpoint {

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TopologyOpDesctiption.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.TOPOLOGY_NOTES, nickname = "getPublicsTopology")
    Set<TopologyResponse> getPublics();

    @GET
    @Path("account/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TopologyOpDesctiption.GET_BY_ID, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES, nickname = "getTopology")
    TopologyResponse get(@PathParam("id") Long id);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TopologyOpDesctiption.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.TOPOLOGY_NOTES, nickname = "postPublicTopology")
    TopologyResponse postPublic(@Valid TopologyRequest topologyRequest);

    @DELETE
    @Path("account/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TopologyOpDesctiption.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.TOPOLOGY_NOTES, nickname = "deleteTopology")
    void delete(@PathParam("id") Long id, @QueryParam("forced") @DefaultValue("false") Boolean forced);
}
