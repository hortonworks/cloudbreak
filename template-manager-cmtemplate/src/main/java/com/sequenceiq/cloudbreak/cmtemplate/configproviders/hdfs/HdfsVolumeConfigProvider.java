package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HdfsVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        Integer volumeCount = Objects.nonNull(hostGroupView) ? hostGroupView.getVolumeCount() : 0;
        switch (roleType) {
            case HdfsRoles.DATANODE:
                return List.of(
                        config("dfs_data_dir_list",
                                buildVolumePathStringZeroVolumeHandled(volumeCount, "datanode"))
                );
            case HdfsRoles.NAMENODE:
                return List.of(
                        config("dfs_name_dir_list",
                                buildVolumePathStringZeroVolumeHandled(volumeCount, "namenode"))
                );
            case HdfsRoles.SECONDARYNAMENODE:
                return List.of(
                        config("fs_checkpoint_dir_list",
                                buildVolumePathStringZeroVolumeHandled(volumeCount, "namesecondary"))
                );
            case HdfsRoles.JOURNALNODE:
                return List.of(
                        config("dfs_journalnode_edits_dir",
                                buildSingleVolumePath(volumeCount, "journalnode"))
                );
            default:
                return List.of();
        }
    }

    @Override
    public String getServiceType() {
        return HdfsRoles.HDFS;
    }

    @Override
    public Set<String> getRoleTypes() {
        return Set.of(HdfsRoles.DATANODE, HdfsRoles.JOURNALNODE, HdfsRoles.NAMENODE, HdfsRoles.SECONDARYNAMENODE);
    }

    @Override
    public boolean sharedRoleType(String roleType) {
        return "JOURNALNODE".equals(roleType);
    }

    @Override
    public Map<String, String> getConfigAfterAddingVolumes(HostgroupView hostgroupView, TemplatePreparationObject source, ServiceComponent serviceComponent) {
        Map<String, String> config = new HashMap<>();

        List<ApiClusterTemplateConfig> roleConfigs = getRoleConfigs(serviceComponent.getComponent(), hostgroupView, source);
        for (ApiClusterTemplateConfig roleConfig : roleConfigs) {
            config.put(roleConfig.getName(), roleConfig.getValue());
        }

        return config;
    }
}
