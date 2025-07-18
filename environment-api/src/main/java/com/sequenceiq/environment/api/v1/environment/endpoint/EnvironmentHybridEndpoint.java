package com.sequenceiq.environment.api.v1.environment.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

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
import com.sequenceiq.environment.api.v1.environment.model.request.CancelCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.SetupCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.CancelCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustResponse;

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
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_CROSS_REALM_TRUST, description = ENVIRONMENT_NOTES,
            operationId = "setupCrossRealmTrustV1ByName", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    SetupCrossRealmTrustResponse setupByName(
            @PathParam("name") String environmentName,
            @Valid SetupCrossRealmTrustRequest request);

    @POST
    @Path("/cross_realm_trust/crn/{crn}/setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_CROSS_REALM_TRUST, description = ENVIRONMENT_NOTES,
            operationId = "setupCrossRealmTrustV1ByCrn", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    SetupCrossRealmTrustResponse setupByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @Valid SetupCrossRealmTrustRequest request);

    @POST
    @Path("/cross_realm_trust/name/{name}/finish_setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_FINISH_CROSS_REALM_TRUST, description = ENVIRONMENT_NOTES,
            operationId = "finishSetupCrossRealmTrustV1ByName", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    FinishSetupCrossRealmTrustResponse finishSetupByName(
            @PathParam("name") String environmentName,
            @Valid FinishSetupCrossRealmTrustRequest request);

    @POST
    @Path("/cross_realm_trust/crn/{crn}/finish_setup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.SETUP_FINISH_CROSS_REALM_TRUST, description = ENVIRONMENT_NOTES,
            operationId = "finishSetupCrossRealmTrustV1ByCrn", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    FinishSetupCrossRealmTrustResponse finishSetupByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @Valid FinishSetupCrossRealmTrustRequest request);

    @POST
    @Path("/cross_realm_trust/name/{name}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.CANCEL_CROSS_REALM_TRUST, description = ENVIRONMENT_NOTES,
            operationId = "cancelCrossRealmTrustV1ByName", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    CancelCrossRealmTrustResponse cancelByName(
            @PathParam("name") String environmentName,
            @Valid CancelCrossRealmTrustRequest request);

    @POST
    @Path("/cross_realm_trust/crn/{crn}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.CANCEL_CROSS_REALM_TRUST, description = ENVIRONMENT_NOTES,
            operationId = "cancelCrossRealmTrustV1ByCrn", responses = @ApiResponse(responseCode = "200", description = "successful operation",
            useReturnTypeSchema = true))
    CancelCrossRealmTrustResponse cancelByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @Valid CancelCrossRealmTrustRequest request);
}