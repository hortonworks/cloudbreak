package com.sequenceiq.environment.environment.service.recipe;

import static com.sequenceiq.authorization.resource.AuthorizationResourceType.RECIPE;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnListProvider;
import com.sequenceiq.cloudbreak.recipe.RecipeCrnListProviderService;
import com.sequenceiq.environment.environment.domain.FreeIpaRecipe;
import com.sequenceiq.environment.environment.repository.FreeIpaRecipeRepository;

@Service
public class EnvironmentRecipeService implements AuthorizationResourceCrnListProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentRecipeService.class);

    @Inject
    private FreeIpaRecipeRepository freeIpaRecipeRepository;

    @Inject
    private RecipeCrnListProviderService recipeCrnListProviderService;

    public EnvironmentRecipeService(FreeIpaRecipeRepository freeIpaRecipeRepository, RecipeCrnListProviderService recipeCrnListProviderService) {
        this.freeIpaRecipeRepository = freeIpaRecipeRepository;
        this.recipeCrnListProviderService = recipeCrnListProviderService;
    }

    public Set<String> getRecipes(Long environmentId) {
        LOGGER.debug("Get recipes for environment with id: {}", environmentId);
        return freeIpaRecipeRepository.findByEnvironmentId(environmentId).stream().map(FreeIpaRecipe::getRecipe).collect(Collectors.toSet());
    }

    public void saveRecipes(Set<String> recipes, Long environmentId) {
        LOGGER.info("Save recipes {} for environment with id: {}", recipes, environmentId);
        freeIpaRecipeRepository.saveAll(recipes.stream().map(recipe -> new FreeIpaRecipe(environmentId, recipe)).collect(Collectors.toSet()));
    }

    public void deleteRecipes(Long environmentId) {
        LOGGER.info("Delete recipes for environment with id: {}", environmentId);
        freeIpaRecipeRepository.deleteFreeIpaRecipesByEnvironmentId(environmentId);
    }

    public void validateFreeipaRecipesExistsByName(Set<String> resourceNames) {
        recipeCrnListProviderService.validateRequestedRecipesExistsByName(resourceNames);
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return recipeCrnListProviderService.getResourceCrnListByResourceNameList(resourceNames);
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return RECIPE;
    }

}
