package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;

@Component
public class PreFlightCheckValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreFlightCheckValidationService.class);

    @Inject
    private EnvironmentService environmentClientService;

    public boolean preFlightCheckSupported(String environmentCrn, boolean proxySupported) {
        boolean proxyConfigUsed = proxyConfigUsed(environmentCrn);
        LOGGER.debug("Checking if pre-flight check is supported, proxy config used : {}, proxy supported: {}", proxyConfigUsed, proxySupported);
        return !proxyConfigUsed || proxySupported;
    }

    private boolean proxyConfigUsed(String environmentCrn) {
        return environmentClientService.getByCrn(environmentCrn).getProxyConfig() != null;
    }
}
