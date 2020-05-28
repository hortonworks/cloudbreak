package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

import javax.persistence.AttributeConverter;

public class RecipeTypeConverterTest extends DefaultEnumConverterBaseTest<RecipeType> {

    @Override
    public RecipeType getDefaultValue() {
        return RecipeType.POST_CLUSTER_INSTALL;
    }

    @Override
    public AttributeConverter<RecipeType, String> getVictim() {
        return new RecipeTypeConverter();
    }
}