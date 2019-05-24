package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;
import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildVolumePathStringZeroVolumeHandled;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
                List<ApiClusterTemplateConfig> configs = List.of(
                        config(DFS_NAME_DIRS, buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "namenode"))
                );
                if (isHA(source)) {
                    configs = new ArrayList<>(configs);
                    configs.addAll(List.of(
                            config("autofailover_enabled", "true"),
                            config("dfs_federation_namenode_nameservice", "ns1"),
                            config("dfs_namenode_quorum_journal_name", "ns1")
                    ));
                }
                return configs;
            case HdfsRoles.SECONDARYNAMENODE:
                return List.of(
                        config(DFS_CHECK_DIRS, buildVolumePathStringZeroVolumeHandled(hostGroupView.getVolumeCount(), "namesecondary"))
                );
            case HdfsRoles.JOURNALNODE:
                return List.of(
                        config("dfs_journalnode_edits_dir", buildSingleVolumePath(hostGroupView.getVolumeCount(), "journalnode"))
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
        return List.of(HdfsRoles.NAMENODE, HdfsRoles.DATANODE, HdfsRoles.SECONDARYNAMENODE, HdfsRoles.JOURNALNODE);
    }

    private boolean isHA(TemplatePreparationObject source) {
        Set<String> namenodeGroups = source.getBlueprintView().getProcessor()
                .getHostGroupsWithComponent(HdfsRoles.NAMENODE);
        return source.getHostgroupViews().stream()
                .filter(hostGroup -> namenodeGroups.contains(hostGroup.getName()))
                .mapToInt(HostgroupView::getNodeCount)
                .sum() > 1;
    }

}
