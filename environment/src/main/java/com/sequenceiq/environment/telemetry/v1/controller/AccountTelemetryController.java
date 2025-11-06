package com.sequenceiq.environment.telemetry.v1.controller;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.telemetry.response.FeaturesResponse;
import com.sequenceiq.environment.api.v1.telemetry.endpoint.AccountTelemetryEndpoint;
import com.sequenceiq.environment.api.v1.telemetry.model.request.AccountTelemetryRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.request.TestAnonymizationRuleRequest;
import com.sequenceiq.environment.api.v1.telemetry.model.response.AccountTelemetryResponse;
import com.sequenceiq.environment.api.v1.telemetry.model.response.TestAnonymizationRuleResponse;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;
import com.sequenceiq.environment.telemetry.service.AccountTelemetryService;
import com.sequenceiq.environment.telemetry.v1.converter.AccountTelemetryConverter;
import com.sequenceiq.notification.WebSocketNotificationController;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class AccountTelemetryController extends WebSocketNotificationController implements AccountTelemetryEndpoint {

    private final AccountTelemetryService accountTelemetryService;

    private final AccountTelemetryConverter accountTelemetryConverter;

    public AccountTelemetryController(AccountTelemetryService accountTelemetryService,
            AccountTelemetryConverter accountTelemetryConverter) {
        this.accountTelemetryService = accountTelemetryService;
        this.accountTelemetryConverter = accountTelemetryConverter;
    }

    @Override
    @DisableCheckPermissions
    public AccountTelemetryResponse get() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return accountTelemetryConverter.convert(accountTelemetryService.getOrDefault(accountId));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public AccountTelemetryResponse update(AccountTelemetryRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        AccountTelemetry telemetry = accountTelemetryConverter.convert(request);
        return accountTelemetryConverter.convert(accountTelemetryService.create(telemetry, accountId));
    }

    @Override
    @DisableCheckPermissions
    public AccountTelemetryResponse getDefault() {
        return accountTelemetryConverter.convert(accountTelemetryService.createDefaultAccountTelemetry());
    }

    @Override
    @DisableCheckPermissions
    public FeaturesResponse listFeatures() {
        AccountTelemetryResponse response = get();
        return response.getFeatures();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FeaturesResponse updateFeatures(FeaturesRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return accountTelemetryConverter.convertFeatures(
                accountTelemetryService.updateFeatures(
                        accountId, accountTelemetryConverter.convertFeatures(request)));
    }

    @Override
    @DisableCheckPermissions
    public List<AnonymizationRule> listRules() {
        AccountTelemetryResponse response = get();
        return response.getRules();
    }

    @Override
    @InternalOnly
    public List<AnonymizationRule> listRulesInAccount(@AccountId String accountId) {
        AccountTelemetryResponse response = accountTelemetryConverter.convert(accountTelemetryService.getOrDefault(accountId));
        return response.getRules();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public TestAnonymizationRuleResponse testRulePattern(TestAnonymizationRuleRequest request) {
        TestAnonymizationRuleResponse response = new TestAnonymizationRuleResponse();
        response.setOutput(accountTelemetryService.testRulePatterns(request.getRules(), request.getInput()));
        return response;
    }
}
