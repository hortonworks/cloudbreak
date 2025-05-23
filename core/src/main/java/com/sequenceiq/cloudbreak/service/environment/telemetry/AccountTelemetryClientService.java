package com.sequenceiq.cloudbreak.service.environment.telemetry;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.environment.api.v1.telemetry.endpoint.AccountTelemetryEndpoint;

@Service
public class AccountTelemetryClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountTelemetryClientService.class);

    @Inject
    private AccountTelemetryEndpoint accountTelemetryEndpoint;

    public List<AnonymizationRule> getAnonymizationRules() {
        try {
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> accountTelemetryEndpoint.listRulesInAccount(accountId));
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET AccountTelemetry with account id, due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
