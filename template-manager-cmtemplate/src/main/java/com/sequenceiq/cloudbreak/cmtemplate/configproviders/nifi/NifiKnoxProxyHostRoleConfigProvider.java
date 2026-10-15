package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CFM_VERSION_4_12_0_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.cfm.CfmUtil.getCfmProduct;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;

/**
 * NiFi in CFM 4.12.0+ validates the X-Forwarded-Host / X-ProxyHost headers sent by Knox against nifi.web.proxy.host.
 * The CSD default (${KNOX_HOST}) only contains the Knox gateway FQDNs, so requests coming through the load balancer
 * or the cluster's public endpoint are rejected with HTTP 421. This provider lists every host a user may reach Knox on.
 */
@Component
public class NifiKnoxProxyHostRoleConfigProvider extends AbstractRoleConfigProvider {

    static final String NIFI_WEB_PROXY_HOST = "nifi.web.proxy.host";

    private static final Logger LOGGER = LoggerFactory.getLogger(NifiKnoxProxyHostRoleConfigProvider.class);

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        Set<String> proxyHosts = collectProxyHosts(source.getGeneralClusterConfigs());
        if (proxyHosts.isEmpty()) {
            LOGGER.info("No proxy hosts found, {} is not configured", NIFI_WEB_PROXY_HOST);
            return List.of();
        }
        String proxyHostsString = String.join(",", proxyHosts);
        LOGGER.info("{} = {} added to template", NIFI_WEB_PROXY_HOST, proxyHostsString);
        return List.of(config(NIFI_WEB_PROXY_HOST, proxyHostsString));
    }

    private Set<String> collectProxyHosts(GeneralClusterConfigs generalClusterConfigs) {
        Set<String> proxyHosts = new LinkedHashSet<>();
        if (generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().isPresent()) {
            addWithLowerCaseVariant(proxyHosts, generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().get());
            // not initialized e.g. during blueprint preview
            if (generalClusterConfigs.getOtherGatewayInstancesDiscoveryFQDN() != null) {
                generalClusterConfigs.getOtherGatewayInstancesDiscoveryFQDN().forEach(fqdn -> addWithLowerCaseVariant(proxyHosts, fqdn));
            }
        }
        if (StringUtils.isNotEmpty(generalClusterConfigs.getExternalFQDN())) {
            addWithLowerCaseVariant(proxyHosts, generalClusterConfigs.getExternalFQDN());
        }
        generalClusterConfigs.getLoadBalancerGatewayFqdn().ifPresent(fqdn -> addWithLowerCaseVariant(proxyHosts, fqdn));
        return proxyHosts;
    }

    // NiFi matches the proxy host header case-sensitively, while browsers send the host lower-cased
    private void addWithLowerCaseVariant(Set<String> proxyHosts, String fqdn) {
        proxyHosts.add(fqdn);
        proxyHosts.add(fqdn.toLowerCase(Locale.ROOT));
    }

    @Override
    public String getServiceType() {
        return "NIFI";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(NifiRoles.NIFI_NODE);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        Optional<ClouderaManagerProduct> cfm = getCfmProduct(source);
        return super.isConfigurationNeeded(cmTemplateProcessor, source)
                && source.getGatewayView() != null
                && cfm.isPresent()
                && isVersionNewerOrEqualThanLimited(cfm.get().getVersion(), CFM_VERSION_4_12_0_0);
    }
}
