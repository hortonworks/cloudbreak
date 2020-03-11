package com.sequenceiq.cloudbreak.service.environment.telemetry;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
            return accountTelemetryEndpoint.listRules();
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET AccountTelemetry with account id, due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
