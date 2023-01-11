package com.sequenceiq.sdx.api.endpoint;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/sdx/cost")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/sdx/cost", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface SdxCostEndpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "cost of SDX clusters", produces = MediaType.APPLICATION_JSON, nickname = "listSdxCost")
    RealTimeCostResponse list(@ValidCrn(resource = CrnResourceDescriptor.DATALAKE) List<String> sdxCrns);
}
