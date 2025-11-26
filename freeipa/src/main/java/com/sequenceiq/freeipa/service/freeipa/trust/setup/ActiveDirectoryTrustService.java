package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil.ignoreNotFoundException;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUSTSETUP_ADTRUST_INSTALL;

import java.util.Locale;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.ActiveDirectoryTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.BaseClusterTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Trust;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.ActiveDirectoryBaseClusterKrb5ConfBuilder;
import com.sequenceiq.freeipa.service.crossrealm.ActiveDirectoryCommandsBuilder;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

@Service
public class ActiveDirectoryTrustService extends TrustProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveDirectoryTrustService.class);

    @Inject
    private ActiveDirectoryCommandsBuilder activeDirectoryCommandsBuilder;

    @Inject
    private ActiveDirectoryBaseClusterKrb5ConfBuilder adBaseClusterKrb5ConfBuilder;

    @Override
    public KdcType kdcType() {
        return KdcType.ACTIVE_DIRECTORY;
    }

    @Override
    public void prepare(Long stackId) throws Exception {
        Stack stack = getStackService().getByIdWithListsInTransaction(stackId);
        OrchestratorStateParams stateParams = createAdTrustInstallOrchestratorStateParams(stack);
        getHostOrchestrator().runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createAdTrustInstallOrchestratorStateParams(Stack stack) {
        OrchestratorStateParams stateParameters = getSaltStateParamsService().createStateParams(stack, TRUSTSETUP_ADTRUST_INSTALL, false,
                getMaxRetryCount(), getMaxRetryCountOnError());
        LOGGER.debug("Created OrchestratorStateParams for running adtrust install: {}", stateParameters);
        return stateParameters;
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void addTrust(Long stackId) throws FreeIpaClientException {
        Stack stack = getStackService().getByIdWithListsInTransaction(stackId);
        CrossRealmTrust crossRealmTrust = getCrossRealmTrustService().getByStackId(stackId);

        FreeIpaClient client = getFreeIpaClientFactory().getFreeIpaClientForStack(stack);
        Trust trust = client.addTrust(crossRealmTrust.getTrustSecret(), "ad", true, crossRealmTrust.getKdcRealm().toUpperCase(Locale.ROOT));
        LOGGER.debug("Added Active Directory trust [{}] for crossRealm [{}], start validation", trust, crossRealmTrust);
    }

    @Override
    public void deleteTrust(Long stackId) throws Exception  {
        Stack stack = getStackService().getByIdWithListsInTransaction(stackId);
        CrossRealmTrust crossRealmTrust = getCrossRealmTrustService().getByStackId(stackId);
        FreeIpaClient client = getFreeIpaClientFactory().getFreeIpaClientForStack(stack);
        String realm = crossRealmTrust.getKdcRealm().toUpperCase(Locale.ROOT);
        ignoreNotFoundException(() -> client.deleteTrust(realm),
                "Deleting trust for [{}] but it was not found", realm);
        LOGGER.debug("Deleting trust for crossRealm [{}]", crossRealmTrust);
    }

    @Override
    public TrustSetupCommandsResponse buildTrustSetupCommandsResponse(TrustCommandType trustCommandType, String environmentCrn, Stack stack, FreeIpa freeIpa,
            CrossRealmTrust crossRealmTrust, LoadBalancer loadBalancer) {
        TrustSetupCommandsResponse response = new TrustSetupCommandsResponse();
        response.setEnvironmentCrn(environmentCrn);
        response.setKdcType(kdcType().name());

        ActiveDirectoryTrustSetupCommands adCommands = new ActiveDirectoryTrustSetupCommands();
        adCommands.setCommands(activeDirectoryCommandsBuilder.buildCommands(trustCommandType, stack, freeIpa, crossRealmTrust));
        response.setActiveDirectoryCommands(adCommands);

        BaseClusterTrustSetupCommands baseClusterTrustSetupCommands = new BaseClusterTrustSetupCommands();
        baseClusterTrustSetupCommands.setKrb5Conf(adBaseClusterKrb5ConfBuilder.buildCommands(stack.getResourceName(), trustCommandType, freeIpa,
                crossRealmTrust));
        response.setBaseClusterCommands(baseClusterTrustSetupCommands);

        return response;
    }
}
