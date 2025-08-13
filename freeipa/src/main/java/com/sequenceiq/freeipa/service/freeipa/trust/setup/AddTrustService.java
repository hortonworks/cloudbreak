package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import java.util.Locale;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Trust;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation.TrustStatusValidationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class AddTrustService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddTrustService.class);

    @Inject
    private StackService stackService;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private TrustStatusValidationService trustStatusValidationService;

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void addAndValidateTrust(Long stackId) throws FreeIpaClientException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stackId);
        FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        Trust trust = client.addTrust(crossRealmTrust.getTrustSecret(), "ad", true, crossRealmTrust.getRealm().toUpperCase(Locale.ROOT));
        LOGGER.debug("Added trust [{}] for crossRealm [{}], start validation", trust, crossRealmTrust);
        ValidationResult validationResult = trustStatusValidationService.validateTrustStatus(stack, crossRealmTrust);
        if (validationResult.hasError()) {
            String message = "Failed to validate trust on FreeIPA: " + validationResult.getFormattedErrors();
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }
        LOGGER.debug("Successful validation of trust [{}] for crossRealm [{}]", trust, crossRealmTrust);
    }
}
