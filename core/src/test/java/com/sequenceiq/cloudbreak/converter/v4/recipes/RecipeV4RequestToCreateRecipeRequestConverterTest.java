package com.sequenceiq.cloudbreak.converter.v4.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.cdp.datahub.model.CreateRecipeRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;

class RecipeV4RequestToCreateRecipeRequestConverterTest {

    private RecipeV4RequestToCreateRecipeRequestConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new RecipeV4RequestToCreateRecipeRequestConverter();
    }

    @Test
    void convert() {
        RecipeV4Request request = new RecipeV4Request();
        request.setContent("content");
        request.setDescription("desc");
        request.setName("name");
        request.setType(RecipeV4Type.POST_CLOUDERA_MANAGER_START);
        CreateRecipeRequest result = underTest.convert(request);
        assertEquals(request.getContent(), result.getRecipeContent());
        assertEquals(request.getDescription(), result.getDescription());
        assertEquals(request.getName(), result.getRecipeName());
        assertEquals(request.getType().name(), result.getType());
    }
}
