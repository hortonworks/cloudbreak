package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;

import java.io.File;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.PluginExecutionType;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;

public class RecipeCommandsTest {
    private static final String RECIPE_ID = "50";
    private static final String RECIPE_NAME = "dummyName";

    @InjectMocks
    private RecipeCommands underTest;

    @Mock
    private RecipeEndpoint recipeEndpoint;

    @Mock
    private CloudbreakContext mockContext;

    @Mock
    private ObjectMapper jsonMapper;

    private RecipeResponse dummyResult;

    @Before
    public void setUp() {
        underTest = new RecipeCommands();
        MockitoAnnotations.initMocks(this);
        dummyResult = new RecipeResponse();
        dummyResult.setId(Long.valueOf(RECIPE_ID));
    }

    @Test
    public void testShowRecipeById() throws Exception {
        given(recipeEndpoint.getPublic(RECIPE_ID)).willReturn(dummyResult);
        underTest.showRecipe(RECIPE_ID, null);
        verify(recipeEndpoint, times(1)).get(anyLong());
    }

    @Test
    public void testShowRecipeByName() throws Exception {
        given(recipeEndpoint.getPublic(RECIPE_NAME)).willReturn(dummyResult);
        given(recipeEndpoint.get(Long.valueOf(RECIPE_ID))).willReturn(dummyResult);
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
        doNothing().when(recipeEndpoint).delete(Long.valueOf(RECIPE_ID));
        underTest.deleteRecipe(RECIPE_ID, null);
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
        doNothing().when(recipeEndpoint).delete(Long.valueOf(RECIPE_ID));
        underTest.deleteRecipe(RECIPE_ID, RECIPE_NAME);
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
    public void testStoreRecipePreScriptExists() throws Exception {
        underTest.storeRecipe("name", null, PluginExecutionType.ALL_NODES, new File(getClass().getResource("/store-recipe-test").getFile()), null, null, null);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipePostScriptExists() throws Exception {
        underTest.storeRecipe("name", null, PluginExecutionType.ALL_NODES, null, new File(getClass().getResource("/store-recipe-test").getFile()), null, null);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipeMissingScriptFiles() throws Exception {
        underTest.storeRecipe("name", null, PluginExecutionType.ALL_NODES, null, null, null, null);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipeNotExistsPreScriptFile() throws Exception {
        underTest.storeRecipe("name", null, PluginExecutionType.ALL_NODES, new File(""), null, null, null);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipeNotExistsPostScriptFile() throws Exception {
        underTest.storeRecipe("name", null, PluginExecutionType.ALL_NODES, null, new File(""), null, null);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }
}
