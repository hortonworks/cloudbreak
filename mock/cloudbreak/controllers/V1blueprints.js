'use strict';

var url = require('url');

var V1blueprints = require('./V1blueprintsService');

module.exports.deleteBlueprint = function deleteBlueprint (req, res, next) {
  V1blueprints.deleteBlueprint(req.swagger.params, res, next);
};

module.exports.deletePrivateBlueprint = function deletePrivateBlueprint (req, res, next) {
  V1blueprints.deletePrivateBlueprint(req.swagger.params, res, next);
};

module.exports.deletePublicBlueprint = function deletePublicBlueprint (req, res, next) {
  V1blueprints.deletePublicBlueprint(req.swagger.params, res, next);
};

module.exports.getBlueprint = function getBlueprint (req, res, next) {
  V1blueprints.getBlueprint(req.swagger.params, res, next);
};

module.exports.getBlueprintRequestFromId = function getBlueprintRequestFromId (req, res, next) {
  V1blueprints.getBlueprintRequestFromId(req.swagger.params, res, next);
};

module.exports.getPrivateBlueprint = function getPrivateBlueprint (req, res, next) {
  V1blueprints.getPrivateBlueprint(req.swagger.params, res, next);
};

module.exports.getPrivatesBlueprint = function getPrivatesBlueprint (req, res, next) {
  V1blueprints.getPrivatesBlueprint(req.swagger.params, res, next);
};

module.exports.getPublicBlueprint = function getPublicBlueprint (req, res, next) {
  V1blueprints.getPublicBlueprint(req.swagger.params, res, next);
};

module.exports.getPublicsBlueprint = function getPublicsBlueprint (req, res, next) {
  V1blueprints.getPublicsBlueprint(req.swagger.params, res, next);
};

module.exports.postPrivateBlueprint = function postPrivateBlueprint (req, res, next) {
  V1blueprints.postPrivateBlueprint(req.swagger.params, res, next);
};

module.exports.postPublicBlueprint = function postPublicBlueprint (req, res, next) {
  V1blueprints.postPublicBlueprint(req.swagger.params, res, next);
};
