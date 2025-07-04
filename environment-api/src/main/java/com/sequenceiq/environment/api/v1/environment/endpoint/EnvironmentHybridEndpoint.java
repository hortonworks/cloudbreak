package com.sequenceiq.environment.api.v1.environment.endpoint;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentFinishSetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentSetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentSetupCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishCrossRealmTrustResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/env/hybrid")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/env/hybrid")
public interface EnvironmentHybridEndpoint {

    @POST
    @Path("/cross_realm_trust/name/{name}/setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_CROSS_REALM_TRUST, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "setupCrossRealmTrustV1ByName", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    EnvironmentSetupCrossRealmTrustResponse setupByName(
            @PathParam("name") String environmentName,
            @Valid EnvironmentSetupCrossRealmTrustRequest request);

    @POST
    @Path("/cross_realm_trust/crn/{crn}/setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_CROSS_REALM_TRUST, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "setupCrossRealmTrustV1ByCrn", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    EnvironmentSetupCrossRealmTrustResponse setupByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @Valid EnvironmentSetupCrossRealmTrustRequest request);

    @POST
    @Path("/cross_realm_trust/name/{name}/finish_setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_FINISH_CROSS_REALM_TRUST, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "finishSetupCrossRealmTrustV1ByName", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    FinishCrossRealmTrustResponse finishSetupByName(
            @PathParam("name") String environmentName,
            @Valid EnvironmentFinishSetupCrossRealmTrustRequest request);

    @POST
    @Path("/cross_realm_trust/crn/{crn}/finish_setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_FINISH_CROSS_REALM_TRUST, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "finishSetupCrossRealmTrustV1ByCrn", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    FinishCrossRealmTrustResponse finishSetupByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @Valid EnvironmentFinishSetupCrossRealmTrustRequest request);
}