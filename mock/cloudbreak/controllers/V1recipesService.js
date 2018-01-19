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
  examples['application/json'] = 
  [
    {
      "name":"pre-start-recipe",
      "description":"mock test recipe",
      "recipeType":"PRE_AMBARI_START",
      "content":"IyEvYmluL2Jhc2gKdG91Y2ggL3ByZS1pbnN0YWxsCmVjaG8gIkhlbGxvIFByZS1JbnN0YWxsIiA+PiAvcHJlLWluc3RhbGw=",
      "uri":"https://gist.githubusercontent.com/aszegedi/a31408e3110eee0ffa34d29db25519d4/raw/be9fc7198d64a9a98ce319a867a6c5e6270386b4/pre-install.sh",
      "id":1,
      "public":true
    }
  ];
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

