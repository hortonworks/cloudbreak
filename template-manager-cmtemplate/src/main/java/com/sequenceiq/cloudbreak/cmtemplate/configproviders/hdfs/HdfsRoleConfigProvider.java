package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.core.CoreRoles.HADOOP_RPC_PROTECTION;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class HdfsRoleConfigProvider extends AbstractRoleConfigProvider {

    private static final String FAILED_VOLUMES_TOLERATED = "dfs_datanode_failed_volumes_tolerated";

    private static final Integer NUM_FAILED_VOLUMES_TOLERATED = 0;

    private static final String DFS_ENCRYPT_DATA_TRANSFER = "dfs_encrypt_data_transfer";

    private static final String DFS_DATA_TRANSFER_PROTECTION = "dfs_data_transfer_protection";

    private static final String DATANODE_CONFIG_SAFETY_VALVE = "datanode_config_safety_valve";

    private static final String CONNECT_MAX_RETRIES_ON_TIMEOUTS = "ipc.client.connect.max.retries.on.timeouts";

    private static final String CLIENT_CONNECT_TIMEOUT = "ipc.client.connect.timeout";

    private static final String MAX_RETRIES = "5";

    private static final String CONNECTION_TIMEOUT = "5000";

    private final EntitlementService entitlementService;

    private final HdfsConfigHelper hdfsConfigHelper;

    public HdfsRoleConfigProvider(EntitlementService entitlementService, HdfsConfigHelper hdfsConfigHelper) {
        this.entitlementService = entitlementService;
        this.hdfsConfigHelper = hdfsConfigHelper;
    }

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        switch (roleType) {
            case HdfsRoles.DATANODE:
                List<ApiClusterTemplateConfig> configs = new ArrayList<>();
                configs.add(
                        config(FAILED_VOLUMES_TOLERATED, NUM_FAILED_VOLUMES_TOLERATED.toString())
                );
                if (isWireEncryptionEnabled(source)) {
                    configs.add(
                            config(DFS_ENCRYPT_DATA_TRANSFER, "true")
                    );
                }
                if (StackType.DATALAKE.equals(source.getStackType()) && hdfsConfigHelper.isDataNodeHA(source)) {
                    StringBuilder safetyValveValue = new StringBuilder();
                    safetyValveValue.append(getSafetyValveProperty(CONNECT_MAX_RETRIES_ON_TIMEOUTS, MAX_RETRIES));
                    safetyValveValue.append(getSafetyValveProperty(CLIENT_CONNECT_TIMEOUT, CONNECTION_TIMEOUT));
                    configs.add(config(DATANODE_CONFIG_SAFETY_VALVE, safetyValveValue.toString()));
                }
                return configs;
            case HdfsRoles.NAMENODE:
                if (hdfsConfigHelper.isNamenodeHA(source)) {
                    String nameService = hdfsConfigHelper.getNameService(templateProcessor, source);
                    return List.of(
                            config("autofailover_enabled", "true"),
                            config("dfs_federation_namenode_nameservice", nameService),
                            config("dfs_namenode_quorum_journal_name", nameService)
                    );
                }
                return List.of();
            case HdfsRoles.GATEWAY:
                if (hdfsConfigHelper.isNamenodeHA(source) && StackType.DATALAKE.equals(source.getStackType())) {
                    return List.of(config(
                            HdfsConfigHelper.HDFS_CLIENT_CONFIG_SAFETY_VALVE,
                            getSafetyValveProperty("dfs.client.block.write.replace-datanode-on-failure.policy", "NEVER")
                                    + getSafetyValveProperty("dfs.client.block.write.replace-datanode-on-failure.enable", "false")
                    ));
                }
                return List.of();
            default:
                return List.of();
        }
    }

    @Override
    public List<ApiClusterTemplateConfig> getServiceConfigs(CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        if (isWireEncryptionEnabled(source) || source.getGeneralClusterConfigs().isGovCloud()) {
            configs.add(config(HADOOP_RPC_PROTECTION, "privacy"));
        }
        if (isWireEncryptionEnabled(source)) {
            configs.add(config(DFS_DATA_TRANSFER_PROTECTION, "privacy"));
            configs.add(config(DFS_ENCRYPT_DATA_TRANSFER, "true"));
        }
        return configs;
    }

    private boolean isWireEncryptionEnabled(TemplatePreparationObject source) {
        return entitlementService.isWireEncryptionEnabled(ThreadBasedUserCrnProvider.getAccountId())
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
