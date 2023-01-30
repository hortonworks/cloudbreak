package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/distrox/carbon_dioxide")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v1/distrox/carbon_dioxide", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface DistroXCO2V1Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DistroXOpDescription.CO2, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_CO2_NOTES, nickname = "listDistroXCO2V1")
    RealTimeCO2Response list(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) List<String> datahubCrns);
}
