package com.sequenceiq.environment.api.v1.telemetry.endpoint;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.environment.api.doc.telemetry.AccountTelemetryDescription;
import com.sequenceiq.environment.api.v1.telemetry.model.request.AccountTelemetryRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.request.TestAnonymizationRuleRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.response.AccountTelemetryResponse;
import com.sequenceiq.environment.api.v1.telemetry.model.response.TestAnonymizationRuleResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/telemetry")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/telemetry", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface AccountTelemetryEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountTelemetryDescription.GET, produces = MediaType.APPLICATION_JSON,
            notes = AccountTelemetryDescription.GET_NOTES, nickname = "getAccountTelemetryV1")
    AccountTelemetryResponse get();

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountTelemetryDescription.POST, produces = MediaType.APPLICATION_JSON,
            notes = AccountTelemetryDescription.POST_NOTES, nickname = "updateAccountTelemetryV1")
    AccountTelemetryResponse update(@Valid AccountTelemetryRequest request);

    @GET
    @Path("/default")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountTelemetryDescription.GET_DEFAULT, produces = MediaType.APPLICATION_JSON,
            notes = AccountTelemetryDescription.GET_DEFAULT_NOTES, nickname = "getDefaultAccountTelemetryV1")
    AccountTelemetryResponse getDefault();

    @GET
    @Path("/features")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountTelemetryDescription.GET, produces = MediaType.APPLICATION_JSON,
            notes = AccountTelemetryDescription.GET_NOTES, nickname = "listFeaturesV1")
    FeaturesResponse listFeatures();

    @POST
    @Path("/features")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountTelemetryDescription.GET_FEATURES, produces = MediaType.APPLICATION_JSON,
            notes = AccountTelemetryDescription.GET_FEATURES_NOTES, nickname = "updateRulesV1")
    FeaturesResponse updateFeatures(FeaturesRequest request);

    @GET
    @Path("/rules")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountTelemetryDescription.GET, produces = MediaType.APPLICATION_JSON,
            notes = AccountTelemetryDescription.GET_NOTES, nickname = "listRulesV1")
    List<AnonymizationRule> listRules();

    @GET
    @Path("/rules/test")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = AccountTelemetryDescription.TEST, produces = MediaType.APPLICATION_JSON,
            notes = AccountTelemetryDescription.TEST_NOTES, nickname = "testRuleV1")
    TestAnonymizationRuleResponse testRulePattern(@NotNull @Valid TestAnonymizationRuleRequest request);
}
