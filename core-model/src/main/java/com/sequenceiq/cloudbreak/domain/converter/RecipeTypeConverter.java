package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class RecipeTypeConverter extends DefaultEnumConverter<RecipeType> {

    @Override
    public RecipeType getDefault() {
        return RecipeType.POST_CLUSTER_INSTALL;
    }
}
