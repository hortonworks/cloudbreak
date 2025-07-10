package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class ConfigureDnsServerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureDnsServerService.class);

    private static final String FORWARD_POLICY = "only";

    @Value("${freeipa.max.salt.trustsetup.maxretry}")
    private int maxRetryCount;

    @Value("${freeipa.max.salt.trustsetup.maxerrorretry}")
    private int maxRetryCountOnError;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

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
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getById(stackId);

        addDnsForwardZone(stack, crossRealmTrust);
        OrchestratorStateParams stateParams = createOrchestratorStateParams(primaryGatewayConfig, crossRealmTrust, stackId);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private void addDnsForwardZone(Stack stack, CrossRealmTrust crossRealmTrust) throws Exception {
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackIgnoreUnreachable(stack);

        String realm = crossRealmTrust.getRealm();
        LOGGER.info("Add forward DNS zone [{}] ", realm);
        Optional<DnsZone> dnsZone = ignoreNotFoundExceptionWithValue(() -> freeIpaClient.showForwardDnsZone(realm), null);
        if (dnsZone.isEmpty()) {
            LOGGER.debug("Forward DNS zone does not exists [{}], add it now", realm);
            freeIpaClient.addForwardDnsZone(realm, crossRealmTrust.getIp(), FORWARD_POLICY);
            LOGGER.debug("Forward DNS zone [{}] added", realm);
        }
    }

    private OrchestratorStateParams createOrchestratorStateParams(GatewayConfig primaryGatewayConfig, CrossRealmTrust crossRealmTrust, Long stackId) {
        OrchestratorStateParams stateParameters = new OrchestratorStateParams();
        stateParameters.setPrimaryGatewayConfig(primaryGatewayConfig);
        stateParameters.setTargetHostNames(Set.of(primaryGatewayConfig.getHostname()));
        stateParameters.setExitCriteriaModel(new StackBasedExitCriteriaModel(stackId));
        OrchestratorStateRetryParams stateRetryParams = new OrchestratorStateRetryParams();
        stateRetryParams.setMaxRetry(maxRetryCount);
        stateRetryParams.setMaxRetryOnError(maxRetryCountOnError);
        stateParameters.setStateRetryParams(stateRetryParams);
        stateParameters.setState("trustsetup.dns");
        stateParameters.setStateParams(Map.of("freeipa", Map.of("trust_setup", Map.of(
                "ad_domain", crossRealmTrust.getFqdn(),
                "realm", StringUtils.capitalize(crossRealmTrust.getRealm()))
        )));
        LOGGER.debug("Created OrchestratorStateParams for running cross-realm trust DNS set up: {}", stateParameters);
        return stateParameters;
    }
}
