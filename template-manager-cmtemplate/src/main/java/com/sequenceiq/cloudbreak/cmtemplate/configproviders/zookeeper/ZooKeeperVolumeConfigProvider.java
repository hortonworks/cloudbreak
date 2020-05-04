package com.sequenceiq.cloudbreak.cmtemplate.configproviders.zookeeper;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class ZooKeeperVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        Integer volumeCount = Objects.nonNull(hostGroupView) ? hostGroupView.getVolumeCount() : 0;
        if (ZooKeeperRoles.ZOOKEEPER_SERVER.equals(roleType)) {
            return List.of(
                    config("dataDir", buildSingleVolumePath(volumeCount, "zookeeper")),
                    config("dataLogDir", buildSingleVolumePath(volumeCount, "zookeeper"))
            );
        }
        return List.of();
    }

    @Override
    public String getServiceType() {
        return ZooKeeperRoles.ZOOKEEPER;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(ZooKeeperRoles.ZOOKEEPER_SERVER);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return "SERVER".equals(roleType);
    }
}
