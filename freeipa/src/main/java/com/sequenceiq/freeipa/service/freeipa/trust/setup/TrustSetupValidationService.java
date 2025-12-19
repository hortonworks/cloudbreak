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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
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

    protected static final String DOCS = "docs";

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustSetupValidationService.class);

    private static final String AD_DNS_VALIDATION_STATE = "trustsetup/validation/ad_dns_validation";

    private static final String AD_REVERSE_DNS_VALIDATION_STATE = "trustsetup/validation/ad_reverse_dns_validation";

    private static final String PACKAGE_VALIDATION_COMMENT = """
            Trust setup requires certain packages to be present on the image. You can check the package versions of an image in the image catalog. \
            Please upgrade to an appropriate image before proceeding with Hybrid Environment configuration.

            Required packages: %s""";

    private static final String DNS_VALIDATION_COMMENT = """
            The fully qualified domain name (FQDN) of the Key Distribution Center (KDC) must be resolvable to the corresponding KDC IP address by the provided \
            DNS server.

            Ensure that the DNS zone contains a valid A record mapping the KDC FQDN to the correct KDCP IP address. If this record is missing or incorrect, \
            Kerberos authentication requests will fail.

            This enables resources in either environment to communicate with each other.""";

    private static final String REVERSE_DNS_VALIDATION_COMMENT = """
            The IP address of the Key Distribution Center (KDC) must be resolvable to the fully qualified domain name (FQDN) of the corresponding KDC by the \
            provided DNS server.

            Ensure that the DNS zone contains a valid PTR (reverse lookup) record so that the KDC IP address resolves back to the same FQDN.

            This enables resources in either environment to communicate with each other.""";

    private static final String SECURITY_VALIDATION_COMMENT = """
            The base cluster selected as a Hybrid Environment Data Lake must be secured by Kerberos.
            Ensure that Kerberos authentication is enabled and fully configured on the on-premises Data Lake before proceeding with Hybrid Environment \
            configuration.

            This enables secure communication with the services, regardless of whether they reside in a public cloud or on-premises.""";

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
        taskResults.addTaskResult(validatePackageAvailability(stackId, crossRealmTrust));
        taskResults.addTaskResult(validateDns(stack, crossRealmTrust));
        taskResults.addTaskResult(validateReverseDns(stack, crossRealmTrust));
        taskResults.addTaskResult(validateKerberization(crossRealmTrust));
        return taskResults;
    }

    private TaskResult validatePackageAvailability(Long stackId, CrossRealmTrust crossRealmTrust) {
        LOGGER.info("Validate for IPA Server image trust packages");
        if (KdcType.ACTIVE_DIRECTORY.equals(crossRealmTrust.getKdcType()) && !packageAvailabilityChecker.isPackageAvailable(stackId)) {
            LOGGER.warn("Missing package [{}] required for AD trust setup", IPA_SERVER_TRUST_AD_PACKAGE);
            return new TaskResult(TaskResultType.ERROR, "Package validation failed",
                    Map.of(COMMENT, String.format(PACKAGE_VALIDATION_COMMENT, (IPA_SERVER_TRUST_AD_PACKAGE))));
        } else {
            return new TaskResult(TaskResultType.INFO, "Valid image trust packages", Map.of());
        }
    }

    private TaskResult validateDns(Stack stack, CrossRealmTrust crossRealmTrust) {
        return executeSaltState(stack, crossRealmTrust, AD_DNS_VALIDATION_STATE, "DNS validation",
                DNS_VALIDATION_COMMENT, DocumentationLinkProvider.hybridDnsArchitectureLink());
    }

    private TaskResult validateReverseDns(Stack stack, CrossRealmTrust crossRealmTrust) {
        return executeSaltState(stack, crossRealmTrust, AD_REVERSE_DNS_VALIDATION_STATE, "Reverse DNS validation",
                REVERSE_DNS_VALIDATION_COMMENT, DocumentationLinkProvider.hybridDnsArchitectureLink());
    }

    private TaskResult validateKerberization(CrossRealmTrust crossRealmTrust) {
        try {
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
                return kerberized ? new TaskResult(TaskResultType.INFO, "The on-premises cluster is kerberized", Map.of()) :
                        new TaskResult(TaskResultType.ERROR, "Security validation failed",
                                Map.of(COMMENT, SECURITY_VALIDATION_COMMENT,
                                        DOCS, DocumentationLinkProvider.hybridSecurityRequirements()));
            } else {
                return new TaskResult(TaskResultType.ERROR, "Security validation failed",
                        Map.of(COMMENT, "Remote environment CRN is missing.\nPlease contact Cloudera support."));
            }
        } catch (RuntimeException e) {
            LOGGER.error("An error occurred during the kerberization verification", e);
            return new TaskResult(TaskResultType.ERROR, "Security validation failed",
                    Map.of(COMMENT, "An error occurred during the kerberization verification: " + e.getMessage()));
        }
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
