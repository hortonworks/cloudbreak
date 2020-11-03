package com.sequenceiq.cloudbreak.cmtemplate.configproviders.srm;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class StreamsReplicationManagerConfigProvider extends AbstractRoleConfigProvider {

    private static final String CLUSTERS_CONFIG = "clusters";

    private static final String REPLICATIONS_CONFIG = "streams.replication.manager.config";

    private static final String DRIVER_TARGET_CONFIG = "streams.replication.manager.driver.target.cluster";

    private static final String SERVICE_TARGET_CONFIG = "streams.replication.manager.service.target.cluster";

    private static final int KAFKA_SECURE_PORT = 9093;

    private static final int KAFKA_PLAIN_PORT = 9092;

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        int kafkaBrokerPort = source.getGeneralClusterConfigs().getAutoTlsEnabled() ? KAFKA_SECURE_PORT : KAFKA_PLAIN_PORT;

        List<String> brokerHosts = source.getHostGroupsWithComponent(KafkaRoles.KAFKA_BROKER)
                .flatMap(h -> h.getHosts().stream()).collect(Collectors.toList());
        String boostrapServers = brokerHosts.stream().map(h -> h + ":" + kafkaBrokerPort)
                .collect(Collectors.joining(","));

        return boostrapServers.isEmpty()
                ? List.of()
                : List.of(ConfigUtils.config(CLUSTERS_CONFIG, "primary,secondary"),
                        ConfigUtils.config(REPLICATIONS_CONFIG, "bootstrap.servers=" + boostrapServers));
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case StreamsReplicationManagerRoles.STREAMS_REPLICATION_MANAGER_DRIVER:
                return List.of(ConfigUtils.config(DRIVER_TARGET_CONFIG, "primary"));
            case StreamsReplicationManagerRoles.STREAMS_REPLICATION_MANAGER_SERVICE:
                return List.of(ConfigUtils.config(SERVICE_TARGET_CONFIG, "primary"));
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return StreamsReplicationManagerRoles.STREAMS_REPLICATION_MANAGER;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(StreamsReplicationManagerRoles.STREAMS_REPLICATION_MANAGER_DRIVER,
                StreamsReplicationManagerRoles.STREAMS_REPLICATION_MANAGER_SERVICE);
    }
}
