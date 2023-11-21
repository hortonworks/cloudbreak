package com.sequenceiq.freeipa.api.v2.freeipa;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RetryAndMetrics
@Path("/v2/freeipa")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v2/freeipa",
        protocols = "http,https",
        authorizations = {@Authorization(value = FreeIpaApi.CRN_HEADER_API_KEY)},
        consumes = MediaType.APPLICATION_JSON)
public interface FreeIpaV2Endpoint {

    @POST
    @Path("v2/rebuild")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.REBUILD, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "rebuildV2")
    DescribeFreeIpaResponse rebuildv2(@Valid RebuildV2Request request) throws Exception;
}
