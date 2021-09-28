package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.recipe.CentralRecipeUpdater;
import com.sequenceiq.cloudbreak.service.recipe.GeneratedRecipeService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

import io.micrometer.core.instrument.util.StringUtils;

@Component
public class RecipeTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTemplateService.class);

    private final GeneratedRecipeService generatedRecipeService;

    private final CentralRecipeUpdater centralRecipeUpdater;

    private final StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    private final TransactionService transactionService;

    public RecipeTemplateService(GeneratedRecipeService generatedRecipeService, CentralRecipeUpdater centralRecipeUpdater,
            StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter, TransactionService transactionService) {
        this.generatedRecipeService = generatedRecipeService;
        this.centralRecipeUpdater = centralRecipeUpdater;
        this.stackToTemplatePreparationObjectConverter = stackToTemplatePreparationObjectConverter;
        this.transactionService = transactionService;
    }

    /**
     * Compare generated recipes from the database against on-the-fly generated recipes for every host groups
     * If any of those will differ from the source (in database) or no recipe relation found for generated recipe, the result will be false.
     */
    public boolean compareGeneratedRecipes(Set<HostGroup> hostGroups, Map<HostGroup, List<RecipeModel>> generatedModels) {
        for (HostGroup hostGroup : hostGroups) {
            Set<GeneratedRecipe> generatedRecipes = hostGroup.getGeneratedRecipes();
            boolean hasRecipes = CollectionUtils.isNotEmpty(hostGroup.getRecipes());
            boolean hasGeneratedRecipes = CollectionUtils.isNotEmpty(generatedRecipes);
            boolean recipeModelsContainsHostGroup = MapUtils.isNotEmpty(generatedModels) && generatedModels.containsKey(hostGroup);
            if (hasRecipes && !hasGeneratedRecipes) {
                LOGGER.debug("No generated recipes found for host group '{}', but it has recipes. Recipes should be uploaded and regenerated.",
                        hostGroup.getName());
                return false;
            } else if (!hasGeneratedRecipes) {
                LOGGER.debug("No source and generated recipes found for host group '{}', skip comparing source and generated recipes.", hostGroup.getName());
                continue;
            } else if (!recipeModelsContainsHostGroup) {
                LOGGER.debug("Generated recipe models do not contain host group {}. Recipes should be regenerated.", hostGroup.getName());
                return false;
            }
            boolean anyWithoutRecipeSource = generatedRecipes.stream().anyMatch(g -> g.getRecipe() == null);
            if (anyWithoutRecipeSource) {
                LOGGER.debug("Not found recipe source for generated recipe. Recipes should be uploaded and regenerated.");
                return false;
            }
            Map<String, GeneratedRecipe> generatedRecipeNameMap = generatedRecipes.stream()
                    .collect(Collectors.toMap(
                            g -> g.getRecipe().getName(), g -> g
                    ));
            List<RecipeModel> recipeModelList = generatedModels.get(hostGroup);
            if (generatedRecipes.size() != recipeModelList.size()) {
                LOGGER.debug("Source and generated recipe counts are not matching for host group '{}'. Recipes should be uploaded and regenerated.",
                        hostGroup.getName());
                return false;
            }
            if (compareHostGroupGeneratedRecipes(hostGroup, generatedRecipeNameMap, recipeModelList)) {
                LOGGER.debug("Recipes matches for host group '{}'", hostGroup.getName());
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Generate recipe model objects (for every host group) from provided stack and host group inputs with stack template object.
     */
    public Map<HostGroup, List<RecipeModel>> createRecipeModels(Stack stack, Set<HostGroup> hostGroups) {
        Set<HostGroup> hostGroupsWithRecipes = hostGroups.stream().filter(hg -> !hg.getRecipes().isEmpty()).collect(Collectors.toSet());
        if (hasAnyTemplateInRecipes(hostGroupsWithRecipes)) {
            TemplatePreparationObject templatePreparationObject = measure(() -> stackToTemplatePreparationObjectConverter.convert(stack),
                    LOGGER, "Template prepartion object generation took {} ms for recipes generation.");
            return hostGroupsWithRecipes.stream().collect(Collectors.toMap(h -> h, h -> convert(h.getRecipes(), templatePreparationObject)));
        } else {
            return hostGroupsWithRecipes.stream().collect(Collectors.toMap(h -> h, h -> convert(h.getRecipes())));
        }
    }

    /**
     * Decides that template preparation object should be used for generating the recipe content.
     */
    public boolean hasAnyTemplateInRecipes(Set<HostGroup> hostGroups) {
        return hostGroups.stream()
                .filter(hg -> CollectionUtils.isNotEmpty(hg.getRecipes()))
                .flatMap(hg -> hg.getRecipes().stream())
                .anyMatch(r -> StringUtils.isNotBlank(r.getContent()) && new String(Base64.decodeBase64(r.getContent())).contains("{{{"));
    }

    /**
     * Create generated recipe objects based on host group recipe models.
     */
    public Map<HostGroup, Set<GeneratedRecipe>> createGeneratedRecipes(Map<HostGroup, List<RecipeModel>> recipeModels, Map<String, Recipe> recipesNameMap,
            Workspace workspace) {
        Map<HostGroup, Set<GeneratedRecipe>> result = new HashMap<>();
        for (Map.Entry<HostGroup, List<RecipeModel>> hostGroupListEntry : recipeModels.entrySet()) {
            Set<GeneratedRecipe> generatedRecipes = new HashSet<>();
            for (RecipeModel recipeModel : hostGroupListEntry.getValue()) {
                GeneratedRecipe generatedRecipe = new GeneratedRecipe();
                generatedRecipe.setWorkspace(workspace);
                generatedRecipe.setHostGroup(hostGroupListEntry.getKey());
                generatedRecipe.setExtendedRecipeText(recipeModel.getGeneratedScript());
                generatedRecipe.setRecipe(recipesNameMap.get(recipeModel.getName()));
                generatedRecipes.add(generatedRecipe);
            }
            result.put(hostGroupListEntry.getKey(), generatedRecipes);
        }
        return result;
    }

    public void updateAllGeneratedRecipes(Set<HostGroup> hostGroups, Map<HostGroup, Set<GeneratedRecipe>> generatedRecipeTemplates) {
        try {
            transactionService.required(() -> {
                deleteAllGeneratedRecipes(hostGroups);
                saveAllGeneratedRecipes(generatedRecipeTemplates);
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.warn("Updating generated recipes failed.");
        }
    }

    private void saveAllGeneratedRecipes(Map<HostGroup, Set<GeneratedRecipe>> generatedRecipeTemplates) {
        for (Map.Entry<HostGroup, Set<GeneratedRecipe>> entry : generatedRecipeTemplates.entrySet()) {
            generatedRecipeService.saveAll(entry.getValue());
        }
    }

    private void deleteAllGeneratedRecipes(Set<HostGroup> hostGroups) {
        for (HostGroup hg : hostGroups) {
            deleteAllGeneratedRecipes(hg);
        }
    }

    private void deleteAllGeneratedRecipes(HostGroup hostGroup) {
        if (CollectionUtils.isNotEmpty(hostGroup.getGeneratedRecipes())) {
            generatedRecipeService.deleteAll(hostGroup.getGeneratedRecipes());
        }
    }

    private boolean compareHostGroupGeneratedRecipes(HostGroup hostGroup, Map<String, GeneratedRecipe> generatedRecipeNameMap,
            List<RecipeModel> recipeModelList) {
        for (RecipeModel recipeModel : recipeModelList) {
            String recipeName = recipeModel.getName();
            if (!generatedRecipeNameMap.containsKey(recipeName)) {
                LOGGER.debug("No generated recipe with name {} for host group '{}'. Recipes should be uploaded and regenerated.",
                        recipeName, hostGroup.getName());
                return false;
            }
            GeneratedRecipe generatedRecipe = generatedRecipeNameMap.get(recipeName);
            if (!recipeModel.getGeneratedScript().equals(generatedRecipe.getExtendedRecipeText())) {
                LOGGER.debug("Regenerated recipe [name: {}] not matching with currently generated recipe for host group '{}'. "
                        + "Recipes should be uploaded and regenerated.", recipeName, hostGroup.getName());
                return false;
            }
        }
        return true;
    }

    private List<RecipeModel> convert(Set<Recipe> recipes, TemplatePreparationObject templatePreparationObject) {
        List<RecipeModel> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            String decodedContent = new String(Base64.decodeBase64(recipe.getContent()));
            final String generatedRecipeText;
            if (templatePreparationObject != null) {
                generatedRecipeText = measure(() -> centralRecipeUpdater.getRecipeText(templatePreparationObject, decodedContent),
                        LOGGER,
                        "Recipe generation took {} ms");
                LOGGER.info("Generated recipe is: {}", generatedRecipeText);
            } else {
                generatedRecipeText = decodedContent;
            }
            RecipeModel recipeModel = new RecipeModel(recipe.getName(), recipe.getRecipeType(), generatedRecipeText);
            result.add(recipeModel);
        }
        return result;
    }

    private List<RecipeModel> convert(Set<Recipe> recipes) {
        return convert(recipes, null);
    }
}
