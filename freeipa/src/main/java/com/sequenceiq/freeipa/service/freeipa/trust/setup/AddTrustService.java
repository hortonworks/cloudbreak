package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import java.util.Locale;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
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
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;

@Service
public class AddTrustService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddTrustService.class);

    private static final String KDC_TYPE_ACTIVE_DIRECTORY = "Active Directory";

    @Inject
    private StackService stackService;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private TrustStatusValidationService trustStatusValidationService;

    @Inject
    private RemoteEnvironmentEndpoint remoteEnvironmentEndpoint;

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void addAndValidateTrust(Long stackId) throws FreeIpaClientException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stackId);
        if (isActiveDirectoryTrust(crossRealmTrust)) {
            FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            Trust trust = client.addTrust(crossRealmTrust.getTrustSecret(), "ad", true, crossRealmTrust.getRealm().toUpperCase(Locale.ROOT));
            LOGGER.debug("Added Active Directory trust [{}] for crossRealm [{}], start validation", trust, crossRealmTrust);
        }
        ValidationResult validationResult = trustStatusValidationService.validateTrustStatus(stack, crossRealmTrust);
        if (validationResult.hasError()) {
            String message = "Failed to validate trust on FreeIPA: " + validationResult.getFormattedErrors();
            LOGGER.error(message);
            throw new IllegalStateException(message);
        } else if (validationResult.hasWarning()) {
            LOGGER.warn("Successful validation of crossRealm trust [{}] with warnings [{}]", crossRealmTrust, validationResult.getFormattedWarnings());
        } else {
            LOGGER.info("Successful validation of crossRealm trust [{}] without warnings", crossRealmTrust);
        }
    }

    private boolean isActiveDirectoryTrust(CrossRealmTrust crossRealmTrust) {
        DescribeRemoteEnvironment describeRemoteEnvironment = new DescribeRemoteEnvironment();
        describeRemoteEnvironment.setCrn(crossRealmTrust.getRemoteEnvironmentCrn());
        DescribeEnvironmentResponse describeRemoteEnvironmentResponse = remoteEnvironmentEndpoint.getByCrn(describeRemoteEnvironment);
        return KDC_TYPE_ACTIVE_DIRECTORY.equals(Optional.of(describeRemoteEnvironmentResponse.getEnvironment())
                .map(Environment::getPvcEnvironmentDetails)
                .map(PvcEnvironmentDetails::getPrivateDatalakeDetails)
                .map(PrivateDatalakeDetails::getKerberosInfo)
                .map(KerberosInfo::getKdcType)
                .orElse(KDC_TYPE_ACTIVE_DIRECTORY));
    }
}
