package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundException;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_FQDN;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_REALM;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUSTSETUP_DNS_STATE;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SETUP_PILLAR;

import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.crossrealm.StackHelper;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsZoneService;
import com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation.TrustStatusValidationService;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class TrustProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustProvider.class);

    @Value("${freeipa.max.salt.trustsetup.maxretry}")
    private int maxRetryCount;

    @Value("${freeipa.max.salt.trustsetup.maxerrorretry}")
    private int maxRetryCountOnError;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private DnsZoneService dnsZoneService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackService stackService;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private TrustStatusValidationService trustStatusValidationService;

    @Inject
    private StackHelper stackHelper;

    public abstract KdcType kdcType();

    public abstract void prepare(Long stackId) throws Exception;

    public void configureDns(Long stackId) throws Exception {
        Stack stack = getStackService().getByIdWithListsInTransaction(stackId);
        CrossRealmTrust crossRealmTrust = getCrossRealmTrustService().getByStackId(stackId);

        dnsZoneService.addDnsForwardZone(freeIpaClientFactory, stack, crossRealmTrust);
        OrchestratorStateParams stateParams = createDnsSetupOrchestratorStateParams(stack, crossRealmTrust);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createDnsSetupOrchestratorStateParams(Stack stack, CrossRealmTrust crossRealmTrust) {
        OrchestratorStateParams stateParameters = saltStateParamsService.createStateParams(
                stack, TRUSTSETUP_DNS_STATE, true, maxRetryCount, maxRetryCountOnError);
        stateParameters.setStateParams(Map.of(FREEIPA, Map.of(TRUST_SETUP_PILLAR, Map.of(
                KDC_FQDN, crossRealmTrust.getKdcFqdn(),
                KDC_REALM, StringUtils.capitalize(crossRealmTrust.getKdcRealm())))));
        LOGGER.debug("Created OrchestratorStateParams for running cross-realm trust DNS set up: {}", stateParameters);
        return stateParameters;
    }

    public abstract void addTrust(Long stackId) throws Exception;

    public void validateTrust(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        CrossRealmTrust crossRealmTrust = crossRealmTrustService.getByStackId(stackId);

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

    public abstract void deleteTrust(Long stackId) throws Exception;

    public void deleteDnsZones(Long stackId) throws FreeIpaClientException {
        Stack stack = getStackService().getByIdWithListsInTransaction(stackId);
        CrossRealmTrust crossRealmTrust = getCrossRealmTrustService().getByStackId(stackId);
        FreeIpaClient client = getFreeIpaClientFactory().getFreeIpaClientForStack(stack);
        String realm = crossRealmTrust.getKdcRealm().toUpperCase(Locale.ROOT);
        ignoreNotFoundException(() -> client.deleteForwardDnsZone("in-addr.arpa."),
                "Deleting DNS forward zone for [in-addr.arpa.] but it was not found", realm);
        ignoreNotFoundException(() -> client.deleteForwardDnsZone(realm),
                "Deleting DNS forward zone for [{}] but it was not found", realm);
        LOGGER.debug("Deleting DNS forward zone for crossRealm [{}]", crossRealmTrust);
    }

    public abstract TrustSetupCommandsResponse buildTrustValidationCommandsResponse(String environmentCrn, Stack stack,
            FreeIpa freeIpa, CrossRealmTrust crossRealmTrust, LoadBalancer loadBalancer);

    public abstract TrustSetupCommandsResponse buildTrustSetupCommandsResponse(TrustCommandType trustCommandType, String environmentCrn, Stack stack,
            FreeIpa freeIpa, CrossRealmTrust crossRealmTrust, LoadBalancer loadBalancer);

    protected StackService getStackService() {
        return stackService;
    }

    protected CrossRealmTrustService getCrossRealmTrustService() {
        return crossRealmTrustService;
    }

    protected SaltStateParamsService getSaltStateParamsService() {
        return saltStateParamsService;
    }

    protected FreeIpaClientFactory getFreeIpaClientFactory() {
        return freeIpaClientFactory;
    }

    protected HostOrchestrator getHostOrchestrator() {
        return hostOrchestrator;
    }

    protected int getMaxRetryCount() {
        return maxRetryCount;
    }

    protected int getMaxRetryCountOnError() {
        return maxRetryCountOnError;
    }

    protected StackHelper getStackHelper() {
        return stackHelper;
    }
}
