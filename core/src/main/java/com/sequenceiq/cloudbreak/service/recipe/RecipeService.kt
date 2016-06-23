package com.sequenceiq.cloudbreak.service.recipe

import javax.inject.Inject
import javax.transaction.Transactional

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.common.type.APIResourceType
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.NotFoundException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Recipe
import com.sequenceiq.cloudbreak.repository.HostGroupRepository
import com.sequenceiq.cloudbreak.repository.RecipeRepository
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException

@Service
@Transactional
class RecipeService {

    @Inject
    private val recipeRepository: RecipeRepository? = null

    @Inject
    private val hostGroupRepository: HostGroupRepository? = null

    @Transactional(Transactional.TxType.NEVER)
    fun create(user: CbUser, recipe: Recipe): Recipe {
        recipe.owner = user.userId
        recipe.account = user.account
        try {
            return recipeRepository!!.save(recipe)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateKeyValueException(APIResourceType.RECIPE, recipe.name, ex)
        }

    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    operator fun get(id: Long?): Recipe? {
        val recipe = recipeRepository!!.findOne(id) ?: throw NotFoundException(String.format("Recipe '%s' not found", id))
        return recipe
    }

    fun retrievePrivateRecipes(user: CbUser): Set<Recipe> {
        return recipeRepository!!.findForUser(user.userId)
    }

    fun retrieveAccountRecipes(user: CbUser): Set<Recipe> {
        if (user.roles.contains(CbUserRole.ADMIN)) {
            return recipeRepository!!.findAllInAccount(user.account)
        } else {
            return recipeRepository!!.findPublicInAccountForUser(user.userId, user.account)
        }
    }

    fun getPrivateRecipe(name: String, user: CbUser): Recipe {
        val recipe = recipeRepository!!.findByNameForUser(name, user.userId) ?: throw NotFoundException(String.format("Recipe '%s' not found.", name))
        return recipe
    }

    fun getPublicRecipe(name: String, user: CbUser): Recipe {
        val recipe = recipeRepository!!.findByNameInAccount(name, user.account) ?: throw NotFoundException(String.format("Recipe '%s' not found.", name))
        return recipe
    }

    fun delete(id: Long?, user: CbUser) {
        val recipe = get(id) ?: throw NotFoundException(String.format("Recipe '%s' not found.", id))
        delete(recipe, user)
    }

    fun delete(name: String, user: CbUser) {
        val recipe = recipeRepository!!.findByNameInAccount(name, user.account) ?: throw NotFoundException(String.format("Recipe '%s' not found.", name))
        delete(recipe, user)
    }

    private fun delete(recipe: Recipe, user: CbUser) {
        if (hostGroupRepository!!.findAllHostGroupsByRecipe(recipe.id).isEmpty()) {
            if (user.userId != recipe.owner && !user.roles.contains(CbUserRole.ADMIN)) {
                throw BadRequestException("Public recipes can only be deleted by owners or account admins.")
            } else {
                recipeRepository!!.delete(recipe)
            }
        } else {
            throw BadRequestException(String.format(
                    "There are clusters associated with recipe '%s'. Please remove these before deleting the recipe.", recipe.id))
        }
    }
}
