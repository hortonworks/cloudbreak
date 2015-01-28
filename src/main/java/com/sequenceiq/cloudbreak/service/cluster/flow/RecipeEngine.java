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

    void executeRecipe(Stack stack) {
        Set<InstanceMetaData> instanceMetaData = instanceMetadataRepository.findAllInStack(stack.getId());
        Map<String, String> properties = stack.getCluster().getRecipe().getKeyValues();
        Set<String> plugins = stack.getCluster().getRecipe().getPlugins();

        pluginManager.prepareKeyValues(instanceMetaData, properties);
        Set<String> installEventIds = pluginManager.installPlugins(instanceMetaData, plugins);
        pluginManager.waitForEventFinish(stack, instanceMetaData, installEventIds);
        Set<String> triggerEventIds = pluginManager.triggerPlugins(instanceMetaData);
        pluginManager.waitForEventFinish(stack, instanceMetaData, triggerEventIds);
    }
}
