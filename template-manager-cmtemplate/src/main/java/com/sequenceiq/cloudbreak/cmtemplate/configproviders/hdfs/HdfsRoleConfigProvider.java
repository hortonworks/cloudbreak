package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HdfsRoleConfigProvider extends AbstractRoleConfigProvider {

    public static final String DEFAULT_NAME_SERVICE = "ns1";

    private static final String FAILED_VOLUMES_TOLERATED = "dfs_datanode_failed_volumes_tolerated";

    private static final Integer NUM_FAILED_VOLUMES_TOLERATED = 0;

    private static final String DFS_REPLICATION = "dfs_replication";

    private static final Integer DFS_REPLICATION_VALUE = 2;

    private final EntitlementService entitlementService;

    public HdfsRoleConfigProvider(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case HdfsRoles.DATANODE:
                List<ApiClusterTemplateConfig> configs = new ArrayList<>();
                configs.add(
                        config(FAILED_VOLUMES_TOLERATED, NUM_FAILED_VOLUMES_TOLERATED.toString())
                );
                if (isDfsReplicaControlled(source)) {
                    configs.add(
                            config(DFS_REPLICATION, DFS_REPLICATION_VALUE.toString())
                    );
                }
                return configs;
            case HdfsRoles.NAMENODE:
                if (isNamenodeHA(source)) {
                    return List.of(
                            config("autofailover_enabled", "true"),
                            config("dfs_federation_namenode_nameservice", DEFAULT_NAME_SERVICE),
                            config("dfs_namenode_quorum_journal_name", DEFAULT_NAME_SERVICE)
                    );
                }
                return List.of();
            default:
                return List.of();
        }
    }

    private boolean isDfsReplicaControlled(TemplatePreparationObject source) {
        return entitlementService.isSDXOptimizedConfigurationEnabled(ThreadBasedUserCrnProvider.getAccountId())
                && isNamenodeHA(source)
                && null != source.getStackType()
                && source.getStackType().equals(StackType.DATALAKE);
    }

    @Override
    public String getServiceType() {
        return HdfsRoles.HDFS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HdfsRoles.NAMENODE, HdfsRoles.DATANODE);
    }

    public static boolean isNamenodeHA(TemplatePreparationObject source) {
        return source.getHostGroupsWithComponent(HdfsRoles.NAMENODE)
                .mapToInt(HostgroupView::getNodeCount)
                .sum() > 1;
    }

    public static Set<String> nameNodeFQDNs(TemplatePreparationObject source) {
        return source.getHostGroupsWithComponent(HdfsRoles.NAMENODE)
                .flatMap(hostGroup -> hostGroup.getHosts().stream())
                .collect(toSet());
    }

}
