package com.sequenceiq.freeipa.api.v1.cost;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RetryAndMetrics
@Path("/v1/freeipa/cost")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa/cost",
        protocols = "http,https",
        authorizations = {@Authorization(value = FreeIpaApi.CRN_HEADER_API_KEY)},
        consumes = MediaType.APPLICATION_JSON)
public interface FreeIpaCostV1Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.COST, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "listFreeIpaCostV1")
    RealTimeCostResponse list();

}
