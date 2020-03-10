package com.sequenceiq.periscope.api.endpoint.v1;

import static com.sequenceiq.periscope.doc.ApiDescription.CONFIGURATION_DESCRIPTION;
import static com.sequenceiq.periscope.doc.ApiDescription.ConfigurationNotes.NOTES;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.doc.ApiDescription.ConfigurationOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/clusters/{clusterId}/configurations")
@Consumes(MediaType.APPLICATION_JSON)
@Api(hidden = true, value = "/v1/configurations", description = CONFIGURATION_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ConfigurationEndpoint {

    @POST
    @Path("scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConfigurationOpDescription.CONFIGURATION_POST, produces = MediaType.APPLICATION_JSON, notes = NOTES)
    ScalingConfigurationRequest setScalingConfiguration(@PathParam("clusterId") Long clusterId, @Valid ScalingConfigurationRequest json);

    @GET
    @Path("scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConfigurationOpDescription.CONFIGURATION_GET, produces = MediaType.APPLICATION_JSON, notes = NOTES)
    ScalingConfigurationRequest getScalingConfiguration(@PathParam("clusterId") Long clusterId);

}
