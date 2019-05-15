package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HdfsRoleConfigConfigProvider extends AbstractRoleConfigConfigProvider {

    private static final String DFS_DATA_DIRS = "dfs_data_dir_list";

    private static final String DFS_NAME_DIRS = "dfs_name_dir_list";

    private static final String DFS_CHECK_DIRS = "fs_checkpoint_dir_list";

    private static final String FAILED_VOLUMES_TOLERATED = "dfs_datanode_failed_volumes_tolerated";

    private static final Integer NUM_FAILED_VOLUMES_TOLERATED = 0;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfig(String roleType, HostgroupView hostGroupView, TemplatePreparationObject source) {
        switch (roleType) {
            case HdfsRoles.DATANODE:
                return List.of(
                        config(DFS_DATA_DIRS, buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "datanode")),
                        config(FAILED_VOLUMES_TOLERATED, NUM_FAILED_VOLUMES_TOLERATED.toString())
                );
            case HdfsRoles.NAMENODE:
                return List.of(
                        config(DFS_NAME_DIRS, buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "namenode"))
                );
            case HdfsRoles.SECONDARYNAMENODE:
                return List.of(
                        config(DFS_CHECK_DIRS, buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "namesecondary"))
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
    public List<String> getRoleTypes() {
        return List.of(HdfsRoles.NAMENODE, HdfsRoles.DATANODE, HdfsRoles.SECONDARYNAMENODE);
    }

}
