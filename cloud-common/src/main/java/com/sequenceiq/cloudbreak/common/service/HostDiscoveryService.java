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

    /*
     * Determines the cluster domain. If the 'cb.host.discovery.custom.domain' variable is not
     * defined it returns null. Null means we're going to use the cloud provider's default domain.
     */
    public String determineDomain(String domain, String subDomain, boolean useSubDomain) {
        String result = null;
        String domainName = getCustomDomainName(domain);
        if (StringUtils.isNoneBlank(domainName)) {
            String sub = getSubDomain(subDomain, useSubDomain);
            if (domainName.startsWith(".")) {
                result = sub + domainName;
            } else {
                result = sub + '.' + domainName;
            }
            LOGGER.info("Custom domain defined: {}", result);

        }
        return result;
    }

    private String getCustomDomainName(String domain) {
        if (StringUtils.isNoneBlank(domain)) {
            return domain;
        }
        if (StringUtils.isNoneBlank(customDomain)) {
            return customDomain;
        }
        return null;
    }

    private String getSubDomain(String subDomain, boolean useSubDomain) {
        String result = subDomain == null ? "" : subDomain;
        if (!useSubDomain) {
            result = "";
        }
        return result;
    }

    /*
     * It generates a hostname based on the instance group name and the node's private id.
     */
    public String generateHostname(String customHostname, String instanceGroupName, long privateId, boolean useInstanceGroupName) {
        if (StringUtils.isBlank(customHostname) && !useInstanceGroupName) {
            return "";
        }
        return getHostname(customHostname, instanceGroupName).replaceAll("_", "") + privateId;
    }

    private String getHostname(String customHostname, String instanceGroupName) {
        if (StringUtils.isNoneBlank(customHostname)) {
            return customHostname;
        }
        return instanceGroupName;
    }

}
