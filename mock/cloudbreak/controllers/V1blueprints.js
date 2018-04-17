'use strict';

var utils = require('../utils/writer.js');
var V1blueprints = require('../service/V1blueprintsService');

module.exports.deleteBlueprint = function deleteBlueprint (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1blueprints.deleteBlueprint(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateBlueprint = function deletePrivateBlueprint (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1blueprints.deletePrivateBlueprint(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicBlueprint = function deletePublicBlueprint (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1blueprints.deletePublicBlueprint(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getBlueprint = function getBlueprint (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1blueprints.getBlueprint(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getBlueprintRequestFromId = function getBlueprintRequestFromId (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1blueprints.getBlueprintRequestFromId(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateBlueprint = function getPrivateBlueprint (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1blueprints.getPrivateBlueprint(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesBlueprint = function getPrivatesBlueprint (req, res, next) {
  V1blueprints.getPrivatesBlueprint()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicBlueprint = function getPublicBlueprint (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1blueprints.getPublicBlueprint(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsBlueprint = function getPublicsBlueprint (req, res, next) {
  V1blueprints.getPublicsBlueprint()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateBlueprint = function postPrivateBlueprint (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1blueprints.postPrivateBlueprint(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicBlueprint = function postPublicBlueprint (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1blueprints.postPublicBlueprint(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
