package com.sequenceiq.sdx.api.endpoint;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/sdx/carbon_dioxide")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/sdx/carbon_dioxide", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxCO2Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "CO2 cost of SDX clusters", produces = MediaType.APPLICATION_JSON, nickname = "listSdxCO2")
    RealTimeCO2Response list(@ValidCrn(resource = CrnResourceDescriptor.DATALAKE) List<String> sdxCrns);
}
