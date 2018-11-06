package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static com.sequenceiq.cloudbreak.api.model.RecipeType.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.PRE_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.PRE_TERMINATION;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.SmartsenseConfigurationLocator;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeScript;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class RecipeEngine {

    public static final Set<String> DEFAULT_RECIPES = Collections.unmodifiableSet(
            Sets.newHashSet("hdfs-home", "smartsense-capture-schedule", "prepare-s3-symlinks"));

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private RecipeBuilder recipeBuilder;

    @Inject
    private OrchestratorRecipeExecutor orchestratorRecipeExecutor;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private SmartsenseConfigurationLocator smartsenseConfigurationLocator;

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Inject
    private VaultService vaultService;

    public void uploadRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (recipesSupportedOnOrchestrator(orchestrator)) {
            String blueprintText = vaultService.resolveSingleValue(stack.getCluster().getBlueprint().getBlueprintText());
            addHDFSRecipe(stack, blueprintText, hostGroups);
            addSmartSenseRecipe(stack, blueprintText, hostGroups);
            addS3SymlinkRecipe(stack, blueprintText, hostGroups);
            addContainerExecutorScripts(stack, hostGroups);
            boolean recipesFound = recipesFound(hostGroups);
            if (recipesFound) {
                orchestratorRecipeExecutor.uploadRecipes(stack, hostGroups);
            }
        }
    }

    public void uploadUpscaleRecipes(Stack stack, HostGroup hostGroup, Set<HostGroup> hostGroups)
            throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (recipesSupportedOnOrchestrator(orchestrator)) {
            Set<HostGroup> hgs = Collections.singleton(hostGroup);
            if (recipesFound(hgs)) {
                if (hostGroup.getConstraint().getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                    orchestratorRecipeExecutor.uploadRecipes(stack, hostGroups);
                }
            }
        }
    }

    public void executePreAmbariStartRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldExecuteRecipeOnStack(hostGroups, orchestrator, PRE_AMBARI_START)) {
            orchestratorRecipeExecutor.preAmbariStartRecipes(stack);
        }
    }

    // note: executed when LDAP config is present, because later the LDAP sync is hooked for this salt state in the top.sls.
    public void executePostAmbariStartRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if ((stack.getCluster() != null && stack.getCluster().getLdapConfig() != null) || recipesFound(hostGroups, POST_AMBARI_START)
                && recipesSupportedOnOrchestrator(orchestrator)) {
            orchestratorRecipeExecutor.postAmbariStartRecipes(stack);
        }
    }

    public void executePostInstallRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldRunConfiguredAndDefaultRecipes(orchestrator)) {
            orchestratorRecipeExecutor.postClusterInstall(stack);
        }
    }

    public void executePreTerminationRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldExecuteRecipeOnStack(hostGroups, orchestrator, PRE_TERMINATION)) {
            orchestratorRecipeExecutor.preTerminationRecipes(stack);
        }
    }

    public void executePreTerminationRecipes(Stack stack, Set<HostGroup> hostGroups, Set<String> hostNames) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldExecuteRecipeOnStack(hostGroups, orchestrator, PRE_TERMINATION)) {
            orchestratorRecipeExecutor.preTerminationRecipes(stack, hostNames);
        }
    }

    private boolean shouldExecuteRecipeOnStack(Set<HostGroup> hostGroups, Orchestrator orchestrator, RecipeType recipeType) throws CloudbreakException {
        return (recipesFound(hostGroups, recipeType)) && recipesSupportedOnOrchestrator(orchestrator);
    }

    private void addContainerExecutorScripts(Stack stack, Set<HostGroup> hostGroups) {
        try {
            Cluster cluster = stack.getCluster();
            if (cluster != null && ExecutorType.CONTAINER.equals(cluster.getExecutorType())) {
                for (HostGroup hostGroup : hostGroups) {
                    String script = FileReaderUtils.readFileFromClasspath("scripts/configure-container-executor.sh");
                    RecipeScript recipeScript = new RecipeScript(script, POST_CLUSTER_INSTALL);
                    Recipe recipe = recipeBuilder.buildRecipes("getConfigurationEntries-container-executor",
                            Collections.singletonList(recipeScript)).get(0);
                    hostGroup.addRecipe(recipe);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot getConfigurationEntries container executor", e);
        }
    }

    private boolean recipesFound(Iterable<HostGroup> hostGroups) {
        for (HostGroup hostGroup : hostGroups) {
            if (!hostGroup.getRecipes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean recipesFound(Iterable<HostGroup> hostGroups, RecipeType recipeType) {
        for (HostGroup hostGroup : hostGroups) {
            for (Recipe recipe : hostGroup.getRecipes()) {
                if (recipe.getRecipeType() == recipeType) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addHDFSRecipe(Stack stack, String blueprintText, Set<HostGroup> hostGroups) {
        try {
            Cluster cluster = stack.getCluster();
            for (HostGroup hostGroup : hostGroups) {
                if (isComponentPresent(blueprintText, "NAMENODE", hostGroup)) {
                    String userName = vaultService.resolveSingleValue(cluster.getUserName());
                    String script = FileReaderUtils.readFileFromClasspath("scripts/hdfs-home.sh").replaceAll("\\$USER", userName);
                    RecipeScript recipeScript = new RecipeScript(script, POST_CLUSTER_INSTALL);
                    Recipe recipe = recipeBuilder.buildRecipes("hdfs-home", Collections.singletonList(recipeScript)).get(0);
                    hostGroup.addRecipe(recipe);
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot create HDFS home dir recipe", e);
        }
    }

    private void addS3SymlinkRecipe(Stack stack, String blueprintText, Set<HostGroup> hostGroups) {
        try {
            Cluster cluster = stack.getCluster();
            for (HostGroup hostGroup : hostGroups) {
                if (isComponentPresent(blueprintText, "ATLAS_SERVER", hostGroup)) {
                    String script = FileReaderUtils.readFileFromClasspath("scripts/prepare-s3-symlinks.sh")
                            .replaceAll("\\$AMBARI_USER", vaultService.resolveSingleValue(cluster.getUserName()))
                            .replaceAll("\\$AMBARI_IP", getAmbariPrivateIp(stack))
                            .replaceAll("\\$AMBARI_PASSWORD", vaultService.resolveSingleValue(cluster.getPassword()))
                            .replaceAll("\\$CLUSTER_NAME", cluster.getName());
                    RecipeScript recipeScript = new RecipeScript(script, RecipeType.POST_CLUSTER_INSTALL);
                    Recipe recipe = recipeBuilder.buildRecipes("prepare-s3-symlinks", Collections.singletonList(recipeScript)).get(0);
                    hostGroup.addRecipe(recipe);
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot create s3 symlinks recipe", e);
        }
    }

    private String getAmbariPrivateIp(Stack stack) {
        String result = null;
        for (InstanceGroup ig : stack.getInstanceGroups()) {
            if (InstanceGroupType.isGateway(ig.getInstanceGroupType())) {
                InstanceMetaData imd = ig.getInstanceMetaDataSet().iterator().next();
                result = imd.getPrivateIp();
            }
        }
        return result;
    }

    private void addSmartSenseRecipe(Stack stack, String blueprintText, Set<HostGroup> hostGroups) {
        try {
            Cluster cluster = stack.getCluster();
            if (smartsenseConfigurationLocator.smartsenseConfigurable(smartSenseSubscriptionService.getDefault())) {
                for (HostGroup hostGroup : hostGroups) {
                    if (isComponentPresent(blueprintText, "HST_AGENT", hostGroup)) {
                        String script = FileReaderUtils.readFileFromClasspath("scripts/smartsense-capture-schedule.sh");
                        RecipeScript recipeScript = new RecipeScript(script, POST_CLUSTER_INSTALL);
                        Recipe recipe = recipeBuilder.buildRecipes("smartsense-capture-schedule",
                                Collections.singletonList(recipeScript)).get(0);
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
            Set<String> components = blueprintProcessorFactory.get(blueprint).getComponentsInHostGroup(hostGroup.getName());
            if (components.contains(component)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldRunConfiguredAndDefaultRecipes(Orchestrator orchestrator) throws CloudbreakException {
        return recipesSupportedOnOrchestrator(orchestrator);
    }

    private boolean recipesSupportedOnOrchestrator(Orchestrator orchestrator) throws CloudbreakException {
        return !orchestratorTypeResolver.resolveType(orchestrator).containerOrchestrator();
    }
}
