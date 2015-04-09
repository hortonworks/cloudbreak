package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.RecipeJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Controller
public class RecipeController {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private RecipeService recipeService;

    @RequestMapping(value = "account/recipes", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountRecipe(@ModelAttribute("user") CbUser user, @RequestBody @Valid RecipeJson recipeRequest) {
        return createRecipe(user, recipeRequest, true);
    }

    @RequestMapping(value = "user/recipes", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createUserRecipe(@ModelAttribute("user") CbUser user, @RequestBody @Valid RecipeJson recipeRequest) {
        return createRecipe(user, recipeRequest, false);
    }

    @RequestMapping(value = "user/recipes", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<RecipeJson>> getPrivateRecipes(@ModelAttribute("user") CbUser user) {
        Set<Recipe> recipes = recipeService.retrievePrivateRecipes(user);
        return new ResponseEntity<>(toJsonSet(recipes), HttpStatus.OK);
    }

    @RequestMapping(value = "account/recipes", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<RecipeJson>> getAccountRecipes(@ModelAttribute("user") CbUser user) {
        Set<Recipe> recipes = recipeService.retrieveAccountRecipes(user);
        return new ResponseEntity<>(toJsonSet(recipes), HttpStatus.OK);
    }

    @RequestMapping(value = "user/recipes/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeJson> getPrivateRecipe(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Recipe recipe = recipeService.getPrivateRecipe(name, user);
        return new ResponseEntity<>(conversionService.convert(recipe, RecipeJson.class), HttpStatus.OK);
    }

    @RequestMapping(value = "account/recipes/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeJson> getAccountRecipe(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Recipe recipe = recipeService.getPublicRecipe(name, user);
        return new ResponseEntity<>(conversionService.convert(recipe, RecipeJson.class), HttpStatus.OK);
    }

    @RequestMapping(value = "recipes/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeJson> getRecipe(@PathVariable Long id) {
        Recipe recipe = recipeService.get(id);
        return new ResponseEntity<>(conversionService.convert(recipe, RecipeJson.class), HttpStatus.OK);
    }

    @RequestMapping(value = "recipes/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteBlueprint(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        recipeService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "account/recipes/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteBlueprintInAccount(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        recipeService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "user/recipes/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteBlueprintInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        recipeService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<IdJson> createRecipe(CbUser user, RecipeJson recipeRequest, boolean publicInAccount) {
        Recipe recipe = conversionService.convert(recipeRequest, Recipe.class);
        recipe.setPublicInAccount(publicInAccount);
        recipe = recipeService.create(user, recipe);
        return new ResponseEntity<>(new IdJson(recipe.getId()), HttpStatus.CREATED);
    }

    private Set<RecipeJson> toJsonSet(Set<Recipe> recipes) {
        return (Set<RecipeJson>) conversionService.convert(recipes, TypeDescriptor.forObject(recipes),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(RecipeJson.class)));
    }

}
