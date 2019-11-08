package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.PRE_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.PRE_TERMINATION;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Component
public class RecipeEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private OrchestratorRecipeExecutor orchestratorRecipeExecutor;

    @Inject
    private LdapConfigService ldapConfigService;

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

    public void executePreClusterManagerRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldExecuteRecipeOnStack(hostGroups, orchestrator, PRE_CLOUDERA_MANAGER_START)) {
            orchestratorRecipeExecutor.preClusterManagerStartRecipes(stack);
        }
    }

    // note: executed when LDAP config is present, because later the LDAP sync is hooked for this salt state in the top.sls.
    public void executePostAmbariStartRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if ((stack.getCluster() != null && ldapConfigService.isLdapConfigExistsForEnvironment(stack.getEnvironmentCrn(), stack.getName()))
                || recipesFound(hostGroups, POST_CLOUDERA_MANAGER_START) && recipesSupportedOnOrchestrator(orchestrator)) {
            orchestratorRecipeExecutor.postClusterManagerStartRecipes(stack);
        }
    }

    public void executePostInstallRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldRunConfiguredAndDefaultRecipes(orchestrator)) {
            orchestratorRecipeExecutor.postClusterInstall(stack);
        }
    }

    public void executePreTerminationRecipes(Stack stack, Set<HostGroup> hostGroups, boolean forced) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        if (shouldExecuteRecipeOnStack(hostGroups, orchestrator, PRE_TERMINATION)) {
            orchestratorRecipeExecutor.preTerminationRecipes(stack, forced);
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

    private boolean shouldRunConfiguredAndDefaultRecipes(Orchestrator orchestrator) throws CloudbreakException {
        return recipesSupportedOnOrchestrator(orchestrator);
    }

    private boolean recipesSupportedOnOrchestrator(Orchestrator orchestrator) throws CloudbreakException {
        return !orchestratorTypeResolver.resolveType(orchestrator).containerOrchestrator();
    }
}
