package com.sequenceiq.environment.api.v1.telemetry.endpoint;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.environment.api.doc.telemetry.AccountTelemetryDescription;
import com.sequenceiq.environment.api.v1.telemetry.model.request.AccountTelemetryRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.request.TestAnonymizationRuleRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.response.AccountTelemetryResponse;
import com.sequenceiq.environment.api.v1.telemetry.model.response.TestAnonymizationRuleResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/telemetry")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/telemetry")
public interface AccountTelemetryEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AccountTelemetryDescription.GET,
            description = AccountTelemetryDescription.GET_NOTES, operationId = "getAccountTelemetryV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AccountTelemetryResponse get();

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AccountTelemetryDescription.POST,
            description = AccountTelemetryDescription.POST_NOTES, operationId = "updateAccountTelemetryV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AccountTelemetryResponse update(@Valid AccountTelemetryRequest request);

    @GET
    @Path("/default")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AccountTelemetryDescription.GET_DEFAULT,
            description = AccountTelemetryDescription.GET_DEFAULT_NOTES, operationId = "getDefaultAccountTelemetryV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AccountTelemetryResponse getDefault();

    @GET
    @Path("/features")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AccountTelemetryDescription.GET,
            description = AccountTelemetryDescription.GET_NOTES, operationId = "listFeaturesV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FeaturesResponse listFeatures();

    @POST
    @Path("/features")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AccountTelemetryDescription.GET_FEATURES,
            description = AccountTelemetryDescription.GET_FEATURES_NOTES, operationId = "updateRulesV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FeaturesResponse updateFeatures(FeaturesRequest request);

    @GET
    @Path("/rules")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AccountTelemetryDescription.GET,
            description = AccountTelemetryDescription.GET_NOTES, operationId = "listRulesV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<AnonymizationRule> listRules();

    @GET
    @Path("/rules/{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AccountTelemetryDescription.GET,
            description = AccountTelemetryDescription.GET_NOTES, operationId = "listRulesInAccountV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<AnonymizationRule> listRulesInAccount(@PathParam("accountId") String accountId);

    @POST
    @Path("/rules/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = AccountTelemetryDescription.TEST,
            description = AccountTelemetryDescription.TEST_NOTES, operationId = "testRuleV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    TestAnonymizationRuleResponse testRulePattern(@NotNull @Valid TestAnonymizationRuleRequest request);
}
