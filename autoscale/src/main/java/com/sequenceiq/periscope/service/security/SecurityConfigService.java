package com.sequenceiq.periscope.service.security;

import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
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
    private CloudbreakCommunicator cloudbreakCommunicator;

    @PostConstruct
    public void init() {
        LOGGER.info("init SecurityConfigService");
    }

    @Cacheable(cacheNames = "securityConfigCache", key = "{ #clusterId }")
    public SecurityConfig getSecurityConfig(Long clusterId) {
        LOGGER.debug("Get SecurityConfig for clusterId: {}", clusterId);
        SecurityConfig securityConfig = securityConfigRepository.findByClusterId(clusterId);
        if (!isSecurityConfigAvailable(securityConfig)) {
            LOGGER.info("SecurityConfig not found by : {}", clusterId);
            if (securityConfig == null) {
                securityConfig = syncSecurityConfigForCluster(clusterId);
            } else {
                securityConfig = syncSecurityConfigForCluster(clusterId, securityConfig);
            }
        }
        return securityConfig;
    }

    private boolean isSecurityConfigAvailable(SecurityConfig securityConfig) {
        return securityConfig != null
                && StringUtils.isNotEmpty(securityConfig.getClientCert())
                && StringUtils.isNotEmpty(securityConfig.getClientKey());
    }

    public SecurityConfig syncSecurityConfigForCluster(Long clusterId, SecurityConfig securityConfig) {
        String stackCrn = clusterRepository.findStackCrnById(clusterId);
        SecurityConfig securityConfigFromCb = cloudbreakCommunicator.getRemoteSecurityConfig(stackCrn);
        Optional<Cluster> cluster = clusterRepository.findById(clusterId);
        LOGGER.info("fetched securityConfig from CB for cluster with ID : {}", clusterId);
        if (cluster.isPresent()) {
            if (securityConfigFromCb.getClientCert() != null) {
                securityConfig.setClientCert(securityConfigFromCb.getClientCert());
            }
            if (securityConfigFromCb.getClientKey() != null) {
                securityConfig.setClientKey(securityConfigFromCb.getClientKey());
            }
            if (securityConfigFromCb.getServerCert() != null) {
                securityConfig.setServerCert(securityConfigFromCb.getServerCert());
            }
            securityConfig.setCluster(cluster.get());
            securityConfigRepository.save(securityConfig);
            LOGGER.info("Updated securityConfig for cluster with ID : {}", clusterId);
        }
        return securityConfig;
    }

    public SecurityConfig syncSecurityConfigForCluster(Long clusterId) {
        String stackCrn = clusterRepository.findStackCrnById(clusterId);
        SecurityConfig securityConfig = cloudbreakCommunicator.getRemoteSecurityConfig(stackCrn);
        Optional<Cluster> cluster = clusterRepository.findById(clusterId);
        if (cluster.isPresent()) {
            securityConfig.setCluster(cluster.get());
            securityConfigRepository.save(securityConfig);
        }
        return securityConfig;
    }

    public void updateServerCertInSecurityConfig(Cluster cluster, String newServerCert) {
        SecurityConfig securityConfig = securityConfigRepository.findByClusterId(cluster.getId());
        if (securityConfig != null) {
            securityConfig.setServerCert(newServerCert);
            securityConfigRepository.save(securityConfig);
        } else {
            LOGGER.error("There is no security config for the cluster!");
        }
    }
}
