package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.POST_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.PRE_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.PRE_TERMINATION;
import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class RecipeEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeEngine.class);

    @Inject
    private OrchestratorRecipeExecutor orchestratorRecipeExecutor;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private RecipeTemplateService recipeTemplateService;

    public void uploadRecipes(Long stackId) throws CloudbreakException {
        Stack stack = measure(() -> stackService.getByIdWithListsInTransaction(stackId), LOGGER,
                "stackService.getByIdWithListsInTransaction() took {} ms");
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Upload recipes started for stack with name {}", stack.getName());
        stack.setResources(measure(() -> resourceService.getNotInstanceRelatedByStackId(stackId), LOGGER,
                "resourceService.getNotInstanceRelatedByStackId() took {} ms"));
        Set<HostGroup> hostGroups = measure(() -> hostGroupService.getByClusterWithRecipes(stack.getCluster().getId()), LOGGER,
                "hostGroupService.getByClusterWithRecipes() took {} ms");
        Map<HostGroup, List<RecipeModel>> recipeModels = recipeTemplateService.createRecipeModels(stack, hostGroups);
        uploadRecipesOnHostGroups(stack, hostGroups, recipeModels);
        LOGGER.info("Upload recipes finished successfully for stack with name {}", stack.getName());
    }

    public void executePreClusterManagerRecipes(Stack stack, Map<String, String> candidateAddresses, Set<HostGroup> hostGroups) throws CloudbreakException {
        Collection<Recipe> recipes = hostGroupService.getRecipesByHostGroups(hostGroups);
        if (shouldExecuteRecipeOnStack(recipes, PRE_CLOUDERA_MANAGER_START, PRE_SERVICE_DEPLOYMENT)) {
            uploadRecipesIfNeeded(stack, hostGroups);
            if (MapUtils.isEmpty(candidateAddresses)) {
                orchestratorRecipeExecutor.preClusterManagerStartRecipes(stack);
            } else {
                orchestratorRecipeExecutor.preClusterManagerStartRecipesOnTargets(stack, candidateAddresses);
            }
        }
    }

    public void executePostClouderaManagerStartRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Collection<Recipe> recipes = hostGroupService.getRecipesByHostGroups(hostGroups);
        if (shouldExecutePostClouderaManagerStartRecipeOnStack(stack, recipes)) {
            uploadRecipesIfNeeded(stack, hostGroups);
            orchestratorRecipeExecutor.postClusterManagerStartRecipes(stack);
        }
    }

    public void executePostClouderaManagerStartRecipesOnTargets(Stack stack, Set<HostGroup> hostGroups, Map<String, String> candidateAddresses)
            throws CloudbreakException {
        Collection<Recipe> recipes = hostGroupService.getRecipesByHostGroups(hostGroups);
        if (shouldExecutePostClouderaManagerStartRecipeOnStack(stack, recipes)) {
            uploadRecipesIfNeeded(stack, hostGroups);
            orchestratorRecipeExecutor.postClusterManagerStartRecipesOnTargets(stack, candidateAddresses);
        }
    }

    public void executePostInstallRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Collection<Recipe> recipes = hostGroupService.getRecipesByHostGroups(hostGroups);
        if (shouldExecuteRecipeOnStack(recipes, POST_CLUSTER_INSTALL, POST_SERVICE_DEPLOYMENT)) {
            uploadRecipesIfNeeded(stack, hostGroups);
        }
        // Post cluster install should be executed even if we don't have post install recipe because we have the internal createuserhome.sh recipe
        orchestratorRecipeExecutor.postClusterInstall(stack);
    }

    public void executePostInstallRecipesOnTargets(Stack stack, Set<HostGroup> hostGroups, Map<String, String> candidateAddresses) throws CloudbreakException {
        Collection<Recipe> recipes = hostGroupService.getRecipesByHostGroups(hostGroups);
        if (shouldExecuteRecipeOnStack(recipes, POST_CLUSTER_INSTALL, POST_SERVICE_DEPLOYMENT)) {
            uploadRecipesIfNeeded(stack, hostGroups);
        }
        // Post cluster install should be executed even if we don't have post install recipe because we have the internal createuserhome.sh recipe
        orchestratorRecipeExecutor.postClusterInstallOnTargets(stack, candidateAddresses);
    }

    public void executePreTerminationRecipes(Stack stack, Set<HostGroup> hostGroups, boolean forced) throws CloudbreakException {
        if (shouldExecutePreTerminationWithUploadRecipes(stack, hostGroups)) {
            orchestratorRecipeExecutor.preTerminationRecipes(stack, forced);
        }
    }

    public void executePreTerminationRecipes(Stack stack, Set<HostGroup> hostGroups, Set<String> hostNames) throws CloudbreakException {
        if (shouldExecutePreTerminationWithUploadRecipes(stack, hostGroups)) {
            orchestratorRecipeExecutor.preTerminationRecipes(stack, hostNames);
        }
    }

    // note: executed when LDAP config is present, because later the LDAP sync is hooked for this salt state in the top.sls.
    private boolean shouldExecutePostClouderaManagerStartRecipeOnStack(Stack stack, Collection<Recipe> recipes) {
        return (stack.getCluster() != null && ldapConfigService.isLdapConfigExistsForEnvironment(stack.getEnvironmentCrn(), stack.getName()))
                || recipesFound(recipes, POST_CLOUDERA_MANAGER_START);
    }

    private boolean shouldExecutePreTerminationWithUploadRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Collection<Recipe> recipes = hostGroupService.getRecipesByHostGroups(hostGroups);
        boolean shouldExecutePreTermination = shouldExecuteRecipeOnStack(recipes, PRE_TERMINATION);
        if (shouldExecutePreTermination) {
            if (stack.getCluster() == null) {
                throw new NotFoundException("Cluster does not found, pre-termination will not be run.");
            }
            if (recipeTemplateService.hasAnyTemplateInRecipes(hostGroups)) {
                LOGGER.warn("No any update of pre termination recipes has been implemented with using templates. Skip updating recipes.");
            } else {
                uploadRecipesIfNeeded(stack, hostGroups);
            }
        }
        return shouldExecutePreTermination;
    }

    private void uploadRecipesIfNeeded(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Map<HostGroup, List<RecipeModel>> recipeModels = recipeTemplateService.createRecipeModels(stack, hostGroups);
        boolean generatedRecipesMatch = recipeTemplateService.isGeneratedRecipesInDbStale(hostGroups, recipeModels);
        if (generatedRecipesMatch) {
            LOGGER.debug("Generated recipes are matched for host group recipes, no need to upload them.");
        } else {
            uploadRecipesOnHostGroups(stack, hostGroups, recipeModels);
        }
    }

    private void uploadRecipesOnHostGroups(Stack stack, Set<HostGroup> hostGroups, Map<HostGroup, List<RecipeModel>> recipeModels) throws CloudbreakException {
        boolean recipesFound = recipesOrGeneratedRecipesFound(hostGroups);
        if (recipesFound) {
            Map<HostGroup, Set<GeneratedRecipe>> generatedRecipeTemplates = recipeTemplateService.createGeneratedRecipes(recipeModels,
                    getRecipeNameMap(hostGroups), stack.getWorkspace());
            checkedMeasure(() -> orchestratorRecipeExecutor.uploadRecipes(stack, recipeModels), LOGGER, "Upload recipes took {} ms");
            measure(() -> recipeTemplateService.updateAllGeneratedRecipes(hostGroups, generatedRecipeTemplates), LOGGER,
                    "Updating all the generated recipes took {} ms");
        } else {
            LOGGER.debug("Not found any recipes for host groups in stack '{}'", stack.getName());
        }
    }

    private boolean shouldExecuteRecipeOnStack(Collection<Recipe> recipes, RecipeType... recipeTypes) {
        return recipesFound(recipes, recipeTypes);
    }

    private boolean recipesOrGeneratedRecipesFound(Iterable<HostGroup> hostGroups) {
        for (HostGroup hostGroup : hostGroups) {
            if (!hostGroup.getRecipes().isEmpty()) {
                return true;
            } else if (!hostGroup.getGeneratedRecipes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean recipesFound(Collection<Recipe> recipes, RecipeType... recipeTypes) {
        return recipes.stream().anyMatch(recipe -> Arrays.stream(recipeTypes).anyMatch(recipeType -> recipeType.equals(recipe.getRecipeType())));
    }

    private Map<String, Recipe> getRecipeNameMap(Set<HostGroup> hostGroups) {
        return hostGroups.stream()
                .flatMap(hg -> hg.getRecipes().stream())
                .collect(Collectors.toMap(Recipe::getName, recipe -> recipe, (r1, r2) -> r1));
    }
}
