package com.sequenceiq.periscope.api.endpoint

import com.sequenceiq.periscope.doc.ApiDescription.CONFIGURATION_DESCRIPTION
import com.sequenceiq.periscope.doc.ApiDescription.JSON

import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.periscope.doc.ApiDescription
import com.sequenceiq.periscope.api.model.ScalingConfigurationJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/clusters/{clusterId}/configurations")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/configurations", description = CONFIGURATION_DESCRIPTION, position = 2)
interface ConfigurationEndpoint {

    @POST
    @Path("scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ConfigurationOpDescription.CONFIGURATION_POST, produces = JSON, notes = ApiDescription.ConfigurationNotes.NOTES)
    fun setScalingConfiguration(@PathParam(value = "clusterId") clusterId: Long?, @Valid json: ScalingConfigurationJson): ScalingConfigurationJson

    @GET
    @Path("scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ApiDescription.ConfigurationOpDescription.CONFIGURATION_GET, produces = JSON, notes = ApiDescription.ConfigurationNotes.NOTES)
    fun getScalingConfiguration(@PathParam(value = "clusterId") clusterId: Long?): ScalingConfigurationJson

}
