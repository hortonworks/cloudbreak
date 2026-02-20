package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getCmVersion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.inifile.IniFile;
import com.sequenceiq.cloudbreak.cmtemplate.inifile.IniFileFactory;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.GatewayView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class HueConfigProvider extends AbstractRdsRoleConfigProvider {

    public static final String HUE_SERVICE_SAFETY_VALVE = "hue_service_safety_valve";

    public static final String HUE_SERVER_HUE_SAFETY_VALVE = "hue_server_hue_safety_valve";

    private static final Logger LOGGER = LoggerFactory.getLogger(HueConfigProvider.class);

    private static final String KNOX_PROXYHOSTS = "knox_proxyhosts";

    private static final String SAFETY_VALVE_KNOX_PROXYHOSTS_KEY_PATTERN = "[desktop]\n[[knox]]\nknox_proxyhosts=";

    private static final String SAFETY_VALVE_DATABASE_KEY_PATTERN = "[desktop]\n[[database]]\noptions=";

    private static final String DATABASE_OPTIONS_FORMAT = "'{\"sslmode\": \"%s\", \"sslrootcert\": \"%s\"}'";

    @Inject
    private IniFileFactory iniFileFactory;

    @Override
    public String dbUserKey() {
        return "database_user";
    }

    @Override
    public String dbPasswordKey() {
        return "database_password";
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> result = new ArrayList<>();
        RdsView hueRdsView = getRdsView(source);
        result.add(config("database_host", hueRdsView.getHost()));
        result.add(config("database_port", hueRdsView.getPort()));
        result.add(config("database_name", hueRdsView.getDatabaseName()));
        result.add(config("database_type", hueRdsView.getSubprotocol()));
        result.add(config(dbUserKey(), hueRdsView.getConnectionUserName()));
        result.add(config(dbPasswordKey(), hueRdsView.getConnectionPassword()));

        IniFile safetyValve = iniFileFactory.create();
        configureKnoxProxyHostsServiceConfig(source, result, safetyValve);
        if (isDbSslNeeded(source, hueRdsView)) {
            LOGGER.info("Adding DB SSL options to {}", HUE_SERVICE_SAFETY_VALVE);
            String connectionURL = hueRdsView.getConnectionURL();
            String sslMode = connectionURL.contains("verify-ca") ? "verify-ca" : "verify-full";
            String dbSslConfig = SAFETY_VALVE_DATABASE_KEY_PATTERN.concat(String.format(DATABASE_OPTIONS_FORMAT, sslMode,
                    hueRdsView.getSslCertificateFilePath()));
            safetyValve.addContent(dbSslConfig);
        }
        String valveValue = safetyValve.print();
        if (!valveValue.isEmpty()) {
            LOGGER.info("Using {} settings of [{}]", HUE_SERVICE_SAFETY_VALVE, valveValue);
            result.add(config(HUE_SERVICE_SAFETY_VALVE, valveValue));
        }
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
    public DatabaseType dbType() {
        return DatabaseType.HUE;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        return List.of();
    }

    private boolean isDbSslNeeded(TemplatePreparationObject source, RdsView hueRdsView) {
        return isVersionNewerOrEqualThanLimited(getCmVersion(source), CLOUDERAMANAGER_VERSION_7_2_2) && hueRdsView.isUseSsl();
    }

    private void configureKnoxProxyHostsServiceConfig(TemplatePreparationObject source, List<ApiClusterTemplateConfig> result, IniFile safetyValve) {
        GatewayView gateway = source.getGatewayView();
        String cdhVersion = ConfigUtils.getCdhVersion(source);
        GeneralClusterConfigs generalClusterConfigs = source.getGeneralClusterConfigs();
        if (externalFqdnShouldBeConfigured(gateway, generalClusterConfigs)) {
            Set<String> proxyHosts = new LinkedHashSet<>();
            if (generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().isPresent()) {
                proxyHosts.add(generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().get());
                proxyHosts.addAll(generalClusterConfigs.getOtherGatewayInstancesDiscoveryFQDN());
            }
            if (StringUtils.isNotEmpty(generalClusterConfigs.getExternalFQDN())) {
                proxyHosts.add(generalClusterConfigs.getExternalFQDN());
            }
            if (generalClusterConfigs.getLoadBalancerGatewayFqdn().isPresent()) {
                String loadBalancerFqdn = generalClusterConfigs.getLoadBalancerGatewayFqdn().get();
                proxyHosts.add(loadBalancerFqdn);
                boolean containsUpperCase = loadBalancerFqdn.chars().anyMatch(Character::isUpperCase);
                if (containsUpperCase) {
                    proxyHosts.add(loadBalancerFqdn.toLowerCase(Locale.ROOT));
                }
            }
            if (!proxyHosts.isEmpty()) {
                String proxyHostsString = String.join(",", proxyHosts);
                // CDPD version 7.1.0 and above have a dedicated knox_proxyhosts property to set the knox proxy hosts.
                if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_0)) {
                    LOGGER.info("Using {} settings of [{}]", KNOX_PROXYHOSTS, proxyHostsString);
                    result.add(config(KNOX_PROXYHOSTS, proxyHostsString));
                } else {
                    LOGGER.info("Adding knox proxy hosts [{}] to {}", proxyHostsString, HUE_SERVICE_SAFETY_VALVE);
                    safetyValve.addContent(SAFETY_VALVE_KNOX_PROXYHOSTS_KEY_PATTERN.concat(proxyHostsString));
                }
            }
        }
    }

    private boolean externalFqdnShouldBeConfigured(GatewayView gateway, GeneralClusterConfigs generalClusterConfigs) {
        return gateway != null
                && (StringUtils.isNotEmpty(generalClusterConfigs.getExternalFQDN())
                && generalClusterConfigs.getPrimaryGatewayInstanceDiscoveryFQDN().isPresent()
                || generalClusterConfigs.getLoadBalancerGatewayFqdn().isPresent());
    }

}
