'use strict';

exports.deletePrivateRecipe = function(args, res, next) {
  /**
   * delete private recipe by name
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicRecipe = function(args, res, next) {
  /**
   * delete public (owned) or private recipe by name
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deleteRecipe = function(args, res, next) {
  /**
   * delete recipe by id
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * id Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getPrivateRecipe = function(args, res, next) {
  /**
   * retrieve a private recipe by name
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * name String 
   * returns RecipeResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "uri" : "aeiou",
  "content" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesRecipe = function(args, res, next) {
  /**
   * retrieve private recipes
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "uri" : "aeiou",
  "content" : "aeiou"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicRecipe = function(args, res, next) {
  /**
   * retrieve a public or private (owned) recipe by name
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * name String 
   * returns RecipeResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "uri" : "aeiou",
  "content" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsRecipe = function(args, res, next) {
  /**
   * retrieve public and private (owned) recipes
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/recipes/recipes.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getRecipe = function(args, res, next) {
  /**
   * retrieve recipe by id
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * id Long 
   * returns RecipeResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "uri" : "aeiou",
  "content" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getRecipeRequestFromName = function(args, res, next) {
  /**
   * retrieve recipe request by recipe name
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * name String 
   * returns RecipeRequest
   **/
  var examples = {};
  examples['application/json'] = require('../responses/recipes/recipes.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateRecipe = function(args, res, next) {
  /**
   * create recipe as private resource
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * body RecipeRequest  (optional)
   * returns RecipeResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "uri" : "aeiou",
  "content" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicRecipe = function(args, res, next) {
  /**
   * create recipe as public resource
   * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
   *
   * body RecipeRequest  (optional)
   * returns RecipeResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "uri" : "aeiou",
  "content" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

