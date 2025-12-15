package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA_REALM;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_FQDN;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_REALM;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUSTCANCEL_DEL_MIT_TRUST;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUSTSETUP_ADD_TRUST;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SECRET;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SETUP_PILLAR;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;
import com.sequenceiq.freeipa.service.crossrealm.commands.mit.MitBaseClusterTrustCommandsBuilder;
import com.sequenceiq.freeipa.service.crossrealm.commands.mit.MitTrustInstructionsBuilder;

@Service
public class MitKdcTrustService extends TrustProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MitKdcTrustService.class);

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private MitTrustInstructionsBuilder mitTrustInstructionsBuilder;

    @Inject
    private MitBaseClusterTrustCommandsBuilder mitBaseClusterTrustCommandsBuilder;

    @Override
    public KdcType kdcType() {
        return KdcType.MIT;
    }

    @Override
    public void prepare(Long stackId) throws Exception {
        LOGGER.info("No preparation steps are needed for MIT KDC trusts.");
    }

    @Override
    public void addTrust(Long stackId) throws Exception {
        Stack stack = getStackService().getByIdWithListsInTransaction(stackId);
        OrchestratorStateParams stateParams = createPrepareKdcOrchestratorStateParams(stack);
        getHostOrchestrator().runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createPrepareKdcOrchestratorStateParams(Stack stack) {
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn());
        CrossRealmTrust crossRealmTrust = getCrossRealmTrustService().getByStackId(stack.getId());
        OrchestratorStateParams stateParameters = getSaltStateParamsService().createStateParams(stack, TRUSTSETUP_ADD_TRUST, false,
                getMaxRetryCount(), getMaxRetryCountOnError());
        stateParameters.setStateParams(Map.of(FREEIPA, Map.of(TRUST_SETUP_PILLAR, Map.of(
                FREEIPA_REALM, kerberosConfig.getRealm(),
                KDC_FQDN, crossRealmTrust.getKdcFqdn(),
                KDC_REALM, StringUtils.capitalize(crossRealmTrust.getKdcRealm()),
                TRUST_SECRET, crossRealmTrust.getTrustSecret()))));
        LOGGER.debug("Created OrchestratorStateParams for running prepare MIT KDC: {}", stateParameters);
        return stateParameters;
    }

    @Override
    public void deleteTrust(Long stackId) throws Exception {
        Stack stack = getStackService().getByIdWithListsInTransaction(stackId);
        OrchestratorStateParams stateParams = createDeleteKdcPrincipalsOrchestratorStateParams(stack);
        getHostOrchestrator().runOrchestratorState(stateParams);
    }

    @Override
    public TrustSetupCommandsResponse buildTrustSetupCommandsResponse(TrustCommandType trustCommandType, String environmentCrn, Stack stack, FreeIpa freeIpa,
            CrossRealmTrust crossRealmTrust, LoadBalancer loadBalancer) {
        TrustSetupCommandsResponse response = new TrustSetupCommandsResponse();
        response.setEnvironmentCrn(environmentCrn);
        response.setKdcType(kdcType().name());
        response.setMitCommands(mitTrustInstructionsBuilder.buildInstructions(trustCommandType, stack, freeIpa, crossRealmTrust));
        response.setBaseClusterCommands(mitBaseClusterTrustCommandsBuilder.buildBaseClusterCommands(stack, trustCommandType, freeIpa,
                crossRealmTrust, loadBalancer));
        return response;
    }

    private OrchestratorStateParams createDeleteKdcPrincipalsOrchestratorStateParams(Stack stack) {
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn());
        CrossRealmTrust crossRealmTrust = getCrossRealmTrustService().getByStackId(stack.getId());
        OrchestratorStateParams stateParameters = getSaltStateParamsService().createStateParams(stack, TRUSTCANCEL_DEL_MIT_TRUST, false,
                getMaxRetryCount(), getMaxRetryCountOnError());
        stateParameters.setStateParams(Map.of(FREEIPA, Map.of(TRUST_SETUP_PILLAR, Map.of(
                FREEIPA_REALM, kerberosConfig.getRealm(),
                KDC_REALM, StringUtils.capitalize(crossRealmTrust.getKdcRealm())))));
        LOGGER.debug("Created OrchestratorStateParams for running delete MIT KDC principals: {}", stateParameters);
        return stateParameters;
    }
}
