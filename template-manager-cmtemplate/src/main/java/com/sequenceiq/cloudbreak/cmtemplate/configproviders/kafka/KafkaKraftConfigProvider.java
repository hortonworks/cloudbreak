package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_17;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.zookeeper.ZooKeeperRoles;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class KafkaKraftConfigProvider extends AbstractRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        ArrayList<ApiClusterTemplateConfig> configs = Lists.newArrayList();
        String cdhVersion = getCdhVersion(source);

        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_17)) {
            Optional<HostgroupView> kraftHostGroup = source.getHostGroupsWithComponent(KafkaRoles.KAFKA_KRAFT).findFirst();
            Optional<HostgroupView> zookeeperHostGroup = source.getHostGroupsWithComponent(ZooKeeperRoles.ZOOKEEPER_SERVER).findFirst();
            if (zookeeperHostGroup.isEmpty() && kraftHostGroup.isEmpty()) {
                throw new CloudbreakRuntimeException("KRaft or Zookeeper hostgroup must be present to determine the metadata store type.");
            }
            if (kraftHostGroup.isPresent() && kraftHostGroup.get().getNodeCount() > 0
                // support KRaft only in Workload clusters for now
                && StackType.WORKLOAD.equals(source.getStackType())) {
                configs.add(
                    config(KafkaConfigs.METADATA_STORE, "KRaft")
                );
            // since KRaft is the default, we need to explicitly set Zookeeper on any clusters if KRaft hot group is not present
            } else if (zookeeperHostGroup.isPresent()) {
                configs.add(
                    config(KafkaConfigs.METADATA_STORE, "Zookeeper")
                );
            }
        }
        return configs;
    }

    @Override
    public String getServiceType() {
        return KafkaRoles.KAFKA_SERVICE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KafkaRoles.KAFKA_BROKER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return StackType.WORKLOAD.equals(source.getStackType()) || StackType.DATALAKE.equals(source.getStackType());
    }
}
