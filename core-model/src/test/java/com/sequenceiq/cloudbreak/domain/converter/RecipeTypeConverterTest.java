package com.sequenceiq.cloudbreak.domain.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

public class RecipeTypeConverterTest extends DefaultEnumConverterBaseTest<RecipeType> {

    @Override
    public RecipeType getDefaultValue() {
        return RecipeType.POST_SERVICE_DEPLOYMENT;
    }

    @Override
    public AttributeConverter<RecipeType, String> getVictim() {
        return new RecipeTypeConverter();
    }
}
