package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.validation.Valid;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.sequenceiq.cloudbreak.converter.RecipeConverter;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Controller
@Api(value = "/recipe", description = "Operations on recipes", position = 5)
public class RecipeController {

    private static final String BLUEPRINT_REQUEST_NOTES =
            "In the recipe request, id is not considered.";

    @Autowired
    private RecipeConverter recipeConverter;

    @Autowired
    private RecipeService recipeService;

    @ApiOperation(value = "create recipe as public or private resource", produces = "application/json", notes = BLUEPRINT_REQUEST_NOTES)
    @RequestMapping(value = "account/recipes", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountRecipe(@ModelAttribute("user") CbUser user, @RequestBody @Valid RecipeJson recipeRequest) {
        return createRecipe(user, recipeRequest, true);
    }

    @ApiOperation(value = "create recipe as private resource", produces = "application/json", notes = BLUEPRINT_REQUEST_NOTES)
    @RequestMapping(value = "user/recipes", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createUserRecipe(@ModelAttribute("user") CbUser user, @RequestBody @Valid RecipeJson recipeRequest) {
        return createRecipe(user, recipeRequest, false);
    }

    @ApiOperation(value = "retrieve private recipes", produces = "application/json", notes = "")
    @RequestMapping(value = "user/recipes", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<RecipeJson>> getPrivateRecipes(@ModelAttribute("user") CbUser user) {
        Set<Recipe> recipes = recipeService.retrievePrivateRecipes(user);
        return new ResponseEntity<>(recipeConverter.convertAllEntityToJson(recipes), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve public and private (owned) recipes", produces = "application/json", notes = "")
    @RequestMapping(value = "account/recipes", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<RecipeJson>> getAccountRecipes(@ModelAttribute("user") CbUser user) {
        Set<Recipe> recipes = recipeService.retrieveAccountRecipes(user);
        return new ResponseEntity<>(recipeConverter.convertAllEntityToJson(recipes), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve a private recipe by name", produces = "application/json", notes = "")
    @RequestMapping(value = "user/recipes/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeJson> getPrivateRecipe(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Recipe recipe = recipeService.getPrivateRecipe(name, user);
        return new ResponseEntity<>(recipeConverter.convert(recipe), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve a public or private (owned) recipe by name", produces = "application/json", notes = "")
    @RequestMapping(value = "account/recipes/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeJson> getAccountRecipe(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        Recipe recipe = recipeService.getPublicRecipe(name, user);
        return new ResponseEntity<>(recipeConverter.convert(recipe), HttpStatus.OK);
    }

    @ApiOperation(value = "retrieve template by id", produces = "application/json", notes = "")
    @RequestMapping(value = "recipes/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeJson> getRecipe(@PathVariable Long id) {
        Recipe recipe = recipeService.get(id);
        return new ResponseEntity<>(recipeConverter.convert(recipe), HttpStatus.OK);
    }

    @ApiOperation(value = "delete recipe by id", produces = "application/json", notes = "")
    @RequestMapping(value = "recipes/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteBlueprint(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        recipeService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "delete public (owned) or private recipe by name", produces = "application/json", notes = "")
    @RequestMapping(value = "account/recipes/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteBlueprintInAccount(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        recipeService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "delete private recipe by name", produces = "application/json", notes = "")
    @RequestMapping(value = "user/recipes/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<String> deleteBlueprintInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        recipeService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private ResponseEntity<IdJson> createRecipe(CbUser user, RecipeJson recipeRequest, boolean publicInAccount) {
        Recipe recipe = recipeService.create(user, recipeConverter.convert(recipeRequest, publicInAccount));
        return new ResponseEntity<>(new IdJson(recipe.getId()), HttpStatus.CREATED);
    }
}
