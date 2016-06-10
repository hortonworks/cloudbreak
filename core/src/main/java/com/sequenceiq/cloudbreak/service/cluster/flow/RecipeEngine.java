package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT;
import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SWARM;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class RecipeEngine {

    public static final int DEFAULT_RECIPE_TIMEOUT = 15;
    private static final String SSSD_CONFIG = "sssd-config-";
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Inject
    private PluginManager pluginManager;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;
    @Resource
    private Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators;
    @Inject
    private RecipeBuilder recipeBuilder;
    @Inject
    private ConsulRecipeExecutor consulRecipeExecutor;
    @Inject
    private OrchestratorRecipeExecutor orchestratorRecipeExecutor;
    @Inject
    private BlueprintProcessor blueprintProcessor;

    public void executePreInstall(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        configureSssd(stack, null);
        addFsRecipes(stack, hostGroups);
        boolean recipesFound = recipesFound(hostGroups);
        if (recipesFound) {
            String orchestrator = stack.getOrchestrator().getType();
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator);
            if (orchestratorType.containerOrchestrator()) {
                consulRecipeExecutor.preInstall(stack, hostGroups);
            } else {
                orchestratorRecipeExecutor.preInstall(stack, hostGroups);
            }
        }
    }

    public void executeUpscalePreInstall(Stack stack, Set<HostMetadata> metaData) throws CloudbreakException {
        String orchestrator = stack.getOrchestrator().getType();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator);
        if (orchestratorType.containerOrchestrator()) {
            consulRecipeExecutor.executePreInstall(stack, metaData);
        } else {
            orchestratorRecipeExecutor.preInstall(stack);
        }
    }

    public void executePostInstall(Stack stack) throws CloudbreakException {
        String orchestrator = stack.getOrchestrator().getType();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator);
        if (orchestratorType.containerOrchestrator()) {
            consulRecipeExecutor.executePostInstall(stack);
        } else {
            orchestratorRecipeExecutor.postInstall(stack);
        }
    }

    public void executePostInstall(Stack stack, Set<HostMetadata> hostMetadata) throws CloudbreakException {
        String orchestrator = stack.getOrchestrator().getType();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator);
        if (orchestratorType.containerOrchestrator()) {
            consulRecipeExecutor.executePostInstall(stack, hostMetadata);
        } else {
            orchestratorRecipeExecutor.postInstall(stack);
        }
    }

    public void addFsRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        String orchestrator = stack.getOrchestrator().getType();
        if (SWARM.equals(orchestrator) || SALT.equals(orchestrator)) {
            Cluster cluster = stack.getCluster();
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            FileSystem fs = cluster.getFileSystem();
            if (fs != null) {
                FileSystemConfigurator fsConfigurator = fileSystemConfigurators.get(FileSystemType.valueOf(fs.getType()));
                List<RecipeScript> recipeScripts = fsConfigurator.getScripts();
                List<Recipe> fsRecipes = recipeBuilder.buildRecipes(recipeScripts, fs.getProperties());
                for (Recipe recipe : fsRecipes) {
                    boolean oneNode = false;
                    for (Map.Entry<String, ExecutionType> pluginEntries : recipe.getPlugins().entrySet()) {
                        if (ExecutionType.ONE_NODE.equals(pluginEntries.getValue())) {
                            oneNode = true;
                        }
                    }
                    if (oneNode) {
                        for (HostGroup hostGroup : hostGroups) {
                            if (isComponentPresent(blueprintText, "HDFS_CLIENT", hostGroup)) {
                                hostGroup.addRecipe(recipe);
                                break;
                            }
                        }
                    } else {
                        for (HostGroup hostGroup : hostGroups) {
                            hostGroup.addRecipe(recipe);
                        }
                    }
                }
            }
            addHDFSRecipe(cluster, blueprintText, hostGroups);
        }
    }

    public void configureSssd(Stack stack, Set<HostMetadata> hostMetadata) throws CloudbreakException {
        if (stack.getCluster().getSssdConfig() != null) {
            List<String> sssdPayload = generateSssdRecipePayload(stack);
            String orchestrator = stack.getOrchestrator().getType();
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator);
            if (orchestratorType.containerOrchestrator()) {
                consulRecipeExecutor.configureSssd(stack, hostMetadata, sssdPayload);
            } else {
                //TODO
            }
        }
    }

    public void installRecipes(Stack stack, HostGroup hostGroup, Set<HostMetadata> hostMetadata) throws CloudbreakException {
        if (recipesFound(Sets.newHashSet(hostGroup))) {
            String orchestrator = stack.getOrchestrator().getType();
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator);
            if (orchestratorType.containerOrchestrator()) {
                consulRecipeExecutor.setupRecipesOnHosts(stack, hostGroup.getRecipes(), hostMetadata);
            } else {
                LOGGER.info("Install recipe is not required in case of host orchestrator");
            }
        }
    }

    private List<String> generateSssdRecipePayload(Stack stack) throws CloudbreakSecuritySetupException {
        SssdConfig config = stack.getCluster().getSssdConfig();
        List<String> payload;
        if (config.getConfiguration() != null) {
            Map<String, String> keyValues = new HashMap<>();
            String configName = SSSD_CONFIG + config.getId();
            keyValues.put(configName, config.getConfiguration());
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), gatewayInstance.getPublicIpWrapper());
            pluginManager.prepareKeyValues(httpClientConfig, keyValues);
            payload = Arrays.asList(configName);
        } else {
            payload = Arrays.asList("-", config.getProviderType().getType(), config.getUrl(), config.getSchema().getRepresentation(),
                    config.getBaseSearch(), config.getTlsReqcert().getRepresentation(), config.getAdServer(),
                    config.getKerberosServer(), config.getKerberosRealm());
        }
        return payload;
    }

    private boolean recipesFound(Set<HostGroup> hostGroups) {
        for (HostGroup hostGroup : hostGroups) {
            if (!hostGroup.getRecipes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void addHDFSRecipe(Cluster cluster, String blueprintText, Set<HostGroup> hostGroups) {
        try {
            for (HostGroup hostGroup : hostGroups) {
                if (isComponentPresent(blueprintText, "HDFS_CLIENT", hostGroup)) {
                    String script = FileReaderUtils.readFileFromClasspath("scripts/hdfs-home.sh").replaceAll("\\$USER", cluster.getUserName());
                    RecipeScript recipeScript = new RecipeScript(script, ClusterLifecycleEvent.POST_INSTALL, ExecutionType.ONE_NODE);
                    Recipe recipe = recipeBuilder.buildRecipes(asList(recipeScript), Collections.emptyMap()).get(0);
                    hostGroup.addRecipe(recipe);
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot create HDFS home dir recipe", e);
        }
    }

    private boolean isComponentPresent(String blueprint, String component, HostGroup hostGroup) {
        return isComponentPresent(blueprint, component, Sets.newHashSet(hostGroup));
    }

    private boolean isComponentPresent(String blueprint, String component, Set<HostGroup> hostGroups) {
        for (HostGroup hostGroup : hostGroups) {
            Set<String> components = blueprintProcessor.getComponentsInHostGroup(blueprint, hostGroup.getName());
            if (components.contains(component)) {
                return true;
            }
        }
        return false;
    }

}
