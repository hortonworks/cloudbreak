package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CFM_VERSION_4_12_0_10000;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Collects the hosts NiFi has to accept in the X-Forwarded-Host / X-ProxyHost headers sent by Knox (nifi.web.proxy.host).
 * NiFi of CFM 4.12.0.10000+ rejects requests with HTTP 421 when the header value is not listed, and the CSD default
 * (${KNOX_HOST}) does not contain the load balancer or the public endpoint of the cluster.
 */
public final class NifiProxyHostsCollector {

    public static final String NIFI_WEB_PROXY_HOST = "nifi.web.proxy.host";

    private NifiProxyHostsCollector() {
    }

    public static boolean isProxyHostConfigRequired(String cfmVersion) {
        // the build suffix is stripped, because a suffixed version (e.g. 4.12.0.10000-238) is considered older than the bare one
        return StringUtils.isNotEmpty(cfmVersion) && isVersionNewerOrEqualThanLimited(cfmVersion.split("-")[0], CFM_VERSION_4_12_0_10000);
    }

    public static Set<String> collect(Optional<String> primaryGatewayFqdn, Collection<String> otherGatewayFqdns, String externalFqdn,
            Optional<String> loadBalancerFqdn) {
        Set<String> proxyHosts = new LinkedHashSet<>();
        if (primaryGatewayFqdn.isPresent()) {
            addWithLowerCaseVariant(proxyHosts, primaryGatewayFqdn.get());
            // not initialized e.g. during blueprint preview
            if (otherGatewayFqdns != null) {
                otherGatewayFqdns.stream().filter(StringUtils::isNotEmpty).forEach(fqdn -> addWithLowerCaseVariant(proxyHosts, fqdn));
            }
        }
        if (StringUtils.isNotEmpty(externalFqdn)) {
            addWithLowerCaseVariant(proxyHosts, externalFqdn);
        }
        loadBalancerFqdn.filter(StringUtils::isNotEmpty).ifPresent(fqdn -> addWithLowerCaseVariant(proxyHosts, fqdn));
        return proxyHosts;
    }

    // NiFi matches the proxy host header case-sensitively, while browsers send the host lower-cased
    private static void addWithLowerCaseVariant(Set<String> proxyHosts, String fqdn) {
        proxyHosts.add(fqdn);
        proxyHosts.add(fqdn.toLowerCase(Locale.ROOT));
    }
}
