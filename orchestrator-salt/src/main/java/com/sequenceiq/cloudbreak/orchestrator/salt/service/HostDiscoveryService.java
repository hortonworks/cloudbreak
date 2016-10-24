package com.sequenceiq.cloudbreak.orchestrator.salt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HostDiscoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostDiscoveryService.class);

    @Value("${cb.host.discovery.custom.domain:}")
    private String customDomain;

    public String determineDomain() {
        String domainName = null;
        if (!StringUtils.isEmpty(customDomain)) {
            // this is just for convenience
            if (customDomain.startsWith(".")) {
                domainName = customDomain;
            } else {
                domainName = "." + customDomain;
            }
            LOGGER.info("Custom domain defined: {}", domainName);

        }
        return domainName;
    }
}
