package com.sequenceiq.freeipa.api.v2.freeipa.crossrealm;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustV2Request;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v2/cross_realm_trust")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v2/cross_realm_trust")
public interface TrustV2Endpoint {

    @POST
    @Path("/setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_CROSS_REALM_TRUST, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "prepareCrossRealmTrustV2", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    PrepareCrossRealmTrustResponse setup(@Valid PrepareCrossRealmTrustV2Request request, @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
