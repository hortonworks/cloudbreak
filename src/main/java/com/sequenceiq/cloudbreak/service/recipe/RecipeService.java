package com.sequenceiq.cloudbreak.service.recipe;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Recipe;

public interface RecipeService {

    Recipe create(CbUser user, Recipe recipe);
}
