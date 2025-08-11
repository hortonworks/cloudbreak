package com.sequenceiq.cloudbreak.cm.config;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Service
class CmMgmtServiceConfigMemoryService implements CmConfigServiceDelegate {

    @Inject
    private SMONMemoryWorkaroundService sMONMemoryWorkaroundService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public void setConfigs(StackDtoDelegate stack, ApiRoleList apiRoleList) {
        Optional<ApiRole> serviceMonitor = getApiRole("SERVICEMONITOR", apiRoleList);
        Set<String> components = cmTemplateProcessorFactory.get(stack.getBlueprintJsonText())
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
                        sMONMemoryWorkaroundService.firehoseNonJavaMemoryBytes(stack.getType(), components, stack.getFullNodeCount().intValue())
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
