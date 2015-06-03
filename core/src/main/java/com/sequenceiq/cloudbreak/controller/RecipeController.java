package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.doc.ContentType;
import com.sequenceiq.cloudbreak.controller.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.controller.doc.Notes;
import com.sequenceiq.cloudbreak.controller.doc.OperationDescriptions.RecipeOpDescription;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.RecipeRequest;
import com.sequenceiq.cloudbreak.controller.json.RecipeResponse;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Controller
@Api(value = "/recipe", description = ControllerDescription.RECIPE_DESCRIPTION, position = 5)
public class RecipeController {

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private RecipeService recipeService;

    @ApiOperation(value = RecipeOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "account/recipes", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountRecipe(@ModelAttribute("user") CbUser user, @RequestBody @Valid RecipeRequest recipeRequest) {
        MDCBuilder.buildMdcContext(user);
        return createRecipe(user, recipeRequest, true);
    }

    @ApiOperation(value = RecipeOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "user/recipes", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createUserRecipe(@ModelAttribute("user") CbUser user, @RequestBody @Valid RecipeRequest recipeRequest) {
        MDCBuilder.buildMdcContext(user);
        return createRecipe(user, recipeRequest, false);
    }

    @ApiOperation(value = RecipeOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "user/recipes", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<RecipeResponse>> getPrivateRecipes(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Recipe> recipes = recipeService.retrievePrivateRecipes(user);
        return new ResponseEntity<>(toJsonSet(recipes), HttpStatus.OK);
    }

    @ApiOperation(value = RecipeOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "account/recipes", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<RecipeResponse>> getAccountRecipes(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Recipe> recipes = recipeService.retrieveAccountRecipes(user);
        return new ResponseEntity<>(toJsonSet(recipes), HttpStatus.OK);
    }

    @ApiOperation(value = RecipeOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "user/recipes/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeResponse> getPrivateRecipe(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Recipe recipe = recipeService.getPrivateRecipe(name, user);
        return new ResponseEntity<>(conversionService.convert(recipe, RecipeResponse.class), HttpStatus.OK);
    }

    @ApiOperation(value = RecipeOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "account/recipes/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeResponse> getAccountRecipe(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Recipe recipe = recipeService.getPublicRecipe(name, user);
        return new ResponseEntity<>(conversionService.convert(recipe, RecipeResponse.class), HttpStatus.OK);
    }

    @ApiOperation(value = RecipeOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "recipes/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeResponse> getRecipe(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildMdcContext(user);
        Recipe recipe = recipeService.get(id);
        return new ResponseEntity<>(conversionService.convert(recipe, RecipeResponse.class), HttpStatus.OK);
    }

    @ApiOperation(value = RecipeOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "recipes/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteRecipe(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildMdcContext(user);
        recipeService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = RecipeOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "account/recipes/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteRecipeInAccount(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        recipeService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = RecipeOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.RECIPE_NOTES)
    @RequestMapping(value = "user/recipes/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteRecipeInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        recipeService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<IdJson> createRecipe(CbUser user, RecipeRequest recipeRequest, boolean publicInAccount) {
        Recipe recipe = conversionService.convert(recipeRequest, Recipe.class);
        recipe.setPublicInAccount(publicInAccount);
        recipe = recipeService.create(user, recipe);
        return new ResponseEntity<>(new IdJson(recipe.getId()), HttpStatus.CREATED);
    }

    private Set<RecipeResponse> toJsonSet(Set<Recipe> recipes) {
        return (Set<RecipeResponse>) conversionService.convert(recipes, TypeDescriptor.forObject(recipes),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(RecipeResponse.class)));
    }

}
