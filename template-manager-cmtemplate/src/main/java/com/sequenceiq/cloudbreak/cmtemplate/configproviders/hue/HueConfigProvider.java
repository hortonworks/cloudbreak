package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class HueConfigProvider extends AbstractRdsRoleConfigProvider {

    private static final String HUE_DATABASE_HOST = "hue-hue_database_host";

    private static final String HUE_DATABASE_PORT = "hue-hue_database_port";

    private static final String HUE_DATABASE_NAME = "hue-hue_database_name";

    private static final String HUE_HUE_DATABASE_TYPE = "hue-hue_database_type";

    private static final String HUE_HUE_DATABASE_USER = "hue-hue_database_user";

    private static final String HUE_DATABASE_PASSWORD = "hue-hue_database_password";

    private static final String HUE_SAFETY_VALVE = "hue-hue_service_safety_valve";

    private static final String HUE_KNOX_PROXYHOSTS = "hue-knox_proxyhosts";

    private static final String KNOX_PROXYHOSTS = "knox_proxyhosts";

    private static final String SAFETY_VALVE_KNOX_PROXYHOSTS_KEY_PATTERN = "[desktop]\n[[knox]]\nknox_proxyhosts=";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        result.add(new ApiClusterTemplateConfig().name("database_host").variable(HUE_DATABASE_HOST));
        result.add(new ApiClusterTemplateConfig().name("database_port").variable(HUE_DATABASE_PORT));
        result.add(new ApiClusterTemplateConfig().name("database_name").variable(HUE_DATABASE_NAME));
        result.add(new ApiClusterTemplateConfig().name("database_type").variable(HUE_HUE_DATABASE_TYPE));
        result.add(new ApiClusterTemplateConfig().name("database_user").variable(HUE_HUE_DATABASE_USER));
        result.add(new ApiClusterTemplateConfig().name("database_password").variable(HUE_DATABASE_PASSWORD));
        configureKnoxProxyHostsServiceConfig(source, result);
        return result;
    }

    @Override
    public List<ApiClusterTemplateVariable> getServiceConfigVariables(TemplatePreparationObject source) {
        List<ApiClusterTemplateVariable> result = new ArrayList<>();
        RdsView hueRdsView = getRdsView(source);
        result.add(new ApiClusterTemplateVariable().name(HUE_DATABASE_HOST).value(hueRdsView.getHost()));
        result.add(new ApiClusterTemplateVariable().name(HUE_DATABASE_PORT).value(hueRdsView.getPort()));
        result.add(new ApiClusterTemplateVariable().name(HUE_DATABASE_NAME).value(hueRdsView.getDatabaseName()));
        result.add(new ApiClusterTemplateVariable().name(HUE_HUE_DATABASE_TYPE).value(hueRdsView.getSubprotocol()));
        result.add(new ApiClusterTemplateVariable().name(HUE_HUE_DATABASE_USER).value(hueRdsView.getConnectionUserName()));
        result.add(new ApiClusterTemplateVariable().name(HUE_DATABASE_PASSWORD).value(hueRdsView.getConnectionPassword()));
        configureKnoxProxyHostsConfigVariables(source, result);
        return result;
    }

    @Override
    public String getServiceType() {
        return "HUE";
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HueRoles.HUE_SERVER, HueRoles.HUE_LOAD_BALANCER);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.HUE;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return List.of();
    }

    private void configureKnoxProxyHostsServiceConfig(TemplatePreparationObject source, List<ApiClusterTemplateConfig> result) {
        GatewayView gateway = source.getGatewayView();
        String cdhVersion = getCdhVersionString(source);
        GeneralClusterConfigs generalClusterConfigs = source.getGeneralClusterConfigs();
        if (externalFQDNShouldConfigured(gateway, generalClusterConfigs)) {
            // CDPD version 7.1.0 and above have a dedicated knox_proxyhosts property to set the knox proxy hosts.
            if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_0)) {
                result.add(new ApiClusterTemplateConfig().name(KNOX_PROXYHOSTS).variable(HUE_KNOX_PROXYHOSTS));
            } else {
                result.add(new ApiClusterTemplateConfig().name("hue_service_safety_valve").variable(HUE_SAFETY_VALVE));
            }
        }
    }

    private void configureKnoxProxyHostsConfigVariables(TemplatePreparationObject source, List<ApiClusterTemplateVariable> result) {
        GatewayView gateway = source.getGatewayView();
        String cdhVersion = getCdhVersionString(source);
        GeneralClusterConfigs generalClusterConfigs = source.getGeneralClusterConfigs();
        if (externalFQDNShouldConfigured(gateway, generalClusterConfigs)) {
            Set<String> proxyHosts = new HashSet<>();
            if (generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().isPresent()) {
                proxyHosts.add(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().get());
            }
            if (StringUtils.isNotEmpty(generalClusterConfigs.getExternalFQDN())) {
                proxyHosts.add(generalClusterConfigs.getExternalFQDN());
            }
            if (generalClusterConfigs.getLoadBalancerGatewayFqdn().isPresent()) {
                proxyHosts.add(generalClusterConfigs.getLoadBalancerGatewayFqdn().get());
            }
            if (!proxyHosts.isEmpty()) {
                String proxyHostsString = String.join(",", proxyHosts);
                if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_0)) {
                    result.add(new ApiClusterTemplateVariable().name(HUE_KNOX_PROXYHOSTS).value(proxyHostsString));
                } else {
                    String valveValue = SAFETY_VALVE_KNOX_PROXYHOSTS_KEY_PATTERN.concat(proxyHostsString);
                    result.add(new ApiClusterTemplateVariable().name(HUE_SAFETY_VALVE).value(valveValue));
                }
            }
        }
    }

    private String getCdhVersionString(TemplatePreparationObject source) {
        return source.getBlueprintView().getProcessor().getVersion().orElse("");
    }

    private boolean externalFQDNShouldConfigured(GatewayView gateway, GeneralClusterConfigs generalClusterConfigs) {
        return gateway != null
                && ((StringUtils.isNotEmpty(generalClusterConfigs.getExternalFQDN())
                && generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().isPresent())
                || generalClusterConfigs.getLoadBalancerGatewayFqdn().isPresent());
    }
}
