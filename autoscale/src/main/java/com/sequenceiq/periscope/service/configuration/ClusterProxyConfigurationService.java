package com.sequenceiq.periscope.service.configuration;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration;
import com.sequenceiq.periscope.cache.ClusterProxyConfigurationCache;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;

@Service
public class ClusterProxyConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyConfigurationService.class);

    @Inject
    private CloudbreakCommunicator cbCommunicator;

    @Cacheable(cacheNames = ClusterProxyConfigurationCache.CLUSTER_PROXY_CONFIGURATION_CACHE, unless = "#result == null")
    public Optional<String> getClusterProxyUrl() {
        ClusterProxyConfiguration clusterProxyconfiguration = cbCommunicator.getClusterProxyconfiguration();
        return clusterProxyconfiguration.isEnabled() ? Optional.ofNullable(clusterProxyconfiguration.getUrl()) : Optional.empty();
    }
}
