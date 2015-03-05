package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.PluginExecutionType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;

@Component
public class RecipeEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Autowired
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Autowired
    private PluginManager pluginManager;

    public void setupRecipes(Stack stack, Set<HostGroup> hostGroups) {
        MDCBuilder.buildMdcContext(stack);
        Set<InstanceMetaData> instances = instanceMetadataRepository.findAllInStack(stack.getId());
        setupProperties(hostGroups, instances);
        installPlugins(stack, hostGroups, instances);
    }

    public void setupRecipesOnHosts(Stack stack, Set<Recipe> recipes, Set<HostMetadata> hostMetadata) {
        MDCBuilder.buildMdcContext(stack);
        Set<InstanceMetaData> instances = instanceMetadataRepository.findAllInStack(stack.getId());
        installPluginsOnHosts(stack, recipes, hostMetadata, instances);
    }

    public void executePreInstall(Stack stack) {
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.PRE_INSTALL);
    }

    public void executePostInstall(Stack stack) {
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.POST_INSTALL);
    }

    private void setupProperties(Set<HostGroup> hostGroups, Set<InstanceMetaData> instances) {
        LOGGER.info("Setting up recipe properties.");
        pluginManager.prepareKeyValues(instances, getAllPropertiesFromRecipes(hostGroups));
    }

    private void installPlugins(Stack stack, Set<HostGroup> hostGroups, Set<InstanceMetaData> instances) {
        for (HostGroup hostGroup : hostGroups) {
            LOGGER.info("Installing plugins for recipes on hostgroup {}.", hostGroup.getName());
            installPluginsOnHosts(stack, hostGroup.getRecipes(), hostGroup.getHostMetadata(), instances);
        }
    }

    private void installPluginsOnHosts(Stack stack, Set<Recipe> recipes, Set<HostMetadata> hostMetadata, Set<InstanceMetaData> instances) {
        for (Recipe recipe : recipes) {
            Map<String, PluginExecutionType> plugins = recipe.getPlugins();
            Map<String, Set<String>> eventIdMap = pluginManager.installPlugins(instances, plugins, getHostnames(hostMetadata));
            pluginManager.waitForEventFinish(stack, instances, eventIdMap);
        }
    }

    private Map<String, String> getAllPropertiesFromRecipes(Set<HostGroup> hostGroups) {
        Map<String, String> properties = new HashMap<>();
        for (HostGroup hostGroup : hostGroups) {
            for (Recipe recipe : hostGroup.getRecipes()) {
                properties.putAll(recipe.getKeyValues());
            }
        }
        return properties;
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
