package com.sequenceiq.freeipa.service.telemetry;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.environment.api.v1.telemetry.endpoint.AccountTelemetryEndpoint;

@Service
public class AccountTelemetryService {

    private final AccountTelemetryEndpoint accountTelemetryEndpoint;

    public AccountTelemetryService(AccountTelemetryEndpoint accountTelemetryEndpoint) {
        this.accountTelemetryEndpoint = accountTelemetryEndpoint;
    }

    public List<AnonymizationRule> getAnonymizationRules(String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> accountTelemetryEndpoint.listRulesInAccount(accountId));
    }
}
