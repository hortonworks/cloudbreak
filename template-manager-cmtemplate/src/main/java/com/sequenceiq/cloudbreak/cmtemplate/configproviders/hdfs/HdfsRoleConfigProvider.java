package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.HADOOP_RPC_PROTECTION;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class HdfsRoleConfigProvider extends AbstractRoleConfigProvider {

    public static final String DEFAULT_NAME_SERVICE = "ns1";

    private static final String FAILED_VOLUMES_TOLERATED = "dfs_datanode_failed_volumes_tolerated";

    private static final Integer NUM_FAILED_VOLUMES_TOLERATED = 0;

    private static final String DFS_ENCRYPT_DATA_TRANSFER = "dfs_encrypt_data_transfer";

    private static final String DFS_DATA_TRANSFER_PROTECTION = "dfs_data_transfer_protection";

    private final EntitlementService entitlementService;

    public HdfsRoleConfigProvider(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
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

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        switch (roleType) {
            case HdfsRoles.DATANODE:
                List<ApiClusterTemplateConfig> configs = new ArrayList<>();
                configs.add(
                        config(FAILED_VOLUMES_TOLERATED, NUM_FAILED_VOLUMES_TOLERATED.toString())
                );
                if (isSDXOptimizationEnabled(source)) {
                    configs.add(
                            config(DFS_ENCRYPT_DATA_TRANSFER, "true")
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
            case HdfsRoles.GATEWAY:
                if (isSDXOptimizationEnabled(source) && isNamenodeHA(source)) {
                    //CHECKSTYLE:OFF
                    return List.of(config(
                            "hdfs_client_config_safety_valve",
                            "<property><name>dfs.client.block.write.replace-datanode-on-failure.policy</name><value>NEVER</value></property><property><name>dfs.client.block.write.replace-datanode-on-failure.enable</name><value>false</value></property>")
                    );
                    //CHECKSTYLE:ON
                }
                return List.of();
            default:
                return List.of();
        }
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        if (isSDXOptimizationEnabled(source)) {
            configs.add(config(DFS_ENCRYPT_DATA_TRANSFER, "true"));
            configs.add(config(DFS_DATA_TRANSFER_PROTECTION, "privacy"));
            configs.add(config(HADOOP_RPC_PROTECTION, "privacy"));
        }
        return configs;
    }

    private boolean isSDXOptimizationEnabled(TemplatePreparationObject source) {
        return entitlementService.isSDXOptimizedConfigurationEnabled(ThreadBasedUserCrnProvider.getAccountId())
                && !CloudPlatform.YARN.equals(source.getCloudPlatform())
                && StackType.DATALAKE.equals(source.getStackType());
    }

    @Override
    public String getServiceType() {
        return HdfsRoles.HDFS;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(HdfsRoles.NAMENODE, HdfsRoles.DATANODE, HdfsRoles.GATEWAY);
    }

}
