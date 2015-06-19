package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.orchestrator.DockerContainer.AMBARI_AGENT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.PluginExecutionType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;

@Component
public class RecipeEngine {

    public static final int DEFAULT_RECIPE_TIMEOUT = 15;
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public void setupRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakSecuritySetupException {
        Set<InstanceMetaData> instances = instanceMetadataRepository.findAllInStack(stack.getId());
        setupProperties(stack, hostGroups);
        installPlugins(stack, hostGroups, instances);
    }

    public void setupRecipesOnHosts(Stack stack, Set<Recipe> recipes, Set<HostMetadata> hostMetadata) throws CloudbreakSecuritySetupException {
        Set<InstanceMetaData> instances = instanceMetadataRepository.findAllInStack(stack.getId());
        installPluginsOnHosts(stack, recipes, hostMetadata, instances);
    }

    public void executePreInstall(Stack stack) throws CloudbreakSecuritySetupException {
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.PRE_INSTALL, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT);
    }

    public void executePostInstall(Stack stack) throws CloudbreakSecuritySetupException {
        pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.POST_INSTALL, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT);
    }

    private void setupProperties(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakSecuritySetupException {
        LOGGER.info("Setting up recipe properties.");
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIp());
        pluginManager.prepareKeyValues(clientConfig, getAllPropertiesFromRecipes(hostGroups));
    }

    private void installPlugins(Stack stack, Set<HostGroup> hostGroups, Set<InstanceMetaData> instances) throws CloudbreakSecuritySetupException {
        for (HostGroup hostGroup : hostGroups) {
            LOGGER.info("Installing plugins for recipes on hostgroup {}.", hostGroup.getName());
            installPluginsOnHosts(stack, hostGroup.getRecipes(), hostGroup.getHostMetadata(), instances);
        }
    }

    private void installPluginsOnHosts(Stack stack, Set<Recipe> recipes, Set<HostMetadata> hostMetadata, Set<InstanceMetaData> instances)
            throws CloudbreakSecuritySetupException {
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIp());
        for (Recipe recipe : recipes) {
            Map<String, PluginExecutionType> plugins = recipe.getPlugins();
            Map<String, Set<String>> eventIdMap =
                    pluginManager.installPlugins(clientConfig, plugins, getHostnames(hostMetadata));
            pluginManager.waitForEventFinish(stack, instances, eventIdMap, recipe.getTimeout());
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
