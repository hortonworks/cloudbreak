package com.sequenceiq.freeipa.api.v1.co2;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RetryAndMetrics
@Path("/v1/freeipa/carbon_dioxide")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa/carbon_dioxide",
        protocols = "http,https",
        authorizations = {@Authorization(value = FreeIpaApi.CRN_HEADER_API_KEY)},
        consumes = MediaType.APPLICATION_JSON)
public interface FreeIpaCO2V1Endpoint {

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.CO2, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "listFreeIpaCO2V1")
    RealTimeCO2Response list(List<String> environmentCrns, @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
