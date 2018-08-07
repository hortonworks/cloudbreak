'use strict';

var utils = require('../utils/writer.js');
var V1recipes = require('../service/V1recipesService');

module.exports.deletePrivateRecipe = function deletePrivateRecipe (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1recipes.deletePrivateRecipe(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicRecipe = function deletePublicRecipe (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1recipes.deletePublicRecipe(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deleteRecipe = function deleteRecipe (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1recipes.deleteRecipe(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateRecipe = function getPrivateRecipe (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1recipes.getPrivateRecipe(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesRecipe = function getPrivatesRecipe (req, res, next) {
  V1recipes.getPrivatesRecipe()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicRecipe = function getPublicRecipe (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1recipes.getPublicRecipe(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsRecipe = function getPublicsRecipe (req, res, next) {
  V1recipes.getPublicsRecipe()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getRecipe = function getRecipe (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1recipes.getRecipe(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getRecipeRequestFromName = function getRecipeRequestFromName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1recipes.getRecipeRequestFromName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateRecipe = function postPrivateRecipe (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1recipes.postPrivateRecipe(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicRecipe = function postPublicRecipe (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1recipes.postPublicRecipe(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
