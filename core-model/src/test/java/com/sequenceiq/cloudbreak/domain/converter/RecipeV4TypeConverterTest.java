package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

import javax.persistence.AttributeConverter;

public class RecipeV4TypeConverterTest extends DefaultEnumConverterBaseTest<RecipeV4Type> {

    @Override
    public RecipeV4Type getDefaultValue() {
        return RecipeV4Type.POST_CLUSTER_INSTALL;
    }

    @Override
    public AttributeConverter<RecipeV4Type, String> getVictim() {
        return new RecipeV4TypeConverter();
    }
}