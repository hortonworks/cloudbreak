package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.KeyValue;
import com.sequenceiq.cloudbreak.domain.Plugin;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.KeyValueRepository;
import com.sequenceiq.cloudbreak.repository.PluginRepository;

@Component
public class RecipeEngine {

    @Autowired
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Autowired
    private PluginRepository pluginRepository;

    @Autowired
    private KeyValueRepository keyValueRepository;

    @Autowired
    private PluginManager pluginManager;

    void executeRecipe(Stack stack) {
        Set<InstanceMetaData> instanceMetaData = instanceMetadataRepository.findAllInStack(stack.getId());
        Set<Plugin> plugins = pluginRepository.findAllForRecipe(stack.getCluster().getRecipe().getId());
        Set<KeyValue> keyValues = keyValueRepository.findAllForRecipe(stack.getCluster().getRecipe().getId());

        pluginManager.prepareKeyValues(instanceMetaData, keyValues);
        Set<String> installEventIds = pluginManager.installPlugins(instanceMetaData, plugins);
        pluginManager.waitForEventFinish(stack, instanceMetaData, installEventIds);
        Set<String> triggerEventIds = pluginManager.triggerPlugins(instanceMetaData, plugins);
        pluginManager.waitForEventFinish(stack, instanceMetaData, triggerEventIds);
    }
}
