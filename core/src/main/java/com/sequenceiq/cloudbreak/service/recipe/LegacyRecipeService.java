package com.sequenceiq.cloudbreak.service.recipe;

import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyRecipeService extends LegacyOrganizationAwareResourceService<Recipe>, RecipeService {
}
