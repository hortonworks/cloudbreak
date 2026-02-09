package com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.StreamingAppRdsRoleConfigProviderUtil.dataBaseTypeForCM;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.utils.KerberosAuthToLocalUtils;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class StreamsMessagingManagerServiceConfigProvider extends AbstractRdsRoleConfigProvider {

    public static final String DATABASE_TYPE = "smm_database_type";

    public static final String DATABASE_NAME = "smm_database_name";

    public static final String DATABASE_HOST = "smm_database_host";

    public static final String DATABASE_PORT = "smm_database_port";

    public static final String DATABASE_USER = "smm_database_user";

    public static final String DATABASE_PASSWORD = "smm_database_password";

    public static final String DATABASE_JDBC_URL_OVERRIDE = "database_jdbc_url_override";

    public static final String KERBEROS_NAME_RULES = "streams.messaging.manager.kerberos.name.rules";

    @Inject
    private KerberosAuthToLocalUtils kerberosAuthToLocalUtils;

    @Override
    public String dbUserKey() {
        return DATABASE_USER;
    }

    @Override
    public String dbPasswordKey() {
        return DATABASE_PASSWORD;
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> config = new ArrayList<>();
        String cmVersion = ConfigUtils.getCmVersion(source);
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_0)) {
            RdsView smmRdsView = getRdsView(source);
            config.add(config(DATABASE_TYPE, dataBaseTypeForCM(smmRdsView.getDatabaseVendor())));
            config.add(config(DATABASE_USER, smmRdsView.getConnectionUserName()));
            config.add(config(DATABASE_PASSWORD, smmRdsView.getConnectionPassword()));
            if (smmRdsView.isUseSsl()
                    && CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(cmVersion, CLOUDERAMANAGER_VERSION_7_2_2)) {
                config.add(config(DATABASE_JDBC_URL_OVERRIDE, smmRdsView.getConnectionURL()));
            } else {
                config.add(config(DATABASE_NAME, smmRdsView.getDatabaseName()));
                config.add(config(DATABASE_HOST, smmRdsView.getHost()));
                config.add(config(DATABASE_PORT, smmRdsView.getPort()));

            }
        } else {
            String cmHost = source.getGeneralClusterConfigs().getPrimaryGatewayInstanceDiscoveryFQDN()
                    .orElse(source.getGeneralClusterConfigs().getClusterManagerIp());

            boolean ssl = source.getGeneralClusterConfigs().getAutoTlsEnabled();
            config = Lists.newArrayList(
                    config("cm.metrics.host", cmHost),
                    config("cm.metrics.username", source.getGeneralClusterConfigs().getCloudbreakClusterManagerUser()),
                    config("cm.metrics.password", source.getGeneralClusterConfigs().getCloudbreakClusterManagerPassword()),
                    config("cm.metrics.protocol", ssl ? "https" : "http"),
                    config("cm.metrics.port", ssl ? "7183" : "7180")
            );
        }
        return config;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        List<ApiClusterTemplateConfig> roleConfigs = new ArrayList<>();
        if (!isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_2_0)) {
            RdsView streamsMessagingManagerRdsView = getRdsView(source);
            roleConfigs.addAll(List.of(
                    config("streams.messaging.manager.storage.connector.connectURI", streamsMessagingManagerRdsView.getConnectionURL()),
                    config("streams.messaging.manager.storage.connector.user", streamsMessagingManagerRdsView.getConnectionUserName()),
                    config("streams.messaging.manager.storage.connector.password", streamsMessagingManagerRdsView.getConnectionPassword())
            ));
        }
        source.getTrustView().ifPresent(trustView -> {
            // OPSAPS-76372 workaround for faulty kerberos.name.rules settings generation
            roleConfigs.add(config(KERBEROS_NAME_RULES, kerberosAuthToLocalUtils.generateEscapedForTrustedRealm(trustView.realm())));
        });
        return roleConfigs;
    }

    @Override
    public String getServiceType() {
        return StreamsMessagingManagerRoles.STREAMS_MESSAGING_MANAGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(StreamsMessagingManagerRoles.STREAMS_MESSAGING_MANAGER_SERVER);
    }

    @Override
    public DatabaseType dbType() {
        return DatabaseType.STREAMS_MESSAGING_MANAGER;
    }
}
