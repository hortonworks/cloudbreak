package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.AD_DOMAIN;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.AD_IP;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY_ON_ERROR;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SETUP_PILLAR;
import static com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker.IPA_SERVER_TRUST_AD_PACKAGE;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResult;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResultType;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker;

@Service
public class TrustSetupValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrustSetupValidationService.class);

    private static final String AD_DNS_VALIDATION_STATE = "trustsetup/validation/ad_dns_validation";

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private IpaTrustAdPackageAvailabilityChecker packageAvailabilityChecker;

    @Inject
    private StackService stackService;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public TaskResults validateTrustSetup(Long stackId) {
        Optional<CrossRealmTrust> crossRealmTrust = crossRealmTrustService.getByStackIdIfExists(stackId);
        return crossRealmTrust
                .map(cr -> validateTrustSetup(stackId, cr))
                .orElse(new TaskResults().addTaskResult(new TaskResult(TaskResultType.ERROR, "No cross realm information is provided", Map.of())));
    }

    private TaskResults validateTrustSetup(Long stackId, CrossRealmTrust crossRealmTrust) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        TaskResults taskResults = new TaskResults();
        taskResults.addTaskResult(validatePackageAvailability(stackId));
        taskResults.addTaskResult(validateDns(stack, crossRealmTrust));
        return taskResults;
    }

    private TaskResult validatePackageAvailability(Long stackId) {
        LOGGER.info("Validate for IPA Server image trust packages");
        if (!packageAvailabilityChecker.isPackageAvailable(stackId)) {
            LOGGER.warn("Missing package [{}] required for AD trust setup", IPA_SERVER_TRUST_AD_PACKAGE);
            return new TaskResult(TaskResultType.ERROR, IPA_SERVER_TRUST_AD_PACKAGE
                    + " package is required for AD trust setup. Please upgrade to the latest image of FreeIPA.", Map.of());
        } else {
            return new TaskResult(TaskResultType.INFO, "Valid image trust packages", Map.of());
        }
    }

    private TaskResult validateDns(Stack stack, CrossRealmTrust crossRealmTrust) {
        try {
            OrchestratorStateParams stateParams = saltStateParamsService.createStateParams(stack, AD_DNS_VALIDATION_STATE, true, MAX_RETRY, MAX_RETRY_ON_ERROR);
            stateParams.setStateParams(Map.of(FREEIPA, Map.of(TRUST_SETUP_PILLAR, Map.of(
                    AD_DOMAIN, crossRealmTrust.getFqdn(),
                    AD_IP, crossRealmTrust.getIp()))));
            hostOrchestrator.runOrchestratorState(stateParams);
            return new TaskResult(TaskResultType.INFO, "Successful dns validation", Map.of());
        } catch (CloudbreakOrchestratorException orchestratorException) {
            LOGGER.error("Dns validation failed on AD: {}", crossRealmTrust.getFqdn(), orchestratorException);
            Map<String, String> params = OrchestratorExceptionAnalyzer.getNodeErrorParameters(orchestratorException);
            return new TaskResult(TaskResultType.ERROR, "Dns validation failed: " + orchestratorException.getMessage(), params);
        } catch (Exception ex) {
            LOGGER.error("Dns validation failed on AD: {}", crossRealmTrust.getFqdn(), ex);
            return new TaskResult(TaskResultType.ERROR, ex.getMessage(), Map.of());
        }
    }
}
