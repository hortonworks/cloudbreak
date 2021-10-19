package com.sequenceiq.periscope.service.security;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
        if (securityConfig == null) {
            LOGGER.info("SecurityConfig not found by : {}", clusterId);
            securityConfig = syncSecurityConfigForCluster(clusterId);
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
}
