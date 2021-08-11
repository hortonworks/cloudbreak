package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class KafkaVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        Optional<HostgroupView> hostGroup = source.getHostGroupsWithComponent(KafkaRoles.KAFKA_BROKER)
                        .min(Comparator.comparing(HostgroupView::getVolumeCount));
        Integer minimumVolumeCount = hostGroup.isPresent() ? hostGroup.get().getVolumeCount() : 0;
        Integer volumeCount = Objects.nonNull(hostGroupView) ? hostGroupView.getVolumeCount() : 0;
        switch (roleType) {
            case KafkaRoles.KAFKA_BROKER:
                if (StackType.WORKLOAD.equals(source.getStackType())) {
                    return List.of(config("log.dirs", buildVolumePathStringZeroVolumeHandled(minimumVolumeCount, "kafka")));
                } else {
                    return List.of(config("log.dirs", buildVolumePathStringZeroVolumeHandled(volumeCount, "kafka")));
                }
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return KafkaRoles.KAFKA_SERVICE;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(KafkaRoles.KAFKA_BROKER);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return KafkaRoles.KAFKA_BROKER.equals(roleType);
    }
}
