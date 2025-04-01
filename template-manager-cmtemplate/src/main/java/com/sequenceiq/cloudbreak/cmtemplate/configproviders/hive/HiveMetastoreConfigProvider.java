package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getCmVersion;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_HOST;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_NAME;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PORT;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_USER;
import static com.sequenceiq.cloudbreak.template.views.DatabaseType.EXTERNAL_DATABASE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    static final String HIVE_COMPACTOR_INITIATOR_ON = "hive_compactor_initiator_on";

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

    @Value("${cb.externaldatabase.gcp.sslmode:verify-ca}")
    private String gcpExternalDatabaseSslVerificationMode;

    @Override
    public String dbUserKey() {
        return HIVE_METASTORE_DATABASE_USER;
    }

    @Override
    public String dbPasswordKey() {
        return HIVE_METASTORE_DATABASE_PASSWORD;
    }

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
        String cmVersion = getCmVersion(source);
        addDbSslConfigsIfNeeded(templateProcessor, hiveRdsView, configs, cmVersion, source);

        // For DataHub, don't start the compactor.Initiator thread which automatically queues ACID compaction.
        // The Initiator thread in the DataLake's HMS will take care of this.
        if (source.getStackType() == StackType.WORKLOAD && isVersionNewerOrEqualThanLimited(cmVersion,
            CLOUDERAMANAGER_VERSION_7_1_1)) {
            configs.add(config(HIVE_COMPACTOR_INITIATOR_ON, Boolean.FALSE.toString()));
        }

        Optional<KerberosConfig> kerberosConfigOpt = source.getKerberosConfig();
        StringBuilder safetyValveValue = new StringBuilder();
        if (kerberosConfigOpt.isPresent()) {
            safetyValveValue.append(getSafetyValveProperty("hive.hook.proto.file.per.event", Boolean.TRUE.toString()));
        }
        if (source.getStackType() != null && source.getStackType().equals(DATALAKE)) {
            safetyValveValue.append(getSafetyValveProperty("hive.metastore.try.direct.sql.ddl", Boolean.TRUE.toString()));
            safetyValveValue.append(getSafetyValveProperty("hive.metastore.try.direct.sql", Boolean.TRUE.toString()));
        }
        configs.add(config(HIVE_SERVICE_CONFIG_SAFETY_VALVE, safetyValveValue.toString()));

        if (source.getStackType() == DATALAKE) {
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
    public DatabaseType dbType() {
        return DatabaseType.HIVE;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
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
            String cmVersion, TemplatePreparationObject source) {
        if (hiveRdsView.isUseSsl()) {
            if (isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_2)) {
                ArrayList<String> overriddenDbConfigKeys = new ArrayList<>(getExistingHiveServiceConfigKeys(templateProcessor));
                overriddenDbConfigKeys.retainAll(HIVE_METASTORE_DATABASE_CONFIG_KEYS);
                if (overriddenDbConfigKeys.isEmpty()) {
                    LOGGER.info("Injecting Hive Metastore DB SSL configs because the supplied DB requires SSL " +
                            "and DB settings are not overridden in the cluster template.");
                    populateSslMode(hiveRdsView, configList, source);
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

    private void populateSslMode(RdsView hiveRdsView, List<ApiClusterTemplateConfig> configList, TemplatePreparationObject source) {
        if (null != source.getDatalakeView() && source.getDatalakeView().isPresent() && source.getCloudPlatform().equalsIgnoreCase("GCP")) {
            com.sequenceiq.cloudbreak.template.views.DatabaseType databaseType = source.getDatalakeView().get().getDatabaseType();
            String connectionUrl;
            if (databaseType.equals(EXTERNAL_DATABASE)) {
                connectionUrl = hiveRdsView.getConnectionURL().replace("verify-full", gcpExternalDatabaseSslVerificationMode);
                configList.add(config(JDBC_URL_OVERRIDE, connectionUrl));
            } else if (!source.getStackType().equals(DATALAKE)) {
                connectionUrl = hiveRdsView.getConnectionURL().replace(gcpExternalDatabaseSslVerificationMode, "verify-full");
                configList.add(config(JDBC_URL_OVERRIDE, connectionUrl));
            } else {
                configList.add(config(JDBC_URL_OVERRIDE, hiveRdsView.getConnectionURL()));
            }
        } else {
            configList.add(config(JDBC_URL_OVERRIDE, hiveRdsView.getConnectionURL()));
        }
    }

}
