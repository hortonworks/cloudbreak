package com.sequenceiq.cloudbreak.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Recipe
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.RecipeRequest
import com.sequenceiq.cloudbreak.api.model.RecipeResponse
import com.sequenceiq.cloudbreak.service.recipe.RecipeService

@Component
class RecipeController : RecipeEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Autowired
    private val recipeService: RecipeService? = null

    @Autowired
    private val authenticatedUserService: AuthenticatedUserService? = null

    override fun postPublic(recipeRequest: RecipeRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createRecipe(user, recipeRequest, true)
    }

    override fun postPrivate(recipeRequest: RecipeRequest): IdJson {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        return createRecipe(user, recipeRequest, false)
    }

    override fun getPrivates(): Set<RecipeResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val recipes = recipeService!!.retrievePrivateRecipes(user)
        return toJsonSet(recipes)
    }

    override fun getPublics(): Set<RecipeResponse> {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val recipes = recipeService!!.retrieveAccountRecipes(user)
        return toJsonSet(recipes)
    }

    override fun getPrivate(name: String): RecipeResponse {
        val user = authenticatedUserService!!.cbUser
        val recipe = recipeService!!.getPrivateRecipe(name, user)
        return conversionService!!.convert<RecipeResponse>(recipe, RecipeResponse::class.java)
    }

    override fun getPublic(name: String): RecipeResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val recipe = recipeService!!.getPublicRecipe(name, user)
        return conversionService!!.convert<RecipeResponse>(recipe, RecipeResponse::class.java)
    }

    override fun get(id: Long?): RecipeResponse {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        val recipe = recipeService!!.get(id)
        return conversionService!!.convert<RecipeResponse>(recipe, RecipeResponse::class.java)
    }

    override fun delete(id: Long?) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        recipeService!!.delete(id, user)
    }

    override fun deletePublic(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        recipeService!!.delete(name, user)
    }

    override fun deletePrivate(name: String) {
        val user = authenticatedUserService!!.cbUser
        MDCBuilder.buildUserMdcContext(user)
        recipeService!!.delete(name, user)
    }

    private fun createRecipe(user: CbUser, recipeRequest: RecipeRequest, publicInAccount: Boolean): IdJson {
        var recipe = conversionService!!.convert<Recipe>(recipeRequest, Recipe::class.java)
        recipe.isPublicInAccount = publicInAccount
        recipe = recipeService!!.create(user, recipe)
        return IdJson(recipe.id)
    }

    private fun toJsonSet(recipes: Set<Recipe>): Set<RecipeResponse> {
        return conversionService!!.convert(recipes, TypeDescriptor.forObject(recipes),
                TypeDescriptor.collection(Set<Any>::class.java, TypeDescriptor.valueOf(RecipeResponse::class.java))) as Set<RecipeResponse>
    }
}
