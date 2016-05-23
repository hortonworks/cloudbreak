package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.api.model.PluginExecutionType;
import com.sequenceiq.cloudbreak.core.CloudbreakRecipeSetupException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig;

@Component
public class RecipeEngine {

    public static final int DEFAULT_RECIPE_TIMEOUT = 15;
    public static final String RECIPE_KEY_PREFIX = "consul-watch-plugin/";
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public void setupRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakSecuritySetupException {
        Set<InstanceMetaData> instances = instanceMetadataRepository.findNotTerminatedForStack(stack.getId());
        cleanupPlugins(stack, hostGroups, instances);
        uploadConsulRecipes(stack, getRecipesInHostGroups(hostGroups));
        setupProperties(stack, hostGroups);
        installPlugins(stack, hostGroups, instances);
    }

    private void uploadConsulRecipes(Stack stack, Iterable<Recipe> recipes) throws CloudbreakSecuritySetupException {
        HttpClientConfig clientConfig = null;
        for (Recipe recipe : recipes) {
            for (String plugin : recipe.getPlugins().keySet()) {
                if (plugin.startsWith("base64://")) {
                    Map<String, String> keyValues = new HashMap<>();
                    keyValues.put(getPluginConsulKey(recipe, plugin), new String(Base64.decodeBase64(plugin.replaceFirst("base64://", ""))));
                    if (clientConfig == null) {
                        InstanceGroup gateway = stack.getGatewayInstanceGroup();
                        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
                        clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIpWrapper());
                    }
                    pluginManager.prepareKeyValues(clientConfig, keyValues);
                }
            }
        }
    }

    public void setupRecipesOnHosts(Stack stack, Set<Recipe> recipes, Set<HostMetadata> hostMetadata) throws CloudbreakSecuritySetupException {
        uploadConsulRecipes(stack, recipes);
        Set<InstanceMetaData> instances = instanceMetadataRepository.findNotTerminatedForStack(stack.getId());
        installPluginsOnHosts(stack, recipes, hostMetadata, instances, true);
    }

    public void executePreInstall(Stack stack) throws CloudbreakSecuritySetupException, CloudbreakRecipeSetupException {
        try {
            pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.PRE_INSTALL, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT);
        } catch (CloudbreakServiceException e) {
            throw new CloudbreakRecipeSetupException("Recipe pre install failed: " + e.getMessage());
        }
    }

    public void executePreInstall(Stack stack, Set<HostMetadata> hostMetadata) throws CloudbreakSecuritySetupException {
        pluginManager.triggerAndWaitForPlugins(stack,
                ConsulPluginEvent.PRE_INSTALL,
                DEFAULT_RECIPE_TIMEOUT,
                AMBARI_AGENT,
                Collections.<String>emptyList(),
                getHostnames(hostMetadata));
    }

    public void executePostInstall(Stack stack) throws CloudbreakSecuritySetupException, CloudbreakRecipeSetupException {
        try {
            pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.POST_INSTALL, DEFAULT_RECIPE_TIMEOUT, AMBARI_AGENT);
        } catch (CloudbreakServiceException e) {
            throw new CloudbreakRecipeSetupException("Recipe post install failed: " + e.getMessage());
        }
    }

    public void executePostInstall(Stack stack, Set<HostMetadata> hostMetadata) throws CloudbreakSecuritySetupException {
        pluginManager.triggerAndWaitForPlugins(stack,
                ConsulPluginEvent.POST_INSTALL,
                DEFAULT_RECIPE_TIMEOUT,
                AMBARI_AGENT,
                Collections.<String>emptyList(),
                getHostnames(hostMetadata));
    }

    private Iterable<Recipe> getRecipesInHostGroups(Set<HostGroup> hostGroups) {
        return Iterables.concat(Collections2.transform(hostGroups, new Function<HostGroup, Set<Recipe>>() {
            @Nullable
            @Override
            public Set<Recipe> apply(@Nullable HostGroup input) {
                return input.getRecipes();
            }
        }));
    }

    private void setupProperties(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakSecuritySetupException {
        LOGGER.info("Setting up recipe properties.");
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIpWrapper());
        pluginManager.prepareKeyValues(clientConfig, getAllPropertiesFromRecipes(hostGroups));
    }

    private void installPlugins(Stack stack, Set<HostGroup> hostGroups, Set<InstanceMetaData> instances) throws CloudbreakSecuritySetupException {
        for (HostGroup hostGroup : hostGroups) {
            LOGGER.info("Installing plugins for recipes on hostgroup {}.", hostGroup.getName());
            installPluginsOnHosts(stack, hostGroup.getRecipes(), hostGroup.getHostMetadata(), instances, false);
        }
    }

    private void installPluginsOnHosts(Stack stack, Set<Recipe> recipes, Set<HostMetadata> hostMetadata, Set<InstanceMetaData> instances,
            boolean existingHostGroup) throws CloudbreakSecuritySetupException {
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIpWrapper());
        for (Recipe recipe : recipes) {
            Map<String, PluginExecutionType> plugins = new HashMap<>();
            for (Map.Entry<String, PluginExecutionType> entry : recipe.getPlugins().entrySet()) {
                String url = entry.getKey().startsWith("base64://") ? "consul://" + getPluginConsulKey(recipe, entry.getKey()) : entry.getKey();
                plugins.put(url, entry.getValue());
            }
            Map<String, Set<String>> eventIdMap = pluginManager.installPlugins(clientConfig, plugins, getHostnames(hostMetadata), existingHostGroup);
            pluginManager.waitForEventFinish(stack, instances, eventIdMap, recipe.getTimeout());
        }
    }

    private void cleanupPlugins(Stack stack, Set<HostGroup> hostGroups, Set<InstanceMetaData> instances) throws CloudbreakSecuritySetupException {
        for (HostGroup hostGroup : hostGroups) {
            LOGGER.info("Cleanup plugins on hostgroup {}.", hostGroup.getName());
            cleanupPluginsOnHosts(stack, hostGroup.getHostMetadata(), instances);
        }
    }

    private void cleanupPluginsOnHosts(Stack stack, Set<HostMetadata> hostMetadata, Set<InstanceMetaData> instances) throws CloudbreakSecuritySetupException {
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIpWrapper());
        Map<String, Set<String>> eventIdMap = pluginManager.cleanupPlugins(clientConfig, getHostnames(hostMetadata));
        pluginManager.waitForEventFinish(stack, instances, eventIdMap, DEFAULT_RECIPE_TIMEOUT);
    }

    private String getPluginConsulKey(Recipe recipe, String plugin) {
        return RECIPE_KEY_PREFIX + recipe.getName() + plugin.hashCode();
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
        return hostMetadata.stream().map(HostMetadata::getHostName).collect(Collectors.toSet());
    }

}
