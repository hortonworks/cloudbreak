package com.sequenceiq.periscope.service.security;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;

@Service
public class SecurityConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfigService.class);

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private CloudbreakInternalCrnClient internalCrnClient;

    @PostConstruct
    public void init() {
        LOGGER.info("init SecurityConfigService");
    }

    @Cacheable(cacheNames = "securityConfigCache", key = "{ #clusterId }")
    public SecurityConfig getSecurityConfig(Long clusterId) {
        LOGGER.info("Get SecurityConfig for clusterId: {}", clusterId);
        SecurityConfig securityConfig = securityConfigRepository.findByClusterId(clusterId);
        if (securityConfig == null) {
            LOGGER.info("SecurityConfig not found by : {}", clusterId);
            String stackCrn = clusterRepository.findStackCrnById(clusterId);
            securityConfig = getRemoteSecurityConfig(stackCrn);
            Optional<Cluster> cluster = clusterRepository.findById(clusterId);
            if (cluster.isPresent()) {
                securityConfig.setCluster(cluster.get());
                securityConfigRepository.save(securityConfig);
            }
        }
        return securityConfig;
    }

    private SecurityConfig getRemoteSecurityConfig(String stackCrn) {
        LOGGER.info("Looks like that SecurityConfig is not in database, calling Cloudbreak: {}", stackCrn);
        CertificateV4Response response = internalCrnClient.withInternalCrn().autoscaleEndpoint().getCertificate(stackCrn);
        LOGGER.info("We got a certificate back from Cloudbreak: {}", stackCrn);
        return new SecurityConfig(response.getClientKeyPath(), response.getClientCertPath(), response.getServerCert());
    }
}
