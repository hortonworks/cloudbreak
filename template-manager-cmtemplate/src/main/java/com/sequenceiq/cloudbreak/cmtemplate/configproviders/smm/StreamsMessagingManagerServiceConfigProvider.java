package com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.schemaregistry.StreamingAppRdsRoleConfigProviderUtil.dataBaseTypeForCM;
import static java.util.Collections.emptyList;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRdsRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.RdsView;

@Component
public class StreamsMessagingManagerServiceConfigProvider extends AbstractRdsRoleConfigProvider {

    static final String DATABASE_TYPE = "smm_database_type";

    static final String DATABASE_NAME = "smm_database_name";

    static final String DATABASE_HOST = "smm_database_host";

    static final String DATABASE_PORT = "smm_database_port";

    static final String DATABASE_USER = "smm_database_user";

    static final String DATABASE_PASSWORD = "smm_database_password";

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        final List<ApiClusterTemplateConfig> configList;
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_1)) {
            RdsView smmRdsView = getRdsView(source);
            configList = List.of(
                    config(DATABASE_TYPE, dataBaseTypeForCM(smmRdsView.getDatabaseVendor())),
                    config(DATABASE_NAME, smmRdsView.getDatabaseName()),
                    config(DATABASE_HOST, smmRdsView.getHost()),
                    config(DATABASE_PORT, smmRdsView.getPort()),
                    config(DATABASE_USER, smmRdsView.getConnectionUserName()),
                    config(DATABASE_PASSWORD, smmRdsView.getConnectionPassword())
            );
        } else {
            String cmHost = source.getGeneralClusterConfigs().getPrimaryGatewayInstanceDiscoveryFQDN()
                    .orElse(source.getGeneralClusterConfigs().getClusterManagerIp());

            boolean ssl = source.getGeneralClusterConfigs().getAutoTlsEnabled();
            configList = Lists.newArrayList(
                    config("cm.metrics.host", cmHost),
                    config("cm.metrics.username", source.getGeneralClusterConfigs().getCloudbreakAmbariUser()),
                    config("cm.metrics.password", source.getGeneralClusterConfigs().getCloudbreakAmbariPassword()),
                    config("cm.metrics.protocol", ssl ? "https" : "http"),
                    config("cm.metrics.port", ssl ? "7183" : "7180")
            );
        }
        return configList;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        String cdhVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        if (!isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERAMANAGER_VERSION_7_1_1)) {
            switch (roleType) {
                case StreamsMessagingManagerRoles.STREAMS_MESSAGING_MANAGER_SERVER:
                    RdsView streamsMessagingManagerRdsView = getRdsView(source);
                    return List.of(
                            config("streams.messaging.manager.storage.connector.connectURI", streamsMessagingManagerRdsView.getConnectionURL()),
                            config("streams.messaging.manager.storage.connector.user", streamsMessagingManagerRdsView.getConnectionUserName()),
                            config("streams.messaging.manager.storage.connector.password", streamsMessagingManagerRdsView.getConnectionPassword())
                    );
                default:
                    return List.of();
            }
        }
        return emptyList();
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
    protected DatabaseType dbType() {
        return DatabaseType.STREAMS_MESSAGING_MANAGER;
    }
}
