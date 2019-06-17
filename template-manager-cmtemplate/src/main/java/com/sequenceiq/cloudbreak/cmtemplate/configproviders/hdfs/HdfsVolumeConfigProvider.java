package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HdfsVolumeConfigProvider implements CmHostGroupRoleConfigProvider {

    @Override
    public List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {
            case HdfsRoles.DATANODE:
                return List.of(
                        config("dfs_data_dir_list",
                                buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "datanode"))
                );
            case HdfsRoles.NAMENODE:
                return List.of(
                        config("dfs_name_dir_list",
                                buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "namenode"))
                );
            case HdfsRoles.SECONDARYNAMENODE:
                return List.of(
                        config("fs_checkpoint_dir_list",
                                buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "namesecondary"))
                );
            case HdfsRoles.JOURNALNODE:
                return List.of(
                        config("dfs_journalnode_edits_dir",
                                buildSingleVolumePath(hostGroupView.getVolumeCount(), "journalnode"))
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

}
