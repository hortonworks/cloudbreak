package com.sequenceiq.cloudbreak.common.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HostDiscoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostDiscoveryService.class);

    @Value("${cb.host.discovery.custom.domain:}")
    private String customDomain;

    @Value("${cb.host.discovery.custom.hostname.enabled:}")
    private Boolean enabledCustomHostNames;

    /*
     * Determines the cluster domain. If the 'cb.host.discovery.custom.domain' variable is not
     * defined it returns null. Null means we're going to use the cloud provider's default domain.
     * If 'cb.host.discovery.custom.hostname.enabled' is set to true the domain will have a sub-domain as well
     * to be able to connect multiple clusters by hostname.
     */
    public String determineDomain(String subDomain) {
        subDomain = subDomain == null ? "" : subDomain;
        if (enabledCustomHostNames == null || !enabledCustomHostNames) {
            subDomain = "";
        }
        String domainName = null;
        if (StringUtils.isNoneBlank(customDomain)) {
            if (customDomain.startsWith(".")) {
                domainName = subDomain + customDomain;
            } else {
                domainName = subDomain + "." + customDomain;
            }
            LOGGER.info("Custom domain defined: {}", domainName);

        }
        return domainName;
    }

    /*
     * It generates a hostname based on the instance group name and the node's private id
     * if the 'cb.host.discovery.custom.hostname.enabled' is set to true. Otherwise returns empty string.
     */
    public String generateHostname(String instanceGroupName, long privateId) {
        if (enabledCustomHostNames == null || !enabledCustomHostNames) {
            return "";
        }
        return instanceGroupName.replaceAll("_", "") + privateId;
    }
}
