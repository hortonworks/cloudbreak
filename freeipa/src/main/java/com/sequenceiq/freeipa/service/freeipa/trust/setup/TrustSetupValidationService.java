package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.COMMENT;
import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.HostSaltCommands;
import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.NAME;
import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.STDERR;
import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.STDOUT;
import static com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer.getHostSaltCommands;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.DNS_IP;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.FREEIPA;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_FQDN;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.KDC_IP;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.MAX_RETRY_ON_ERROR;
import static com.sequenceiq.freeipa.service.freeipa.trust.TrustSaltStateParamsConstants.TRUST_SETUP_PILLAR;
import static com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker.IPA_SERVER_TRUST_AD_PACKAGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResult;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResultType;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.IpaTrustAdPackageAvailabilityChecker;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.ValidateForDatalakeRequest;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.ValidateForDatalakeResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.ValidateForDatalakeValidationResponse;

@Service
public class TrustSetupValidationService {

    protected static final String DOCS = "docs";

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustSetupValidationService.class);

    private static final String AD_DNS_VALIDATION_STATE = "trustsetup/validation/ad_dns_validation";

    private static final String AD_REVERSE_DNS_VALIDATION_STATE = "trustsetup/validation/ad_reverse_dns_validation";

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

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

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private EnvironmentService environmentService;

    public TaskResults validateTrustSetup(Long stackId) {
        Optional<CrossRealmTrust> crossRealmTrust = crossRealmTrustService.getByStackIdIfExists(stackId);
        return crossRealmTrust
                .map(cr -> validateTrustSetup(stackId, cr))
                .orElse(new TaskResults().addTaskResult(new TaskResult(
                        TaskResultType.ERROR, messagesService.getMessage("trust.validation.notfound"), Map.of())));
    }

    private TaskResults validateTrustSetup(Long stackId, CrossRealmTrust crossRealmTrust) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        EnvironmentType environmentType = environmentService.getEnvironmentType(stack.getEnvironmentCrn());
        TaskResults taskResults = new TaskResults();
        taskResults.addTaskResult(validateLoadBalancer(stackId));
        taskResults.addTaskResult(validatePackageAvailability(stackId, crossRealmTrust));
        taskResults.addTaskResult(validateDns(stack, crossRealmTrust));
        taskResults.addTaskResult(validateReverseDns(stack, crossRealmTrust));
        if (EnvironmentType.HYBRID.equals(environmentType)) {
            validateForDatalake(crossRealmTrust.getRemoteEnvironmentCrn())
                    .forEach(taskResults::addTaskResult);
        }
        return taskResults;
    }

    private TaskResult validateLoadBalancer(Long stackId) {
        LOGGER.info("Validate for IPA load balancer");
        return freeIpaLoadBalancerService.findByStackId(stackId)
                .map(lb -> new TaskResult(TaskResultType.INFO, messagesService.getMessage("trust.validation.loadbalancer.success"), Map.of()))
                .orElse(new TaskResult(TaskResultType.ERROR, messagesService.getMessage("trust.validation.loadbalancer.failure"), Map.of(
                        COMMENT, messagesService.getMessage("trust.validation.loadbalancer.comment")
                )));
    }

    private TaskResult validatePackageAvailability(Long stackId, CrossRealmTrust crossRealmTrust) {
        LOGGER.info("Validate for IPA Server image trust packages");
        if (KdcType.ACTIVE_DIRECTORY.equals(crossRealmTrust.getKdcType()) && !packageAvailabilityChecker.isPackageAvailable(stackId)) {
            LOGGER.warn("Missing package [{}] required for AD trust setup", IPA_SERVER_TRUST_AD_PACKAGE);
            return new TaskResult(TaskResultType.ERROR, messagesService.getMessage("trust.validation.packageavailability.failure"),
                    Map.of(COMMENT, messagesService.getMessageWithArgs("trust.validation.packageavailability.comment", IPA_SERVER_TRUST_AD_PACKAGE)));
        } else {
            return new TaskResult(TaskResultType.INFO, messagesService.getMessage("trust.validation.packageavailability.success"), Map.of());
        }
    }

    private TaskResult validateDns(Stack stack, CrossRealmTrust crossRealmTrust) {
        return executeSaltState(stack, crossRealmTrust, AD_DNS_VALIDATION_STATE, messagesService.getMessage("trust.validation.dns"),
                messagesService.getMessage("trust.validation.dns.comment"), DocumentationLinkProvider.hybridDnsArchitectureLink());
    }

    private TaskResult validateReverseDns(Stack stack, CrossRealmTrust crossRealmTrust) {
        return executeSaltState(stack, crossRealmTrust, AD_REVERSE_DNS_VALIDATION_STATE, messagesService.getMessage("trust.validation.reversedns"),
                messagesService.getMessage("trust.validation.reversedns.comment"), DocumentationLinkProvider.hybridDnsArchitectureLink());
    }

    private Collection<TaskResult> validateForDatalake(String remoteEnvironmentCrn) {
        List<TaskResult> taskResults = new ArrayList<>();
        try {
            if (StringUtils.isNotBlank(remoteEnvironmentCrn)) {
                ValidateForDatalakeRequest request = new ValidateForDatalakeRequest();
                request.setCrn(remoteEnvironmentCrn);
                ValidateForDatalakeResponse validateForDatalakeResponse = remoteEnvironmentEndpoint.validateForDatalake(request);
                validateForDatalakeResponse.getValidations().stream()
                        .filter(this::isFailedOrHasMessage)
                        .map(this::toTaskResult)
                        .forEach(taskResults::add);
            } else {
                taskResults.add(new TaskResult(TaskResultType.ERROR, messagesService.getMessage("trust.validation.datalake.failure"),
                        Map.of(COMMENT, messagesService.getMessage("trust.validation.datalake.comment.missingcrn"))));
            }
        } catch (RuntimeException e) {
            String message = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("An error occurred during the datalake validation: {}", message, e);
            taskResults.add(new TaskResult(TaskResultType.ERROR, messagesService.getMessage("trust.validation.datalake.failure"), Map.of()));
        }
        return taskResults;
    }

    private boolean isFailedOrHasMessage(ValidateForDatalakeValidationResponse validation) {
        return !validation.isPassed() || StringUtils.isNotBlank(validation.getMessage());
    }

    private TaskResult toTaskResult(ValidateForDatalakeValidationResponse validation) {
        TaskResultType taskResultType = validation.isPassed() ? TaskResultType.INFO : TaskResultType.ERROR;
        String message = validation.getMessage();
        Map<String, String> additionalParams = new HashMap<>();
        messagesService.getMessageIfExists(String.format("trust.validation.%s.comment", validation.getValidationType()))
                        .ifPresent(comment -> additionalParams.put(COMMENT, comment));
        Optional.ofNullable(switch (validation.getValidationType()) {
            case "KERBERIZED" -> DocumentationLinkProvider.hybridSecurityRequirements();
            default -> null;
        }).ifPresent(docs -> additionalParams.put(DOCS, docs));
        return new TaskResult(taskResultType, message, additionalParams);
    }

    private TaskResult executeSaltState(Stack stack, CrossRealmTrust crossRealmTrust, String stateName, String messagePrefix, String comment, String docs) {
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
            LOGGER.error("{} failed on KDC: {}", messagePrefix, crossRealmTrust.getKdcFqdn(), orchestratorException);
            Set<HostSaltCommands> hostSaltCommands = getHostSaltCommands(orchestratorException);
            return new TaskResult(TaskResultType.ERROR, messagePrefix + " failed", hostSaltCommands.stream()
                    .findFirst()
                    .flatMap(hsc -> hsc.saltCommands().stream().findFirst())
                    .map(saltCommand -> getAdditionalParams(saltCommand.params(), comment, docs))
                    .orElse(Map.of(COMMENT, orchestratorException.getMessage())));
        } catch (Exception ex) {
            LOGGER.error("{} failed for KDC: {}", messagePrefix, crossRealmTrust.getKdcFqdn(), ex);
            return new TaskResult(TaskResultType.ERROR, messagePrefix + " failed", Map.of(COMMENT, ex.getMessage()));
        }
    }

    private Map<String, String> getAdditionalParams(Map<String, String> saltCommandParams, String comment, String docs) {
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(NAME, saltCommandParams.get(NAME));
        additionalParams.put(STDOUT, saltCommandParams.get(STDOUT));
        additionalParams.put(STDERR, saltCommandParams.get(STDERR));
        additionalParams.put(COMMENT, comment);
        if (docs != null) {
            additionalParams.put(DOCS, docs);
        }
        return additionalParams;
    }
}
