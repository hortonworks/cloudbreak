package com.sequenceiq.periscope.api.endpoint;

import static com.sequenceiq.periscope.doc.ApiDescription.CONFIGURATION_DESCRIPTION;
import static com.sequenceiq.periscope.doc.ApiDescription.ConfigurationNotes.NOTES;
import static com.sequenceiq.periscope.doc.ApiDescription.JSON;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.ScalingConfigurationJson;
import com.sequenceiq.periscope.doc.ApiDescription.ConfigurationOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/clusters/{clusterId}/configurations")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/configurations", description = CONFIGURATION_DESCRIPTION, protocols = "http,https")
public interface ConfigurationEndpoint {

    @POST
    @Path("scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConfigurationOpDescription.CONFIGURATION_POST, produces = JSON, notes = NOTES)
    ScalingConfigurationJson setScalingConfiguration(@PathParam("clusterId") Long clusterId, @Valid ScalingConfigurationJson json);

    @GET
    @Path("scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConfigurationOpDescription.CONFIGURATION_GET, produces = JSON, notes = NOTES)
    ScalingConfigurationJson getScalingConfiguration(@PathParam("clusterId") Long clusterId);

}
