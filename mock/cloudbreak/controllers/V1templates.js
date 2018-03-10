'use strict';

var url = require('url');

var V1templates = require('./V1templatesService');

module.exports.deletePrivateTemplate = function deletePrivateTemplate (req, res, next) {
  V1templates.deletePrivateTemplate(req.swagger.params, res, next);
};

module.exports.deletePublicTemplate = function deletePublicTemplate (req, res, next) {
  V1templates.deletePublicTemplate(req.swagger.params, res, next);
};

module.exports.deleteTemplate = function deleteTemplate (req, res, next) {
  V1templates.deleteTemplate(req.swagger.params, res, next);
};

module.exports.getPrivateTemplate = function getPrivateTemplate (req, res, next) {
  V1templates.getPrivateTemplate(req.swagger.params, res, next);
};

module.exports.getPrivatesTemplate = function getPrivatesTemplate (req, res, next) {
  V1templates.getPrivatesTemplate(req.swagger.params, res, next);
};

module.exports.getPublicTemplate = function getPublicTemplate (req, res, next) {
  V1templates.getPublicTemplate(req.swagger.params, res, next);
};

module.exports.getPublicsTemplate = function getPublicsTemplate (req, res, next) {
  V1templates.getPublicsTemplate(req.swagger.params, res, next);
};

module.exports.getTemplate = function getTemplate (req, res, next) {
  V1templates.getTemplate(req.swagger.params, res, next);
};
