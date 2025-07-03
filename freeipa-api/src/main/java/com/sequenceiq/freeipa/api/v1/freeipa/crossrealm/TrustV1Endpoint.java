package com.sequenceiq.freeipa.api.v1.freeipa.crossrealm;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/cross_realm_trust")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/cross_realm_trust")
public interface TrustV1Endpoint {

    @POST
    @Path("/setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_CROSS_REALM_TRUST, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "prepareCrossRealmTrustV1", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    PrepareCrossRealmTrustResponse setup(@Valid PrepareCrossRealmTrustRequest request);

    @POST
    @Path("/finish_setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_FINISH_CROSS_REALM_TRUST, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "finishCrossRealmTrustV1", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    FinishCrossRealmTrustResponse finishSetup(@Valid FinishCrossRealmTrustRequest request);
}
