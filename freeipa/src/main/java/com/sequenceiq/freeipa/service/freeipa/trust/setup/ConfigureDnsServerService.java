package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreEmptyModOrDuplicateException;
import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.AD_DOMAIN;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.REALM;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SETUP_PILLAR;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class ConfigureDnsServerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureDnsServerService.class);

    private static final String FORWARD_POLICY = "only";

    private static final String TRUSTSETUP_DNS_STATE = "trustsetup.dns";

    @Value("${freeipa.max.salt.trustsetup.maxretry}")
    private int maxRetryCount;

    @Value("${freeipa.max.salt.trustsetup.maxerrorretry}")
    private int maxRetryCountOnError;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackService stackService;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void configureDnsServer(Long stackId) throws Exception {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stackId);

        addDnsForwardZone(stack, crossRealmTrust);
        OrchestratorStateParams stateParams = createOrchestratorStateParams(stack, crossRealmTrust);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private void addDnsForwardZone(Stack stack, CrossRealmTrust crossRealmTrust) throws Exception {
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackIgnoreUnreachable(stack);

        String realm = crossRealmTrust.getRealm();
        LOGGER.info("Add forward DNS zone [{}] ", realm);
        Optional<DnsZone> dnsZone = ignoreNotFoundExceptionWithValue(() -> freeIpaClient.showForwardDnsZone(realm), null);
        if (dnsZone.isEmpty()) {
            LOGGER.debug("Forward DNS zone does not exists [{}], add it now", realm);
            ignoreEmptyModOrDuplicateException(() -> freeIpaClient.addForwardDnsZone(realm, crossRealmTrust.getIp(), FORWARD_POLICY), null);
            LOGGER.debug("Forward DNS zone [{}] added", realm);
        }
        ignoreEmptyModOrDuplicateException(() -> freeIpaClient.addForwardDnsZone("in-addr.arpa.", crossRealmTrust.getIp(), FORWARD_POLICY), null);
    }

    private OrchestratorStateParams createOrchestratorStateParams(Stack stack, CrossRealmTrust crossRealmTrust) {
        OrchestratorStateParams stateParameters = saltStateParamsService.createStateParams(
                stack, TRUSTSETUP_DNS_STATE, true, maxRetryCount, maxRetryCountOnError);
        stateParameters.setStateParams(Map.of(FREEIPA, Map.of(TRUST_SETUP_PILLAR, Map.of(
                AD_DOMAIN, crossRealmTrust.getFqdn(),
                REALM, StringUtils.capitalize(crossRealmTrust.getRealm())))));
        LOGGER.debug("Created OrchestratorStateParams for running cross-realm trust DNS set up: {}", stateParameters);
        return stateParameters;
    }
}
