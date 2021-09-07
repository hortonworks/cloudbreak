package com.sequenceiq.periscope.service.configuration;

import static com.sequenceiq.periscope.cache.LimitsConfigurationCache.LIMITS_CONFIGURATION_CACHE;
import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_CLUSTER_MAX_SIZE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;

@Service
public class LimitsConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LimitsConfigurationService.class);

    @Inject
    private CloudbreakCommunicator cbCommunicator;

    @Cacheable(cacheNames = LIMITS_CONFIGURATION_CACHE)
    public Integer getMaxNodeCountLimit() {
        Integer supportedMaxNodeCount = DEFAULT_CLUSTER_MAX_SIZE;
        try {
            supportedMaxNodeCount = cbCommunicator.getLimitsConfiguration().getMaxNodeCountLimit();
        } catch (Exception ex) {
            LOGGER.warn("Error retrieving CB Limits Configuration, Using default max cluster size '{}'", supportedMaxNodeCount, ex);
        }
        LOGGER.info("Using CB Supported Max Cluster size as '{}'", supportedMaxNodeCount);
        return supportedMaxNodeCount;
    }
}
