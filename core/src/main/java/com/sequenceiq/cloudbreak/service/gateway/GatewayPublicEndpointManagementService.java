package com.sequenceiq.cloudbreak.service.gateway;

import java.security.KeyPair;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class GatewayPublicEndpointManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayPublicEndpointManagementService.class);

    @Value("${gateway.cert.generation.enabled:false}")
    private boolean certGenerationEnabled;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private DnsManagementService dnsManagementService;

    @Inject
    private EnvironmentBasedDomainNameProvider environmentBasedDomainNameProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private CertificateCreationService certificateCreationService;

    @Inject
    private ClusterService clusterService;

    public boolean isCertGenerationEnabled() {
        return certGenerationEnabled;
    }

    public boolean generateCertAndSaveForStackAndUpdateDnsEntry(Stack stack) {
        boolean certGeneratedAndSaved = false;
        if (isCertGenerationEnabled()
                && stack != null
                && stack.getCluster() != null) {
            if (StringUtils.isEmpty(stack.getSecurityConfig().getUserFacingCert())) {
                certGeneratedAndSaved = generateCertAndSaveForStack(stack);
                if (certGeneratedAndSaved) {
                    updateDnsEntryForCluster(stack);
                }
            } else {
                LOGGER.info("CERT is already generated for stack, we don't generate a new one");
                updateDnsEntryForCluster(stack);
            }
        } else {
            LOGGER.info("Cert generation is disabled.");
        }
        return certGeneratedAndSaved;
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
        boolean success = dnsManagementService.createDnsEntryWithIp(userCrn, accountId, stack.getName(), environment.getName(), false, List.of(gatewayIp));
        if (success) {
            try {
                String fullQualifiedDomainName = environmentBasedDomainNameProvider
                        .getDomainName(stack.getName(), environment.getName(), getWorkloadSubdomain(userCrn));
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
        dnsManagementService.deleteDnsEntryWithIp(actorCrn, accountId, stack.getName(), environmentName, false, List.of(ip));
        return ip;
    }

    private boolean generateCertAndSaveForStack(Stack stack) {
        boolean success = false;
        LOGGER.info("Generate cert and save for stack");
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String accountId = threadBasedUserCrnProvider.getAccountId();
        KeyPair keyPair;
        SecurityConfig securityConfig = stack.getSecurityConfig();
        if (StringUtils.isEmpty(securityConfig.getUserFacingKey())) {
            keyPair = PkiUtil.generateKeypair();
            securityConfig.setUserFacingKey(PkiUtil.convert(keyPair.getPrivate()));
            securityConfig = securityConfigService.save(securityConfig);
        } else {
            keyPair = PkiUtil.fromPrivateKeyPem(securityConfig.getUserFacingKey());
            if (keyPair == null) {
                keyPair = PkiUtil.generateKeypair();
            }
        }
        try {
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            List<String> certs = certificateCreationService.create(userCrn, accountId, stack.getName(), environment.getName(), false, keyPair);
            securityConfig.setUserFacingCert(String.join("", certs));
            securityConfigService.save(securityConfig);
            success = true;
        } catch (Exception e) {
            LOGGER.info("The cert could not be generated by Public Endpoint Management service: " + e.getMessage(), e);
        }
        return success;
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

    private String getWorkloadSubdomain(String actorCrn) {
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
        UserManagementProto.Account account = grpcUmsClient.getAccountDetails(actorCrn, actorCrn, requestIdOptional);
        return account.getWorkloadSubdomain();
    }
}
