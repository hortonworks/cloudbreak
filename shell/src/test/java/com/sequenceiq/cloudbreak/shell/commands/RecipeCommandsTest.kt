package com.sequenceiq.cloudbreak.shell.commands

import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.times
import org.mockito.BDDMockito.verify
import org.mockito.Matchers.any
import org.mockito.Matchers.anyLong
import org.mockito.Matchers.anyObject
import org.mockito.Matchers.anyVararg
import org.mockito.Mockito.doNothing

import java.io.File
import java.util.Collections

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.fasterxml.jackson.databind.ObjectMapper
import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint
import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.RecipeRequest
import com.sequenceiq.cloudbreak.api.model.RecipeResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.commands.common.RecipeCommands
import com.sequenceiq.cloudbreak.shell.completion.PluginExecutionType
import com.sequenceiq.cloudbreak.shell.model.OutPutType
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer

class RecipeCommandsTest {

    @InjectMocks
    private var underTest: RecipeCommands? = null

    @Mock
    private val recipeEndpoint: RecipeEndpoint? = null

    @Mock
    private val cloudbreakClient: CloudbreakClient? = null

    @Mock
    private val mockContext: ShellContext? = null

    @Mock
    private val jsonMapper: ObjectMapper? = null

    @Mock
    private val exceptionTransformer: ExceptionTransformer? = null

    @Mock
    private val outputTransformer: OutputTransformer? = null

    private var dummyResult: RecipeResponse? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        underTest = RecipeCommands(mockContext)
        MockitoAnnotations.initMocks(this)
        dummyResult = RecipeResponse()
        dummyResult!!.id = RECIPE_ID
        dummyResult!!.timeout = 1
        dummyResult!!.plugins = emptyMap<String, ExecutionType>()
        dummyResult!!.properties = emptyMap<String, String>()
        given(cloudbreakClient!!.recipeEndpoint()).willReturn(recipeEndpoint)
        given(recipeEndpoint!!.postPrivate(any<RecipeRequest>(RecipeRequest::class.java))).willReturn(IdJson(1L))
        given(recipeEndpoint.postPublic(any<RecipeRequest>(RecipeRequest::class.java))).willReturn(IdJson(1L))
        given(recipeEndpoint[RECIPE_ID]).willReturn(dummyResult)
        given(recipeEndpoint.getPublic(RECIPE_NAME)).willReturn(dummyResult)
        given(mockContext!!.cloudbreakClient()).willReturn(cloudbreakClient)
        given(exceptionTransformer!!.transformToRuntimeException(any<Exception>(Exception::class.java))).willThrow(RuntimeException::class.java)
        given(mockContext.outputTransformer()).willReturn(outputTransformer)
        given(outputTransformer!!.render(any<OutPutType>(OutPutType::class.java), *anyVararg<String>())).willReturn("id 1 name test1")
        given(outputTransformer.render(anyObject<Any>())).willReturn("id 1 name test1")
    }

    @Test
    @Throws(Exception::class)
    fun testShowRecipeById() {
        underTest!!.show(RECIPE_ID, null)
        Mockito.verify(recipeEndpoint, Mockito.times(1)).get(anyLong())
        Mockito.verify(recipeEndpoint, Mockito.times(0)).getPublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testShowRecipeByName() {
        underTest!!.show(null, RECIPE_NAME)
        Mockito.verify(recipeEndpoint, Mockito.times(0)).get(anyLong())
        Mockito.verify(recipeEndpoint, Mockito.times(1)).getPublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testShowRecipeWithoutIdAndName() {
        underTest!!.show(null, null)
        Mockito.verify(recipeEndpoint, Mockito.times(0)).get(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteRecipeById() {
        doNothing().`when`<RecipeEndpoint>(recipeEndpoint).delete(RECIPE_ID)
        underTest!!.delete(RECIPE_ID, null)
        Mockito.verify(recipeEndpoint, Mockito.times(1)).delete(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteRecipeByName() {
        doNothing().`when`<RecipeEndpoint>(recipeEndpoint).deletePublic(RECIPE_NAME)
        underTest!!.delete(null, RECIPE_NAME)
        Mockito.verify(recipeEndpoint, Mockito.times(1)).deletePublic(Matchers.anyString())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteRecipeByIdAndName() {
        doNothing().`when`<RecipeEndpoint>(recipeEndpoint).delete(RECIPE_ID)
        underTest!!.delete(RECIPE_ID, RECIPE_NAME)
        Mockito.verify(recipeEndpoint, Mockito.times(0)).deletePublic(Matchers.anyString())
        Mockito.verify(recipeEndpoint, Mockito.times(1)).delete(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteRecipeWithoutIdAndName() {
        underTest!!.delete(null, null)
        Mockito.verify(recipeEndpoint, Mockito.times(0)).deletePublic(Matchers.anyString())
        Mockito.verify(recipeEndpoint, Mockito.times(0)).delete(anyLong())
    }

    @Test
    @Throws(Exception::class)
    fun testStoreRecipePreScriptExistsAndPublic() {

        underTest!!.storeRecipe("name", null, PluginExecutionType("ALL_NODES"), File(javaClass.getResource("/store-recipe-test").getFile()), null, null,
                true)
        Mockito.verify(recipeEndpoint, Mockito.times(1)).postPublic(any<RecipeRequest>(RecipeRequest::class.java))
        Mockito.verify(recipeEndpoint, Mockito.times(0)).postPrivate(any<RecipeRequest>(RecipeRequest::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testStoreRecipePostScriptExistsAndPrivate() {
        underTest!!.storeRecipe("name", null, PluginExecutionType("ALL_NODES"), null, File(javaClass.getResource("/store-recipe-test").getFile()), null,
                false)
        Mockito.verify(recipeEndpoint, Mockito.times(0)).postPublic(any<RecipeRequest>(RecipeRequest::class.java))
        Mockito.verify(recipeEndpoint, Mockito.times(1)).postPrivate(any<RecipeRequest>(RecipeRequest::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testStoreRecipeMissingScriptFiles() {
        underTest!!.storeRecipe("name", null, PluginExecutionType("ALL_NODES"), null, null, null, null)
        Mockito.verify(recipeEndpoint, Mockito.times(0)).postPublic(any<RecipeRequest>(RecipeRequest::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testStoreRecipeNotExistsPreScriptFile() {
        underTest!!.storeRecipe("name", null, PluginExecutionType("ALL_NODES"), File(""), null, null, null)
        Mockito.verify(recipeEndpoint, Mockito.times(0)).postPublic(any<RecipeRequest>(RecipeRequest::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testStoreRecipeNotExistsPostScriptFile() {
        underTest!!.storeRecipe("name", null, PluginExecutionType("ALL_NODES"), null, File(""), null, null)
        Mockito.verify(recipeEndpoint, Mockito.times(0)).postPublic(any<RecipeRequest>(RecipeRequest::class.java))
    }

    companion object {
        private val RECIPE_ID = 50L
        private val RECIPE_NAME = "dummyName"
    }
}
