package com.sequenceiq.cloudbreak.service.cluster;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.OrchestratorExceptionAnalyzer;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.common.api.type.CertExpirationState;

@Service
public class CertificateExpirationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateExpirationService.class);

    private static final Pattern CERT_DAYS_EXPIRED_PATTERN = Pattern.compile("^.*Certificate of Cloudera Manager Agent will expire within 0 days.*$");

    private static final String CERT_CHECK_ERROR = "Certificate check error";

    private static final String CERT_CHECK_EXPIRED = "Certificate is expired";

    private static final List<String> CERT_CHECK_STATE = List.of("cloudera.manager.rotate.host-cert-check-expiration");

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private SecretRotationSaltService saltService;

    public boolean isAnyCertExpiredOnHosts(StackDto stackDto) throws CloudbreakOrchestratorFailedException {
        Set<String> targets = stackDto.getAllFunctioningNodes().stream()
                .map(Node::getHostname)
                .collect(Collectors.toSet());
        try {
            saltService.executeSaltState(stackDto, targets, CERT_CHECK_STATE);
            return false;
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.info("Host certificate expiration check failed: {}", e.getMessage());
            List<String> certCheckErrors = OrchestratorExceptionAnalyzer.getHostSaltCommands(e).stream()
                    .flatMap(hostCmd -> hostCmd.saltCommands().stream()
                            .map(command -> hostCmd.host() + ": " + command.params().get(OrchestratorExceptionAnalyzer.STDERR)))
                    .filter(stderr -> Strings.CS.contains(stderr, CERT_CHECK_ERROR))
                    .toList();
            if (certCheckErrors.isEmpty()) {
                LOGGER.error("Host certificate expiration check failed", e);
                throw e;
            } else {
                LOGGER.info("Error during host certificate expiration check: {}", certCheckErrors);
                return certCheckErrors.stream().allMatch(certCheckError -> Strings.CS.contains(certCheckError, CERT_CHECK_EXPIRED));
            }
        }
    }

    public boolean validateCertificateFullyExpired(StackDto stackDto) throws CloudbreakOrchestratorFailedException {
        if (isCertFullyExpired(stackDto.getCluster())) {
            LOGGER.info("Host certificates are expired on cluster {}", stackDto.getName());
            return true;
        } else if (hasUnhealthyHosts(stackDto)) {
            if (isAnyCertExpiredOnHosts(stackDto)) {
                LOGGER.info("Cluster {} has unhealthy hosts with expired certificates",  stackDto.getName());
                return true;
            } else {
                LOGGER.warn("Cluster {} has unhealthy hosts but host certificates are not expired, skip the rotation",  stackDto.getName());
                throw new SecretRotationException("Cert rotation is not possible if there are unhealthy hosts and the certs are not expired");
            }
        } else {
            return false;
        }
    }

    public boolean isCertFullyExpired(ClusterView clusterView) {
        if (CertExpirationState.HOST_CERT_EXPIRING.equals(clusterView.getCertExpirationState())
                && StringUtils.isNotBlank(clusterView.getCertExpirationDetails())) {
            return CERT_DAYS_EXPIRED_PATTERN.matcher(clusterView.getCertExpirationDetails()).matches();
        } else {
            return false;
        }
    }

    public boolean hasUnhealthyHosts(StackDto stackDto) {
        Optional<String> runtimeVersion = runtimeVersionService.getRuntimeVersion(stackDto.getCluster().getId());
        ExtendedHostStatuses extendedHostStatuses = apiConnectors.getConnector(stackDto).clusterStatusService().getExtendedHostStatuses(runtimeVersion);
        return extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.HOST) ||
                extendedHostStatuses.isAnyUnhealthyWithType(HealthCheckType.CERTIFICATE);
    }
}
