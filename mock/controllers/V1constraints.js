'use strict';

var url = require('url');

var V1constraints = require('./V1constraintsService');

module.exports.deleteConstraint = function deleteConstraint (req, res, next) {
  V1constraints.deleteConstraint(req.swagger.params, res, next);
};

module.exports.deletePrivateConstraint = function deletePrivateConstraint (req, res, next) {
  V1constraints.deletePrivateConstraint(req.swagger.params, res, next);
};

module.exports.deletePublicConstraint = function deletePublicConstraint (req, res, next) {
  V1constraints.deletePublicConstraint(req.swagger.params, res, next);
};

module.exports.getConstraint = function getConstraint (req, res, next) {
  V1constraints.getConstraint(req.swagger.params, res, next);
};

module.exports.getPrivateConstraint = function getPrivateConstraint (req, res, next) {
  V1constraints.getPrivateConstraint(req.swagger.params, res, next);
};

module.exports.getPrivatesConstraint = function getPrivatesConstraint (req, res, next) {
  V1constraints.getPrivatesConstraint(req.swagger.params, res, next);
};

module.exports.getPublicConstraint = function getPublicConstraint (req, res, next) {
  V1constraints.getPublicConstraint(req.swagger.params, res, next);
};

module.exports.getPublicsConstraint = function getPublicsConstraint (req, res, next) {
  V1constraints.getPublicsConstraint(req.swagger.params, res, next);
};

module.exports.postPrivateConstraint = function postPrivateConstraint (req, res, next) {
  V1constraints.postPrivateConstraint(req.swagger.params, res, next);
};

module.exports.postPublicConstraint = function postPublicConstraint (req, res, next) {
  V1constraints.postPublicConstraint(req.swagger.params, res, next);
};
