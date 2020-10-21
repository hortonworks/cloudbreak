package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getCmVersion;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class HiveMetastoreConfigProvider extends AbstractRdsRoleConfigProvider {

    @VisibleForTesting
    static final String HIVE_METASTORE_DATABASE_HOST = "hive_metastore_database_host";

    @VisibleForTesting
    static final String HIVE_METASTORE_DATABASE_NAME = "hive_metastore_database_name";

    @VisibleForTesting
    static final String HIVE_METASTORE_DATABASE_PASSWORD = "hive_metastore_database_password";

    @VisibleForTesting
    static final String HIVE_METASTORE_DATABASE_PORT = "hive_metastore_database_port";

    @VisibleForTesting
    static final String HIVE_METASTORE_DATABASE_TYPE = "hive_metastore_database_type";

    @VisibleForTesting
    static final String HIVE_METASTORE_DATABASE_USER = "hive_metastore_database_user";

    @VisibleForTesting
    static final String HIVE_SERVICE_CONFIG_SAFETY_VALVE = "hive_service_config_safety_valve";

    @VisibleForTesting
    static final String HIVE_METASTORE_ENABLE_LDAP_AUTH = "hive_metastore_enable_ldap_auth";

    @VisibleForTesting
    static final String HIVE_METASTORE_LDAP_URI = "hive_metastore_ldap_uri";

    @VisibleForTesting
    static final String HIVE_METASTORE_LDAP_BASEDN = "hive_metastore_ldap_basedn";

    @VisibleForTesting
    static final String METASTORE_CANARY_HEALTH_ENABLED = "metastore_canary_health_enabled";

    @VisibleForTesting
    static final String JDBC_URL_OVERRIDE = "jdbc_url_override";

    private static final Set<String> HIVE_METASTORE_DATABASE_CONFIG_KEYS = Set.of(HIVE_METASTORE_DATABASE_HOST, HIVE_METASTORE_DATABASE_NAME,
            HIVE_METASTORE_DATABASE_PASSWORD, HIVE_METASTORE_DATABASE_PORT, HIVE_METASTORE_DATABASE_TYPE, HIVE_METASTORE_DATABASE_USER,
            JDBC_URL_OVERRIDE);

    private static final Logger LOGGER = LoggerFactory.getLogger(HiveMetastoreConfigProvider.class);

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        RdsView hiveRdsView = getRdsView(source);

        List<ApiClusterTemplateConfig> configs = Lists.newArrayList(
                config(HIVE_METASTORE_DATABASE_HOST, hiveRdsView.getHost()),
                config(HIVE_METASTORE_DATABASE_NAME, hiveRdsView.getDatabaseName()),
                config(HIVE_METASTORE_DATABASE_PASSWORD, hiveRdsView.getConnectionPassword()),
                config(HIVE_METASTORE_DATABASE_PORT, hiveRdsView.getPort()),
                config(HIVE_METASTORE_DATABASE_TYPE, hiveRdsView.getSubprotocol()),
                config(HIVE_METASTORE_DATABASE_USER, hiveRdsView.getConnectionUserName())
        );
        addDbSslConfigsIfNeeded(templateProcessor, hiveRdsView, configs, getCmVersion(source));

        Optional<KerberosConfig> kerberosConfigOpt = source.getKerberosConfig();
        if (kerberosConfigOpt.isPresent()) {
            String safetyValveValue = getSafetyValveProperty("hive.hook.proto.file.per.event", Boolean.TRUE.toString());
            configs.add(config(HIVE_SERVICE_CONFIG_SAFETY_VALVE, safetyValveValue));
        }

        if (source.getStackType() == StackType.DATALAKE) {
            source.getLdapConfig().ifPresent(ldap -> {
                configs.add(config(HIVE_METASTORE_ENABLE_LDAP_AUTH, Boolean.TRUE.toString()));
                configs.add(config(HIVE_METASTORE_LDAP_URI, ldap.getConnectionURL()));
                configs.add(config(HIVE_METASTORE_LDAP_BASEDN, ldap.getUserSearchBase()));
            });
        }
        return configs;
    }

    @Override
    public String getServiceType() {
        return HiveRoles.HIVE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HiveRoles.HIVEMETASTORE);
    }

    @Override
    protected DatabaseType dbType() {
        return DatabaseType.HIVE;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        return List.of(
                config(METASTORE_CANARY_HEALTH_ENABLED, Boolean.FALSE.toString())
        );
    }

    private List<String> getExistingHiveServiceConfigKeys(CmTemplateProcessor templateProcessor) {
        return templateProcessor
                .getServiceByType(getServiceType())
                .map(ApiClusterTemplateService::getServiceConfigs)
                .map(this::extractValidConfigKeys)
                .orElse(List.of());
    }

    private List<String> extractValidConfigKeys(List<ApiClusterTemplateConfig> configs) {
        return configs.stream()
                .filter(config -> config.getValue() != null || config.getVariable() != null)
                .map(ApiClusterTemplateConfig::getName)
                .collect(Collectors.toList());
    }

    private void addDbSslConfigsIfNeeded(CmTemplateProcessor templateProcessor, RdsView hiveRdsView, List<ApiClusterTemplateConfig> configList,
            String cmVersion) {
        if (hiveRdsView.isUseSsl()) {
            if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_2)) {
                ArrayList<String> overriddenDbConfigKeys = new ArrayList<>(getExistingHiveServiceConfigKeys(templateProcessor));
                overriddenDbConfigKeys.retainAll(HIVE_METASTORE_DATABASE_CONFIG_KEYS);
                if (overriddenDbConfigKeys.isEmpty()) {
                    LOGGER.info("Injecting Hive Metastore DB SSL configs because the supplied DB requires SSL " +
                            "and DB settings are not overridden in the cluster template.");
                    configList.add(config(JDBC_URL_OVERRIDE, hiveRdsView.getConnectionURL()));
                } else {
                    // This is currently possible for Data Hub clusters using a custom cluster template
                    LOGGER.info("The supplied Hive Metastore DB would require SSL, but the following config keys are present in the cluster template: {}. " +
                            "Skipping Hive Metastore DB SSL configs injection in favor of cluster template settings.", overriddenDbConfigKeys);
                }
            } else {
                LOGGER.warn("The supplied Hive Metastore DB would require SSL, but this setting is not supported for the CM version {} used here.", cmVersion);
            }
        }
    }

}
