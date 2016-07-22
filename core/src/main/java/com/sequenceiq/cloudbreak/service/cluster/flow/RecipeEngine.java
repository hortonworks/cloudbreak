package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SALT;
import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SWARM;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.SmartSenseConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigurator;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class RecipeEngine {

    public static final int DEFAULT_RECIPE_TIMEOUT = 15;
    private static final String SSSD_CONFIG = "sssd-config-";
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;
    @Resource
    private Map<FileSystemType, FileSystemConfigurator> fileSystemConfigurators;
    @Inject
    private RecipeBuilder recipeBuilder;
    @Inject
    private OrchestratorRecipeExecutor orchestratorRecipeExecutor;
    @Inject
    private BlueprintProcessor blueprintProcessor;
    @Inject
    private SmartSenseConfigProvider smartSenseConfigProvider;

    public void executePreInstall(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (recipesSupportedOnOrchestrator(orchestrator)) {
            configureSssd(stack, null);
            addFsRecipes(stack, hostGroups);
            addSmartSenseRecipe(stack, hostGroups);
            boolean recipesFound = recipesFound(hostGroups);
            if (recipesFound) {
                orchestratorRecipeExecutor.uploadRecipes(stack, hostGroups);
                orchestratorRecipeExecutor.preInstall(stack);
            }
        }
    }

    public void executeUpscalePreInstall(Stack stack, HostGroup hostGroup, Set<HostMetadata> metaData) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (recipesSupportedOnOrchestrator(orchestrator)) {
            Set<HostGroup> hostGroups = Collections.singleton(hostGroup);
            configureSssd(stack, metaData);
            addFsRecipes(stack, hostGroups);
            boolean recipesFound = recipesFound(hostGroups);
            if (recipesFound) {
                orchestratorRecipeExecutor.preInstall(stack);
            }
        }
    }

    public void executePostInstall(Stack stack) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (recipesSupportedOnOrchestrator(orchestrator)) {
            orchestratorRecipeExecutor.postInstall(stack);
        }
    }

    public void executeUpscalePostInstall(Stack stack, Set<HostMetadata> hostMetadata) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (recipesSupportedOnOrchestrator(orchestrator)) {
            orchestratorRecipeExecutor.postInstall(stack);
        }
    }

    private void addFsRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        String orchestrator = stack.getOrchestrator().getType();
        if (SWARM.equals(orchestrator) || SALT.equals(orchestrator)) {
            Cluster cluster = stack.getCluster();
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            FileSystem fs = cluster.getFileSystem();
            if (fs != null) {
                try {
                    addFsRecipesToHostGroups(hostGroups, blueprintText, fs);
                } catch (IOException e) {
                    throw new CloudbreakException("can not add FS recipes to host groups", e);
                }
            }
            addHDFSRecipe(cluster, blueprintText, hostGroups);
        }
    }

    private void addFsRecipesToHostGroups(Set<HostGroup> hostGroups, String blueprintText, FileSystem fs) throws IOException {
        FileSystemConfigurator fsConfigurator = fileSystemConfigurators.get(FileSystemType.valueOf(fs.getType()));
        FileSystemConfiguration fsConfiguration = getFileSystemConfiguration(fs);
        List<RecipeScript> recipeScripts = fsConfigurator.getScripts(fsConfiguration);
        List<Recipe> fsRecipes = recipeBuilder.buildRecipes(recipeScripts, fs.getProperties());
        for (Recipe recipe : fsRecipes) {
            for (HostGroup hostGroup : hostGroups) {
                hostGroup.addRecipe(recipe);
            }
        }
    }

    private FileSystemConfiguration getFileSystemConfiguration(FileSystem fs) throws IOException {
        String json = JsonUtil.writeValueAsString(fs.getProperties());
        return (FileSystemConfiguration) JsonUtil.readValue(json, FileSystemType.valueOf(fs.getType()).getClazz());
    }

    private void configureSssd(Stack stack, Set<HostMetadata> hostMetadata) throws CloudbreakException {
        if (stack.getCluster().getSssdConfig() != null) {
            List<String> sssdPayload = generateSssdRecipePayload(stack);
            // TODO implementation required
            throw new IllegalStateException("implementation required");
        }
    }

    private List<String> generateSssdRecipePayload(Stack stack) throws CloudbreakSecuritySetupException {
        SssdConfig config = stack.getCluster().getSssdConfig();
        List<String> payload;
        if (config.getConfiguration() != null) {
            String configName = SSSD_CONFIG + config.getId();
                    payload = Collections.singletonList(configName);
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
                if (isComponentPresent(blueprintText, "NAMENODE", hostGroup)) {
                    String script = FileReaderUtils.readFileFromClasspath("scripts/hdfs-home.sh").replaceAll("\\$USER", cluster.getUserName());
                    RecipeScript recipeScript = new RecipeScript(script, ClusterLifecycleEvent.POST_INSTALL);
                    Recipe recipe = recipeBuilder.buildRecipes(Collections.singletonList(recipeScript), Collections.emptyMap()).get(0);
                    hostGroup.addRecipe(recipe);
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot create HDFS home dir recipe", e);
        }
    }

    private void addSmartSenseRecipe(Stack stack, Set<HostGroup> hostGroups) {
        try {
            Cluster cluster = stack.getCluster();
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            if (smartSenseConfigProvider.smartSenseIsConfigurable(blueprintText)) {
                for (HostGroup hostGroup : hostGroups) {
                    if (isComponentPresent(blueprintText, "HST_AGENT", hostGroup)) {
                        String script = FileReaderUtils.readFileFromClasspath("scripts/smartsense-capture-schedule.sh");
                        RecipeScript recipeScript = new RecipeScript(script, ClusterLifecycleEvent.POST_INSTALL);
                        Recipe recipe = recipeBuilder.buildRecipes(Collections.singletonList(recipeScript), Collections.emptyMap()).get(0);
                        hostGroup.addRecipe(recipe);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot create SmartSense caputre schedule setter recipe", e);
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

    private boolean recipesSupportedOnOrchestrator(Orchestrator orchestrator) {
        return !OrchestratorConstants.MARATHON.equals(orchestrator.getType());
    }
}
