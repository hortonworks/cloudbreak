package com.sequenceiq.cloudbreak.cm.config;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
class CmMgmtServiceConfigMemoryService implements CmConfigServiceDelegate {

    @Inject
    private SMONMemoryWorkaroundService sMONMemoryWorkaroundService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public void setConfigs(Stack stack, ApiRoleList apiRoleList) {
        Optional<ApiRole> serviceMonitor = getApiRole("SERVICEMONITOR", apiRoleList);
        Set<String> components = cmTemplateProcessorFactory.get(stack.getCluster().getBlueprint().getBlueprintText())
                .getComponentsByHostGroup()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        serviceMonitor.ifPresent(apiRole ->
                addConfig(apiRole,
                    createApiConfig("firehose_heapsize",
                        sMONMemoryWorkaroundService.firehoseHeapsize(stack.getType(), components)
                    )
                )
        );
        serviceMonitor.ifPresent(apiRole ->
                addConfig(apiRole,
                    createApiConfig("firehose_non_java_memory_bytes",
                        sMONMemoryWorkaroundService.firehoseNonJavaMemoryBytes(stack.getType(), components, stack.getFullNodeCount())
                    )
                )
        );
    }

    private Optional<ApiRole> getApiRole(String roleName, ApiRoleList apiRoleList) {
        if (CollectionUtils.isEmpty(apiRoleList.getItems())) {
            return Optional.empty();
        }
        return apiRoleList.getItems().stream()
                .filter(apiRole -> apiRole != null && apiRole.getName().equals(roleName))
                .findFirst();
    }
}
