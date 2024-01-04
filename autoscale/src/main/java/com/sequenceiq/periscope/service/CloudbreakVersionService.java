package com.sequenceiq.periscope.service;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.cache.CloudbreakVersionCache;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;

@Service
public class CloudbreakVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakVersionService.class);

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Cacheable(cacheNames = CloudbreakVersionCache.CB_VERSION_CACHE, key = "{ #stackCrn }")
    public String getCloudbreakSaltStateVersionByStackCrn(String stackCrn) {
        LOGGER.info("Fetching cloudbreak version info");
        return cloudbreakCommunicator.getAutoscaleClusterByCrn(stackCrn).getSaltCbVersion();
    }

}
