package com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.cfm.CfmUtil.getCfmProduct;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiProxyHostsCollector.NIFI_WEB_PROXY_HOST;

import java.util.List;
import java.util.Set;

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
 * NiFi in CFM 4.12.0.10000+ validates the X-Forwarded-Host / X-ProxyHost headers sent by Knox against nifi.web.proxy.host.
 * The CSD default (${KNOX_HOST}) only contains a Knox gateway FQDN, so requests coming through the load balancer
 * or the cluster's public endpoint are rejected with HTTP 421. This provider lists every host a user may reach Knox on.
 * The property is exposed as a NIFI_NODE role config by the CSD only from CFM 4.12.0.10000 (CFM-6907).
 */
@Component
public class NifiKnoxProxyHostRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NifiKnoxProxyHostRoleConfigProvider.class);

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        GeneralClusterConfigs generalClusterConfigs = source.getGeneralClusterConfigs();
        Set<String> proxyHosts = NifiProxyHostsCollector.collect(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN(),
                generalClusterConfigs.getOtherGatewayInstancesDiscoveryFQDN(), generalClusterConfigs.getExternalFQDN(),
                generalClusterConfigs.getLoadBalancerGatewayFqdn());
        if (proxyHosts.isEmpty()) {
            LOGGER.info("No proxy hosts found, {} is not configured", NIFI_WEB_PROXY_HOST);
            return List.of();
        }
        String proxyHostsString = String.join(",", proxyHosts);
        LOGGER.info("{} = {} added to template", NIFI_WEB_PROXY_HOST, proxyHostsString);
        return List.of(config(NIFI_WEB_PROXY_HOST, proxyHostsString));
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
        return super.isConfigurationNeeded(cmTemplateProcessor, source)
                && source.getGatewayView() != null
                && getCfmProduct(source).map(ClouderaManagerProduct::getVersion).filter(NifiProxyHostsCollector::isProxyHostConfigRequired).isPresent();
    }
}
