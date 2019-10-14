package com.sequenceiq.cloudbreak.cmtemplate.configproviders.smm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

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

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        String cmHost = source.getGeneralClusterConfigs().getPrimaryGatewayInstanceDiscoveryFQDN()
                .orElse(source.getGeneralClusterConfigs().getClusterManagerIp());

        List<ApiClusterTemplateConfig> configs = Lists.newArrayList(
                config("cm.metrics.host", cmHost),
                config("cm.metrics.username", source.getGeneralClusterConfigs().getCloudbreakAmbariUser()),
                config("cm.metrics.password", source.getGeneralClusterConfigs().getCloudbreakAmbariPassword())
        );
        return configs;
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

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
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
}
