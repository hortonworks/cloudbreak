package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Service
public class DefaultBlueprintJvmParameterRemovalService {

    private static final Set<String> REMOVABLE_PROPERTIES = Set.of(
            "atlas_max_heap_size", "broker_max_heap_size", "datanode_java_heapsize", "dfs_datanode_max_locked_memory", "hbase_regionserver_java_heapsize",
            "hive_metastore_java_heapsize", "hive_metastore_server_max_message_size", "java.arg.2", "java.arg.3", "namenode_java_heapsize",
            "solr_java_heapsize", "solr_java_direct_memory_size", "zookeeper_server_java_heapsize");

    @Inject
    private EntitlementService entitlementService;

    public void removeJvmPropertiesIfNeeded(StackDtoDelegate stack, ApiClusterTemplate apiClusterTemplate) {
        if (isDefaultTemplate(stack) && isNewerOrEqualThan732(apiClusterTemplate) && isBlueprintJvmParameterRemovalAllowed(stack)
                && apiClusterTemplate.getServices() != null) {
            for (ApiClusterTemplateService service : apiClusterTemplate.getServices()) {
                if (service.getRoleConfigGroups() != null) {
                    for (ApiClusterTemplateRoleConfigGroup roleConfigGroup : service.getRoleConfigGroups()) {
                        if (roleConfigGroup.getConfigs() != null) {
                            roleConfigGroup.configs(roleConfigGroup.getConfigs()
                                    .stream()
                                    .filter(config -> !REMOVABLE_PROPERTIES.contains(config.getName()))
                                    .toList());
                        }
                    }
                }
            }
        }
    }

    private boolean isDefaultTemplate(StackDtoDelegate stack) {
        return stack.getBlueprint().getStatus().isDefault();
    }

    private boolean isNewerOrEqualThan732(ApiClusterTemplate apiClusterTemplate) {
        return isVersionNewerOrEqualThanLimited(apiClusterTemplate.getCdhVersion(), CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2);
    }

    private boolean isBlueprintJvmParameterRemovalAllowed(StackDtoDelegate stack) {
        return entitlementService.isBlueprintJvmParameterRemovalEnabled(Crn.safeFromString(stack.getResourceCrn()).getAccountId());
    }
}
