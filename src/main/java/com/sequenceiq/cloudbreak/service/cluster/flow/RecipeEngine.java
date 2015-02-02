package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Component
public class RecipeEngine {

    @Autowired
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Autowired
    private PluginManager pluginManager;

    public void setupRecipe(Stack stack) {
        Set<InstanceMetaData> instances = instanceMetadataRepository.findAllInStack(stack.getId());
        Map<String, String> properties = stack.getCluster().getRecipe().getKeyValues();
        Set<String> plugins = stack.getCluster().getRecipe().getPlugins();
        pluginManager.prepareKeyValues(instances, properties);
        Set<String> installEventIds = pluginManager.installPlugins(instances, plugins);
        pluginManager.waitForEventFinish(stack, instances, installEventIds);
    }

    public void executePreInstall(Stack stack) {
        triggerAndWaitForPlugins(stack, RecipeLifecycleEvent.PRE_INSTALL);
    }

    public void executePostInstall(Stack stack) {
        triggerAndWaitForPlugins(stack, RecipeLifecycleEvent.POST_INSTALL);
    }

    private void triggerAndWaitForPlugins(Stack stack, RecipeLifecycleEvent event) {
        Set<InstanceMetaData> instances = instanceMetadataRepository.findAllInStack(stack.getId());
        Set<String> triggerEventIds = pluginManager.triggerPlugins(instances, event);
        pluginManager.waitForEventFinish(stack, instances, triggerEventIds);
    }

}
