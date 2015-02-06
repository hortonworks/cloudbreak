package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.PluginExecutionType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Component
public class RecipeEngine {

    @Autowired
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Autowired
    private HostMetadataRepository hostMetadataRepository;

    @Autowired
    private PluginManager pluginManager;

    public void setupRecipe(Stack stack) {
        Set<InstanceMetaData> instances = instanceMetadataRepository.findAllInStack(stack.getId());
        Map<String, String> properties = stack.getCluster().getRecipe().getKeyValues();
        pluginManager.prepareKeyValues(instances, properties);
        Map<String, PluginExecutionType> plugins = stack.getCluster().getRecipe().getPlugins();
        Set<HostMetadata> hostMetadata = hostMetadataRepository.findHostsInCluster(stack.getCluster().getId());
        Map<String, Set<String>> eventIdMap = pluginManager.installPlugins(instances, plugins, getHostnames(hostMetadata));
        pluginManager.waitForEventFinish(stack, instances, eventIdMap);
    }

    public void executePreInstall(Stack stack) {
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.PRE_INSTALL);
    }

    public void executePostInstall(Stack stack) {
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.POST_INSTALL);
    }

    private Set<String> getHostnames(Set<HostMetadata> hostMetadata) {
        return FluentIterable.from(hostMetadata).transform(new Function<HostMetadata, String>() {
            @Nullable
            @Override
            public String apply(@Nullable HostMetadata metadata) {
                return metadata.getHostName();
            }
        }).toSet();
    }

}
