'use strict';


/**
 * delete private recipe by name
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateRecipe = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private recipe by name
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicRecipe = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete recipe by id
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteRecipe = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve a private recipe by name
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * name String 
 * returns RecipeResponse
 **/
exports.getPrivateRecipe = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "uri" : "uri",
  "content" : "content"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private recipes
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * returns List
 **/
exports.getPrivatesRecipe = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "uri" : "uri",
  "content" : "content"
}, {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "uri" : "uri",
  "content" : "content"
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) recipe by name
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * name String 
 * returns RecipeResponse
 **/
exports.getPublicRecipe = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "uri" : "uri",
  "content" : "content"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) recipes
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * returns List
 **/
exports.getPublicsRecipe = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/recipes/recipes.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve recipe by id
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * id Long 
 * returns RecipeResponse
 **/
exports.getRecipe = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "uri" : "uri",
  "content" : "content"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve recipe request by recipe name
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * name String 
 * returns RecipeRequest
 **/
exports.getRecipeRequestFromName = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/recipes/recipes.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create recipe as private resource
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * body RecipeRequest  (optional)
 * returns RecipeResponse
 **/
exports.postPrivateRecipe = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "uri" : "uri",
  "content" : "content"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create recipe as public resource
 * Recipes are basically script extensions to a cluster that run on a set of nodes before or after the Ambari cluster installation.
 *
 * body RecipeRequest  (optional)
 * returns RecipeResponse
 **/
exports.postPublicRecipe = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "recipeType" : "PRE_AMBARI_START",
  "public" : false,
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "uri" : "uri",
  "content" : "content"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

