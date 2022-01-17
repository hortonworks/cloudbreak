package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.POST_CLUSTER_INSTALL;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.PRE_CLOUDERA_MANAGER_START;
import static com.sequenceiq.cloudbreak.common.model.recipe.RecipeType.PRE_TERMINATION;
import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import com.sequenceiq.common.api.type.InstanceGroupType;

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

    public void uploadUpscaleRecipes(Stack stack, Set<HostGroup> targetHostGroups, Set<HostGroup> allHostGroups)
            throws CloudbreakException {
        if (recipesFound(targetHostGroups)) {
            Map<HostGroup, List<RecipeModel>> recipeModels = recipeTemplateService.createRecipeModels(stack, allHostGroups);
            Map<HostGroup, Set<GeneratedRecipe>> generatedRecipeTemplates = recipeTemplateService.createGeneratedRecipes(recipeModels,
                    getRecipeNameMap(allHostGroups), stack.getWorkspace());
            if (targetHostGroups.stream().anyMatch(hostGroup -> hostGroup.getInstanceGroup().getInstanceGroupType() == InstanceGroupType.GATEWAY)) {
                orchestratorRecipeExecutor.uploadRecipes(stack, recipeModels);
            }
            measure(() -> recipeTemplateService.updateAllGeneratedRecipes(targetHostGroups, generatedRecipeTemplates), LOGGER,
                    "Updating all the generated recipes took {} ms");
        } else {
            LOGGER.debug("Not found any recipes for host groups '{}'. No recipe uploaad will happen during upscale.", targetHostGroups);
        }
    }

    public void executePreClusterManagerRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Collection<Recipe> recipes = hostGroupService.getRecipesByHostGroups(hostGroups);
        if (shouldExecuteRecipeOnStack(recipes, PRE_CLOUDERA_MANAGER_START)) {
            uploadRecipesIfNeeded(stack, hostGroups);
            orchestratorRecipeExecutor.preClusterManagerStartRecipes(stack);
        }
    }

    // note: executed when LDAP config is present, because later the LDAP sync is hooked for this salt state in the top.sls.
    public void executePostAmbariStartRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Collection<Recipe> recipes = hostGroupService.getRecipesByHostGroups(hostGroups);
        if ((stack.getCluster() != null && ldapConfigService.isLdapConfigExistsForEnvironment(stack.getEnvironmentCrn(), stack.getName()))
                || recipesFound(recipes, POST_CLOUDERA_MANAGER_START)) {
            uploadRecipesIfNeeded(stack, hostGroups);
            orchestratorRecipeExecutor.postClusterManagerStartRecipes(stack);
        }
    }

    public void executePostInstallRecipes(Stack stack, Set<HostGroup> hostGroups) throws CloudbreakException {
        Collection<Recipe> recipes = hostGroupService.getRecipesByHostGroups(hostGroups);
        if (shouldExecuteRecipeOnStack(recipes, POST_CLUSTER_INSTALL)) {
            uploadRecipesIfNeeded(stack, hostGroups);
        }
        orchestratorRecipeExecutor.postClusterInstall(stack);
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
        boolean recipesFound = recipesFound(hostGroups);
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

    private boolean shouldExecuteRecipeOnStack(Collection<Recipe> recipes, RecipeType recipeType) {
        return recipesFound(recipes, recipeType);
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

    private Map<String, Recipe> getRecipeNameMap(Set<HostGroup> hostGroups) {
        return hostGroups.stream()
                .flatMap(hg -> hg.getRecipes().stream())
                .collect(Collectors.toMap(Recipe::getName, recipe -> recipe, (r1, r2) -> r1));
    }
}
