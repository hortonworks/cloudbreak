package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static com.sequenceiq.cloudbreak.api.model.RecipeType.POST_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.PRE_AMBARI_START;
import static com.sequenceiq.cloudbreak.api.model.RecipeType.PRE_TERMINATION;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Component
public class RecipeEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private OrchestratorRecipeExecutor orchestratorRecipeExecutor;

    public void uploadRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (recipesSupportedOnOrchestrator(orchestrator)) {
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
                if (hostGroup.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                    orchestratorRecipeExecutor.uploadRecipes(stack, hostGroups);
                }
            }
        }
    }

    public void executePreAmbariStartRecipes(Stack stack, Collection<Recipe> recipes) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldExecuteRecipeOnStack(recipes, orchestrator, PRE_AMBARI_START)) {
            orchestratorRecipeExecutor.preAmbariStartRecipes(stack);
        }
    }

    // note: executed when LDAP config is present, because later the LDAP sync is hooked for this salt state in the top.sls.
    public void executePostAmbariStartRecipes(Stack stack, Collection<Recipe> recipes) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if ((stack.getCluster() != null && stack.getCluster().getLdapConfig() != null) || recipesFound(recipes, POST_AMBARI_START)
                && recipesSupportedOnOrchestrator(orchestrator)) {
            orchestratorRecipeExecutor.postAmbariStartRecipes(stack);
        }
    }

    public void executePostInstallRecipes(Stack stack) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldRunConfiguredAndDefaultRecipes(orchestrator)) {
            orchestratorRecipeExecutor.postClusterInstall(stack);
        }
    }

    public void executePreTerminationRecipes(Stack stack, Collection<Recipe> recipes) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldExecuteRecipeOnStack(recipes, orchestrator, PRE_TERMINATION)) {
            orchestratorRecipeExecutor.preTerminationRecipes(stack);
        }
    }

    public void executePreTerminationRecipes(Stack stack, Collection<Recipe> recipes, Set<String> hostNames) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldExecuteRecipeOnStack(recipes, orchestrator, PRE_TERMINATION)) {
            orchestratorRecipeExecutor.preTerminationRecipes(stack, hostNames);
        }
    }

    private boolean shouldExecuteRecipeOnStack(Collection<Recipe> recipes, Orchestrator orchestrator, RecipeType recipeType) throws CloudbreakException {
        return (recipesFound(recipes, recipeType)) && recipesSupportedOnOrchestrator(orchestrator);
    }

    private boolean recipesFound(Iterable<HostGroup> hostGroups) {
        for (HostGroup hostGroup : hostGroups) {
            if (!hostGroup.getRecipes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean recipesFound(Collection<Recipe> recipes, RecipeType recipeType) {
        return recipes.stream().anyMatch(recipe -> recipeType.equals(recipe.getRecipeType()));
    }

    private boolean shouldRunConfiguredAndDefaultRecipes(Orchestrator orchestrator) throws CloudbreakException {
        return recipesSupportedOnOrchestrator(orchestrator);
    }

    private boolean recipesSupportedOnOrchestrator(Orchestrator orchestrator) throws CloudbreakException {
        return !orchestratorTypeResolver.resolveType(orchestrator).containerOrchestrator();
    }
}
