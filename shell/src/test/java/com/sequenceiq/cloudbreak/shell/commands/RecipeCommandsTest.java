package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;

import java.io.File;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.completion.PluginExecutionType;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;

public class RecipeCommandsTest {
    private static final Long RECIPE_ID = 50L;
    private static final String RECIPE_NAME = "dummyName";

    @InjectMocks
    private RecipeCommands underTest;

    @Mock
    private RecipeEndpoint recipeEndpoint;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private CloudbreakContext mockContext;

    @Mock
    private ObjectMapper jsonMapper;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    private RecipeResponse dummyResult;

    @Before
    public void setUp() throws Exception {
        underTest = new RecipeCommands();
        MockitoAnnotations.initMocks(this);
        dummyResult = new RecipeResponse();
        dummyResult.setId(RECIPE_ID);
        dummyResult.setTimeout(1);
        dummyResult.setPlugins(Collections.<String, com.sequenceiq.cloudbreak.api.model.PluginExecutionType>emptyMap());
        dummyResult.setProperties(Collections.<String, String>emptyMap());
        given(cloudbreakClient.recipeEndpoint()).willReturn(recipeEndpoint);
        given(recipeEndpoint.postPrivate(any(RecipeRequest.class))).willReturn(new IdJson(1L));
        given(recipeEndpoint.postPublic(any(RecipeRequest.class))).willReturn(new IdJson(1L));
        given(recipeEndpoint.get(RECIPE_ID)).willReturn(dummyResult);
        given(recipeEndpoint.getPublic(RECIPE_NAME)).willReturn(dummyResult);
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willThrow(RuntimeException.class);
    }

    @Test
    public void testShowRecipeById() throws Exception {
        underTest.showRecipe(RECIPE_ID.toString(), null);
        verify(recipeEndpoint, times(1)).get(anyLong());
        verify(recipeEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testShowRecipeByName() throws Exception {
        underTest.showRecipe(null, RECIPE_NAME);
        verify(recipeEndpoint, times(0)).get(anyLong());
        verify(recipeEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowRecipeWithoutIdAndName() throws Exception {
        underTest.showRecipe(null, null);
        verify(recipeEndpoint, times(0)).get(anyLong());
    }

    @Test
    public void testDeleteRecipeById() throws Exception {
        doNothing().when(recipeEndpoint).delete(RECIPE_ID);
        underTest.deleteRecipe(RECIPE_ID.toString(), null);
        verify(recipeEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteRecipeByName() throws Exception {
        doNothing().when(recipeEndpoint).deletePublic(RECIPE_NAME);
        underTest.deleteRecipe(null, RECIPE_NAME);
        verify(recipeEndpoint, times(1)).deletePublic(anyString());
    }

    @Test
    public void testDeleteRecipeByIdAndName() throws Exception {
        doNothing().when(recipeEndpoint).delete(RECIPE_ID);
        underTest.deleteRecipe(RECIPE_ID.toString(), RECIPE_NAME);
        verify(recipeEndpoint, times(0)).deletePublic(anyString());
        verify(recipeEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteRecipeWithoutIdAndName() throws Exception {
        underTest.deleteRecipe(null, null);
        verify(recipeEndpoint, times(0)).deletePublic(anyString());
        verify(recipeEndpoint, times(0)).delete(anyLong());
    }

    @Test
    public void testStoreRecipePreScriptExistsAndPublic() throws Exception {

        underTest.storeRecipe("name", null, new PluginExecutionType("ALL_NODES"), new File(getClass().getResource("/store-recipe-test").getFile()), null, null,
                true);
        verify(recipeEndpoint, times(1)).postPublic(any(RecipeRequest.class));
        verify(recipeEndpoint, times(0)).postPrivate(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipePostScriptExistsAndPrivate() throws Exception {
        underTest.storeRecipe("name", null, new PluginExecutionType("ALL_NODES"), null, new File(getClass().getResource("/store-recipe-test").getFile()), null,
                false);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
        verify(recipeEndpoint, times(1)).postPrivate(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipeMissingScriptFiles() throws Exception {
        underTest.storeRecipe("name", null, new PluginExecutionType("ALL_NODES"), null, null, null, null);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipeNotExistsPreScriptFile() throws Exception {
        underTest.storeRecipe("name", null, new PluginExecutionType("ALL_NODES"), new File(""), null, null, null);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipeNotExistsPostScriptFile() throws Exception {
        underTest.storeRecipe("name", null, new PluginExecutionType("ALL_NODES"), null, new File(""), null, null);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }
}
