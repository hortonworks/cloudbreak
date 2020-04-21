package com.sequenceiq.periscope.monitor.client;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.InstanceConfig;
import com.sequenceiq.periscope.monitor.handler.ClouderaManagerCommunicator;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component
public class YarnServiceConfigClient {

    private static final String YARN_SERVICE = "yarn";

    private static final String NODEMANAGER_ROLE = "NODEMANAGER";

    private static final String YARN_NODEMANAGER_VCORES = "yarn_nodemanager_resource_cpu_vcores";

    private static final String YARN_NODEMANAGER_MEMORY = "yarn_nodemanager_resource_memory_mb";

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private ClouderaManagerCommunicator clouderaManagerCommunicator;

    @Cacheable(cacheNames = "instanceConfigCache", unless = "#result.getDefaultValueUsed() == true", key = "#cluster.id + #hostGroup")
    public InstanceConfig getInstanceConfigFromCM(Cluster cluster, StackV4Response stackV4Response, String hostGroup)
            throws Exception {
        String nodeManagerRoleConfigName = stackResponseUtils
                .getRoleConfigNameForHostGroup(stackV4Response, hostGroup, YARN_SERVICE, NODEMANAGER_ROLE);

        Map<String, ApiConfig> roleConfigProperties = clouderaManagerCommunicator.getRoleConfigPropertiesFromCM(cluster, YARN_SERVICE,
                nodeManagerRoleConfigName, Set.of(YARN_NODEMANAGER_VCORES, YARN_NODEMANAGER_MEMORY));

        ApiConfig apiConfigVCores = roleConfigProperties.get(YARN_NODEMANAGER_VCORES);
        ApiConfig apiConfigMemory = roleConfigProperties.get(YARN_NODEMANAGER_MEMORY);

        InstanceConfig instanceConfig = new InstanceConfig(hostGroup);
        String hostVCores = Optional.ofNullable(apiConfigVCores.getValue()).orElseGet(() -> {
            instanceConfig.setDefaultValueUsed(true);
            return apiConfigVCores.getDefault();
        });

        String hostMemory = Optional.ofNullable(apiConfigMemory.getValue()).orElseGet(() -> {
            instanceConfig.setDefaultValueUsed(true);
            return apiConfigMemory.getDefault();
        });

        instanceConfig.setCoreCPU(Integer.valueOf(hostVCores));
        instanceConfig.setMemoryInMb(Long.valueOf(hostMemory));
        return instanceConfig;
    }
}
