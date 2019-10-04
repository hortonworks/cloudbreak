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
        if (StringUtils.isNoneBlank(domain)) {
            result = domain;
        }
        if (StringUtils.isNoneBlank(customDomain)) {
            result = customDomain;
        }
        return result;
    }

    /*
     * It generates a hostname based on the instance group name and the node's private id.
     */
    public String generateHostname(String customHostnamePrefix, String instanceGroupName, long privateId, boolean useInstanceGroupName) {
        if (StringUtils.isBlank(customHostnamePrefix) && !useInstanceGroupName) {
            return "";
        }
        return getHostname(customHostnamePrefix, instanceGroupName).replaceAll("_", "") + privateId;
    }

    private String getHostname(String customHostnamePrefix, String instanceGroupName) {
        String hostname;
        if (StringUtils.isNoneBlank(customHostnamePrefix)) {
            if (StringUtils.isNoneBlank(instanceGroupName)) {
                hostname = customHostnamePrefix + "-" + instanceGroupName;
            } else {
                hostname = customHostnamePrefix;
            }
        } else {
            hostname = instanceGroupName;
        }
        return hostname;
    }

}
