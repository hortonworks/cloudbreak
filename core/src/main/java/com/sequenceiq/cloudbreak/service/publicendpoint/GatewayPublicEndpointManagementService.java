package com.sequenceiq.cloudbreak.service.publicendpoint;

import java.security.KeyPair;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class GatewayPublicEndpointManagementService extends BasePublicEndpointManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayPublicEndpointManagementService.class);

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterService clusterService;

    public boolean generateCertAndSaveForStackAndUpdateDnsEntry(Stack stack) {
        boolean success = false;
        if (isCertGenerationEnabled()
                && stack != null
                && stack.getCluster() != null) {
            if (StringUtils.isEmpty(stack.getSecurityConfig().getUserFacingCert())) {
                success = generateCertAndSaveForStack(stack);
            }
            updateDnsEntryForCluster(stack);
        } else {
            LOGGER.info("External FQDN and valid certificate creation is disabled.");
        }
        return success;
    }

    public String updateDnsEntry(Stack stack, String gatewayIp) {
        LOGGER.info("Update dns entry");
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String accountId = threadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());

        if (StringUtils.isEmpty(gatewayIp)) {
            Optional<InstanceMetaData> gateway = Optional.ofNullable(stack.getPrimaryGatewayInstance());
            if (gateway.isEmpty()) {
                LOGGER.info("No running gateway or all node is terminated, we skip the dns entry deletion.");
                return null;
            } else {
                gatewayIp = gateway.get().getPublicIpWrapper();
            }
        }
        String endpointName = stack.getPrimaryGatewayInstance().getShortHostname();
        boolean success = getDnsManagementService().createDnsEntryWithIp(userCrn, accountId, endpointName, environment.getName(), false, List.of(gatewayIp));
        if (success) {
            try {
                String fullQualifiedDomainName = getDomainNameProvider()
                        .getFullyQualifiedEndpointName(endpointName, environment.getName(), getWorkloadSubdomain(userCrn));
                if (fullQualifiedDomainName != null) {
                    LOGGER.info("Dns entry updated: ip: {}, FQDN: {}", gatewayIp, fullQualifiedDomainName);
                    return fullQualifiedDomainName;
                }
            } catch (Exception e) {
                LOGGER.info("Cannot generate fqdn: {}", e.getMessage(), e);
            }
        }
        return null;
    }

    public String deleteDnsEntry(Stack stack, String environmentName) {
        String actorCrn = threadBasedUserCrnProvider.getUserCrn();
        String accountId = threadBasedUserCrnProvider.getAccountId();
        if (StringUtils.isEmpty(environmentName)) {
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            environmentName = environment.getName();
        }
        Optional<InstanceMetaData> gateway = Optional.ofNullable(stack.getPrimaryGatewayInstance());
        if (!gateway.isPresent()) {
            LOGGER.info("No running gateway or all node is terminated, we skip the dns entry deletion.");
            return null;
        }
        String ip = gateway.get().getPublicIpWrapper();
        if (ip == null) {
            return null;
        }
        getDnsManagementService().deleteDnsEntryWithIp(actorCrn, accountId, stack.getName(), environmentName, false, List.of(ip));
        return ip;
    }

    private boolean generateCertAndSaveForStack(Stack stack) {
        boolean result = false;
        LOGGER.info("Generate cert and save for stack");
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String accountId = threadBasedUserCrnProvider.getAccountId();
        SecurityConfig securityConfig = stack.getSecurityConfig();
        KeyPair keyPair = getKeyPairForStack(stack);
        try {
            String endpointName = stack.getPrimaryGatewayInstance().getShortHostname();
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            String environmentName = environment.getName();
            String workloadSubdomain = getWorkloadSubdomain(userCrn);
            String commonName = getDomainNameProvider().getCommonName(endpointName, environmentName, workloadSubdomain);
            String fullyQualifiedEndpointName = getDomainNameProvider().getFullyQualifiedEndpointName(endpointName, environmentName, workloadSubdomain);
            List<String> subjectAlternativeNames = List.of(commonName, fullyQualifiedEndpointName);
            LOGGER.info("Creating certificate with common name:{} and SANs: {}", commonName, String.join(",", subjectAlternativeNames));
            PKCS10CertificationRequest csr = PkiUtil.csr(keyPair, commonName, subjectAlternativeNames);
            List<String> certs = getCertificateCreationService().create(userCrn, accountId, endpointName, environmentName, csr);
            securityConfig.setUserFacingCert(String.join("", certs));
            securityConfigService.save(securityConfig);
            result = true;
        } catch (Exception e) {
            LOGGER.info("The cert could not be generated by Public Endpoint Management service: " + e.getMessage(), e);
        }
        return result;
    }

    private KeyPair getKeyPairForStack(Stack stack) {
        KeyPair keyPair;
        SecurityConfig securityConfig = stack.getSecurityConfig();
        if (StringUtils.isEmpty(securityConfig.getUserFacingKey())) {
            keyPair = PkiUtil.generateKeypair();
            securityConfig.setUserFacingKey(PkiUtil.convert(keyPair.getPrivate()));
            securityConfigService.save(securityConfig);
        } else {
            keyPair = PkiUtil.fromPrivateKeyPem(securityConfig.getUserFacingKey());
            if (keyPair == null) {
                keyPair = PkiUtil.generateKeypair();
            }
        }
        return keyPair;
    }

    private void updateDnsEntryForCluster(Stack stack) {
        String fqdn = updateDnsEntry(stack, null);
        if (fqdn != null) {
            Cluster cluster = stack.getCluster();
            cluster.setFqdn(fqdn);
            clusterService.save(cluster);
            LOGGER.info("The '{}' domain name has been generated, registered through PEM service and saved for the cluster.", fqdn);
        }
    }
}
