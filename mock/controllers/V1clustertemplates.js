'use strict';

var url = require('url');

var V1clustertemplates = require('./V1clustertemplatesService');

module.exports.deleteClusterTemplate = function deleteClusterTemplate (req, res, next) {
  V1clustertemplates.deleteClusterTemplate(req.swagger.params, res, next);
};

module.exports.deletePrivateClusterTemplate = function deletePrivateClusterTemplate (req, res, next) {
  V1clustertemplates.deletePrivateClusterTemplate(req.swagger.params, res, next);
};

module.exports.deletePublicClusterTemplate = function deletePublicClusterTemplate (req, res, next) {
  V1clustertemplates.deletePublicClusterTemplate(req.swagger.params, res, next);
};

module.exports.getClusterTemplate = function getClusterTemplate (req, res, next) {
  V1clustertemplates.getClusterTemplate(req.swagger.params, res, next);
};

module.exports.getPrivateClusterTemplate = function getPrivateClusterTemplate (req, res, next) {
  V1clustertemplates.getPrivateClusterTemplate(req.swagger.params, res, next);
};

module.exports.getPrivatesClusterTemplate = function getPrivatesClusterTemplate (req, res, next) {
  V1clustertemplates.getPrivatesClusterTemplate(req.swagger.params, res, next);
};

module.exports.getPublicClusterTemplate = function getPublicClusterTemplate (req, res, next) {
  V1clustertemplates.getPublicClusterTemplate(req.swagger.params, res, next);
};

module.exports.getPublicsClusterTemplate = function getPublicsClusterTemplate (req, res, next) {
  V1clustertemplates.getPublicsClusterTemplate(req.swagger.params, res, next);
};

module.exports.postPrivateClusterTemplate = function postPrivateClusterTemplate (req, res, next) {
  V1clustertemplates.postPrivateClusterTemplate(req.swagger.params, res, next);
};

module.exports.postPublicClusterTemplate = function postPublicClusterTemplate (req, res, next) {
  V1clustertemplates.postPublicClusterTemplate(req.swagger.params, res, next);
};
