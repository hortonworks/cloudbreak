package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

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

import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.RecipeJson;
import com.sequenceiq.cloudbreak.converter.BlueprintConverter;
import com.sequenceiq.cloudbreak.converter.RecipeConverter;
import com.sequenceiq.cloudbreak.domain.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.repository.RecipeRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Controller
public class RecipeController {

    public static final String RECIPE_BP_PREFIX = "recipe-bp-";

    @Autowired
    private RecipeConverter recipeConverter;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private BlueprintConverter blueprintConverter;

    @Autowired
    private BlueprintService blueprintService;

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
        return new ResponseEntity<>(recipeConverter.convertAllEntityToJson(recipes), HttpStatus.OK);
    }

    @RequestMapping(value = "account/recipes", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<RecipeJson>> getAccountRecipes(@ModelAttribute("user") CbUser user) {
        Set<Recipe> recipes = recipeService.retrieveAccountRecipes(user);
        return new ResponseEntity<>(recipeConverter.convertAllEntityToJson(recipes), HttpStatus.OK);
    }

    @RequestMapping(value = "recipes/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RecipeJson> getRecipe(@PathVariable Long id) {
        Recipe recipe = recipeService.get(id);
        return new ResponseEntity<>(recipeConverter.convert(recipe), HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createRecipe(CbUser user, RecipeJson recipeRequest, boolean publicInAccount) {
        if (recipeRepository.findByNameInAccount(recipeRequest.getName(), user.getAccount()) != null) {
            throw new DuplicateKeyValueException(APIResourceType.RECIPE, recipeRequest.getName());
        }
        Blueprint blueprint = blueprintService.create(
                user,
                blueprintConverter.convert(RECIPE_BP_PREFIX + recipeRequest.getName(), recipeRequest.getBlueprint(), publicInAccount));
        Recipe recipe = recipeService.create(user, recipeConverter.convert(recipeRequest, blueprint, publicInAccount));
        return new ResponseEntity<>(new IdJson(recipe.getId()), HttpStatus.CREATED);
    }
}
