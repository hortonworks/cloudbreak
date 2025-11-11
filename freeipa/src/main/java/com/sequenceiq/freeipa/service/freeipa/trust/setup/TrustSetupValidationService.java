package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.DNS_IP;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_FQDN;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_IP;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY_ON_ERROR;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SETUP_PILLAR;
import static com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker.IPA_SERVER_TRUST_AD_PACKAGE;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
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
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;

@Service
public class TrustSetupValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrustSetupValidationService.class);

    private static final String AD_DNS_VALIDATION_STATE = "trustsetup/validation/ad_dns_validation";

    private static final String AD_REVERSE_DNS_VALIDATION_STATE = "trustsetup/validation/ad_reverse_dns_validation";

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

    @Inject
    private RemoteEnvironmentEndpoint remoteEnvironmentEndpoint;

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
        taskResults.addTaskResult(validateReverseDns(stack, crossRealmTrust));
        taskResults.addTaskResult(validateKerberization(crossRealmTrust));
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
        return executeSaltState(stack, crossRealmTrust, AD_DNS_VALIDATION_STATE, "Dns validation");
    }

    private TaskResult validateReverseDns(Stack stack, CrossRealmTrust crossRealmTrust) {
        return executeSaltState(stack, crossRealmTrust, AD_REVERSE_DNS_VALIDATION_STATE, "Reverse dns validation");
    }

    private TaskResult validateKerberization(CrossRealmTrust crossRealmTrust) {
        if (StringUtils.isNotBlank(crossRealmTrust.getRemoteEnvironmentCrn())) {
            DescribeRemoteEnvironment describeRemoteEnvironment = new DescribeRemoteEnvironment();
            describeRemoteEnvironment.setCrn(crossRealmTrust.getRemoteEnvironmentCrn());
            DescribeEnvironmentResponse describeRemoteEnvironmentResponse = remoteEnvironmentEndpoint.getByCrn(describeRemoteEnvironment);
            boolean kerberized = Optional.of(describeRemoteEnvironmentResponse.getEnvironment())
                    .map(Environment::getPvcEnvironmentDetails)
                    .map(PvcEnvironmentDetails::getPrivateDatalakeDetails)
                    .map(PrivateDatalakeDetails::getKerberosInfo)
                    .map(KerberosInfo::getKerberized)
                    .orElse(false);
            return kerberized ? new TaskResult(TaskResultType.INFO, "The on premises cluster is kerberized", Map.of()) :
                    new TaskResult(TaskResultType.ERROR, "The on premises cluster is not kerberized", Map.of());
        } else {
            return new TaskResult(TaskResultType.WARNING, "Remote environment crn is missing", Map.of());
        }
    }

    private TaskResult executeSaltState(Stack stack, CrossRealmTrust crossRealmTrust, String stateName, String messagePrefix) {
        try {
            OrchestratorStateParams stateParams = saltStateParamsService.createStateParams(stack, stateName, true,
                    MAX_RETRY, MAX_RETRY_ON_ERROR);
            stateParams.setStateParams(Map.of(FREEIPA, Map.of(TRUST_SETUP_PILLAR, Map.of(
                    KDC_FQDN, crossRealmTrust.getKdcFqdn(),
                    KDC_IP, crossRealmTrust.getKdcIp(),
                    DNS_IP, crossRealmTrust.getDnsIp()))));
            hostOrchestrator.runOrchestratorState(stateParams);
            return new TaskResult(TaskResultType.INFO, "Successful " + messagePrefix, Map.of());
        } catch (CloudbreakOrchestratorException orchestratorException) {
            LOGGER.error("{} failed on AD: {}", messagePrefix, crossRealmTrust.getKdcFqdn(), orchestratorException);
            Map<String, String> params = OrchestratorExceptionAnalyzer.getNodeErrorParameters(orchestratorException);
            return new TaskResult(TaskResultType.ERROR, messagePrefix + " failed: " + orchestratorException.getMessage(), params);
        } catch (Exception ex) {
            LOGGER.error("{} failed on AD: {}", messagePrefix, crossRealmTrust.getKdcFqdn(), ex);
            return new TaskResult(TaskResultType.ERROR, ex.getMessage(), Map.of());
        }
    }
}
