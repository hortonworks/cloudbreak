package com.sequenceiq.periscope.service.configuration;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.periscope.cache.ClusterProxyConfigurationCache;

@Service
public class ClusterProxyConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyConfigurationService.class);

    @Inject
    private CloudbreakInternalCrnClient internalCrnClient;

    @Cacheable(ClusterProxyConfigurationCache.CLUSTER_PROXY_CONFIGURATION_CACHE)
    public Optional<String> getClusterProxyUrl() {
        try {
            ClusterProxyConfiguration clusterProxyconfiguration = internalCrnClient.withInternalCrn().autoscaleEndpoint().getClusterProxyconfiguration();
            return clusterProxyconfiguration.isEnabled() ? Optional.ofNullable(clusterProxyconfiguration.getUrl()) : Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Couldn't retrieve Cluster Proxy configuration", e);
            return Optional.empty();
        }
    }
}
