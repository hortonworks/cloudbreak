package com.sequenceiq.cloudbreak.api.endpoint


import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.TopologyOpDesctiption
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.TopologyRequest
import com.sequenceiq.cloudbreak.api.model.TopologyResponse

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/topologies")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/topologies", description = ControllerDescription.TOPOLOGY_DESCRIPTION, position = 9)
interface TopologyEndpoint {

    val publics: Set<TopologyResponse>

    @GET
    @Path(value = "account/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TopologyOpDesctiption.GET_BY_ID, produces = ContentType.JSON, notes = Notes.TEMPLATE_NOTES)
    operator fun get(@PathParam(value = "id") id: Long?): TopologyResponse

    @POST
    @Path(value = "account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TopologyOpDesctiption.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.TOPOLOGY_NOTES)
    fun postPublic(@Valid topologyRequest: TopologyRequest): IdJson

    @DELETE
    @Path(value = "account/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TopologyOpDesctiption.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.TOPOLOGY_NOTES)
    fun delete(@PathParam(value = "id") id: Long?, @QueryParam("forced") @DefaultValue("false") forced: Boolean?)
}
