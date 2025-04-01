package com.sequenceiq.cloudbreak.cmtemplate.configproviders.srm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_12;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

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
        String cdpVersion = source.getBlueprintView().getProcessor().getStackVersion() == null ?
                "" : source.getBlueprintView().getProcessor().getStackVersion();
        StringBuilder replicationConfig = new StringBuilder();

        int kafkaBrokerPort = source.getGeneralClusterConfigs().getAutoTlsEnabled() ? KAFKA_SECURE_PORT : KAFKA_PLAIN_PORT;
        List<HostgroupView> hostGroups = source.getHostGroupsWithComponent(KafkaRoles.KAFKA_BROKER).collect(Collectors.toList());
        List<HostgroupView> coreBrokerHostGroup = hostGroups.stream().filter(g -> g.getName().equals("core_broker")).collect(Collectors.toList());
        List<String> brokerHosts;

        if (!coreBrokerHostGroup.isEmpty() && isVersionNewerOrEqualThanLimited(cdpVersion, CLOUDERA_STACK_VERSION_7_2_12)) {
            brokerHosts = coreBrokerHostGroup.get(0).getHosts().stream().collect(Collectors.toList());
        } else {
            brokerHosts = hostGroups.stream()
                    .flatMap(h -> h.getHosts().stream()).collect(Collectors.toList());
        }

        String boostrapServers = brokerHosts.stream().map(h -> h + ":" + kafkaBrokerPort)
                .collect(Collectors.joining(","));
        replicationConfig.append("bootstrap.servers=" + boostrapServers);

        if (isVersionNewerOrEqualThanLimited(cdpVersion, CLOUDERA_STACK_VERSION_7_2_12)) {
            replicationConfig.append("|" + "security.protocol=SASL_SSL");
        }

        return boostrapServers.isEmpty()
                ? List.of()
                : List.of(ConfigUtils.config(CLUSTERS_CONFIG, "primary,secondary"),
                        ConfigUtils.config(REPLICATIONS_CONFIG, replicationConfig.toString()));
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
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
