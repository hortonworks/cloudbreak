package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class RecipeV4TypeConverter extends DefaultEnumConverter<RecipeV4Type> {

    @Override
    public RecipeV4Type getDefault() {
        return RecipeV4Type.POST_CLUSTER_INSTALL;
    }
}
