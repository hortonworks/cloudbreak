package com.sequenceiq.environment.api.v1.telemetry.endpoint;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.environment.api.doc.telemetry.AccountTelemetryDescription;
import com.sequenceiq.environment.api.v1.telemetry.model.request.AccountTelemetryRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.request.TestAnonymizationRuleRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.response.AccountTelemetryResponse;
import com.sequenceiq.environment.api.v1.telemetry.model.response.TestAnonymizationRuleResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RetryAndMetrics
@Path("/v1/telemetry")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/telemetry")
public interface AccountTelemetryEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  AccountTelemetryDescription.GET,
            description =  AccountTelemetryDescription.GET_NOTES, operationId ="getAccountTelemetryV1")
    AccountTelemetryResponse get();

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  AccountTelemetryDescription.POST,
            description =  AccountTelemetryDescription.POST_NOTES, operationId ="updateAccountTelemetryV1")
    AccountTelemetryResponse update(@Valid AccountTelemetryRequest request);

    @GET
    @Path("/default")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  AccountTelemetryDescription.GET_DEFAULT,
            description =  AccountTelemetryDescription.GET_DEFAULT_NOTES, operationId ="getDefaultAccountTelemetryV1")
    AccountTelemetryResponse getDefault();

    @GET
    @Path("/features")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  AccountTelemetryDescription.GET,
            description =  AccountTelemetryDescription.GET_NOTES, operationId ="listFeaturesV1")
    FeaturesResponse listFeatures();

    @POST
    @Path("/features")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  AccountTelemetryDescription.GET_FEATURES,
            description =  AccountTelemetryDescription.GET_FEATURES_NOTES, operationId ="updateRulesV1")
    FeaturesResponse updateFeatures(FeaturesRequest request);

    @GET
    @Path("/rules")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  AccountTelemetryDescription.GET,
            description =  AccountTelemetryDescription.GET_NOTES, operationId ="listRulesV1")
    List<AnonymizationRule> listRules();

    @GET
    @Path("/rules/{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  AccountTelemetryDescription.GET,
            description =  AccountTelemetryDescription.GET_NOTES, operationId ="listRulesInAccountV1")
    List<AnonymizationRule> listRulesInAccount(@AccountId @PathParam("accountId") String accountId);

    @POST
    @Path("/rules/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  AccountTelemetryDescription.TEST,
            description =  AccountTelemetryDescription.TEST_NOTES, operationId ="testRuleV1")
    TestAnonymizationRuleResponse testRulePattern(@NotNull @Valid TestAnonymizationRuleRequest request);
}
