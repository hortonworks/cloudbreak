package com.sequenceiq.cloudbreak.shell.commands;

import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.common.type.RecipeType;
import com.sequenceiq.cloudbreak.shell.commands.common.RecipeCommands;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;

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
    private ShellContext mockContext;

    @Mock
    private ObjectMapper jsonMapper;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    @Mock
    private OutputTransformer outputTransformer;

    private final RuntimeException expectedException = new RuntimeException("something not found");

    @Before
    public void setUp() throws Exception {
        underTest = new RecipeCommands(mockContext);
        MockitoAnnotations.initMocks(this);
        RecipeResponse dummyResult = new RecipeResponse();
        dummyResult.setId(RECIPE_ID);
        given(cloudbreakClient.recipeEndpoint()).willReturn(recipeEndpoint);
        RecipeResponse recipeResponse = new RecipeResponse();
        recipeResponse.setId(1L);
        given(recipeEndpoint.postPrivate(any(RecipeRequest.class))).willReturn(recipeResponse);
        given(recipeEndpoint.postPublic(any(RecipeRequest.class))).willReturn(recipeResponse);
        given(recipeEndpoint.get(RECIPE_ID)).willReturn(dummyResult);
        given(recipeEndpoint.getPublic(RECIPE_NAME)).willReturn(dummyResult);
        given(mockContext.cloudbreakClient()).willReturn(cloudbreakClient);
        given(exceptionTransformer.transformToRuntimeException(eq(expectedException))).willThrow(expectedException);
        given(exceptionTransformer.transformToRuntimeException(anyString())).willThrow(expectedException);
        given(mockContext.exceptionTransformer()).willReturn(exceptionTransformer);
        given(mockContext.outputTransformer()).willReturn(outputTransformer);
        given(outputTransformer.render(any(OutPutType.class), anyVararg())).willReturn("id 1 name test1");
        given(outputTransformer.render(anyObject())).willReturn("id 1 name test1");
    }

    @Test
    public void testShowRecipeById() throws Exception {
        underTest.show(RECIPE_ID, null, null);
        verify(recipeEndpoint, times(1)).get(anyLong());
        verify(recipeEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testShowRecipeByName() throws Exception {
        underTest.show(null, RECIPE_NAME, null);
        verify(recipeEndpoint, times(0)).get(anyLong());
        verify(recipeEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowRecipeWithoutIdAndName() throws Exception {
        RuntimeException ext = null;
        try {
            underTest.show(null, null, null);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(recipeEndpoint, times(0)).get(anyLong());
    }

    @Test
    public void testDeleteRecipeById() throws Exception {
        doNothing().when(recipeEndpoint).delete(RECIPE_ID);
        underTest.delete(RECIPE_ID, null);
        verify(recipeEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteRecipeByName() throws Exception {
        doNothing().when(recipeEndpoint).deletePublic(RECIPE_NAME);
        underTest.delete(null, RECIPE_NAME);
        verify(recipeEndpoint, times(1)).deletePublic(anyString());
    }

    @Test
    public void testDeleteRecipeByIdAndName() throws Exception {
        doNothing().when(recipeEndpoint).delete(RECIPE_ID);
        underTest.delete(RECIPE_ID, RECIPE_NAME);
        verify(recipeEndpoint, times(0)).deletePublic(anyString());
        verify(recipeEndpoint, times(1)).delete(anyLong());
    }

    @Test
    public void testDeleteRecipeWithoutIdAndName() throws Exception {
        RuntimeException ext = null;
        try {
            underTest.delete(null, null);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(recipeEndpoint, times(0)).deletePublic(anyString());
        verify(recipeEndpoint, times(0)).delete(anyLong());
    }

    @Test
    public void testStoreRecipePreScriptExistsAndPublic() throws Exception {
        underTest.createRecipe("name", RecipeType.PRE, null,
                new File(getClass().getResource("/store-recipe-test").getFile()), null, true);
        verify(recipeEndpoint, times(1)).postPublic(any(RecipeRequest.class));
        verify(recipeEndpoint, times(0)).postPrivate(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipePostScriptExistsAndPrivate() throws Exception {
        underTest.createRecipe("name", RecipeType.PRE, null,
                new File(getClass().getResource("/store-recipe-test").getFile()), null, false);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
        verify(recipeEndpoint, times(1)).postPrivate(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipeMissingScriptFiles() throws Exception {
        RuntimeException ext = null;
        try {
            underTest.createRecipe("name", RecipeType.PRE, null, null, null, null);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipeNotExistsPreScriptFile() throws Exception {
        RuntimeException ext = null;
        try {
            underTest.createRecipe("name", RecipeType.PRE, null, new File(""), null, null);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }

    @Test
    public void testStoreRecipeNotExistsPostScriptFile() throws Exception {
        RuntimeException ext = null;
        try {
            underTest.createRecipe("name", RecipeType.PRE, null, new File(""), null, null);
        } catch (RuntimeException e) {
            ext = e;
        }
        Assert.assertEquals("Wrong error occurred", expectedException, ext);
        verify(recipeEndpoint, times(0)).postPublic(any(RecipeRequest.class));
    }
}
