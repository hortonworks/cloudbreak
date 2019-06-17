package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HdfsRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String FAILED_VOLUMES_TOLERATED = "dfs_datanode_failed_volumes_tolerated";

    private static final Integer NUM_FAILED_VOLUMES_TOLERATED = 0;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case HdfsRoles.DATANODE:
                return List.of(
                        config(FAILED_VOLUMES_TOLERATED, NUM_FAILED_VOLUMES_TOLERATED.toString())
                );
            case HdfsRoles.NAMENODE:
                if (isHA(source)) {
                    return List.of(
                            config("autofailover_enabled", "true"),
                            config("dfs_federation_namenode_nameservice", "ns1"),
                            config("dfs_namenode_quorum_journal_name", "ns1")
                    );
                }
                return List.of();
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
        return List.of(HdfsRoles.NAMENODE, HdfsRoles.DATANODE);
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
