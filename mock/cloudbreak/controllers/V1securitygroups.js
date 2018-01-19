'use strict';

var url = require('url');

var V1securitygroups = require('./V1securitygroupsService');

module.exports.deletePrivateSecurityGroup = function deletePrivateSecurityGroup (req, res, next) {
  V1securitygroups.deletePrivateSecurityGroup(req.swagger.params, res, next);
};

module.exports.deletePublicSecurityGroup = function deletePublicSecurityGroup (req, res, next) {
  V1securitygroups.deletePublicSecurityGroup(req.swagger.params, res, next);
};

module.exports.deleteSecurityGroup = function deleteSecurityGroup (req, res, next) {
  V1securitygroups.deleteSecurityGroup(req.swagger.params, res, next);
};

module.exports.getPrivateSecurityGroup = function getPrivateSecurityGroup (req, res, next) {
  V1securitygroups.getPrivateSecurityGroup(req.swagger.params, res, next);
};

module.exports.getPrivatesSecurityGroup = function getPrivatesSecurityGroup (req, res, next) {
  V1securitygroups.getPrivatesSecurityGroup(req.swagger.params, res, next);
};

module.exports.getPublicSecurityGroup = function getPublicSecurityGroup (req, res, next) {
  V1securitygroups.getPublicSecurityGroup(req.swagger.params, res, next);
};

module.exports.getPublicsSecurityGroup = function getPublicsSecurityGroup (req, res, next) {
  V1securitygroups.getPublicsSecurityGroup(req.swagger.params, res, next);
};

module.exports.getSecurityGroup = function getSecurityGroup (req, res, next) {
  V1securitygroups.getSecurityGroup(req.swagger.params, res, next);
};

module.exports.postPrivateSecurityGroup = function postPrivateSecurityGroup (req, res, next) {
  V1securitygroups.postPrivateSecurityGroup(req.swagger.params, res, next);
};

module.exports.postPublicSecurityGroup = function postPublicSecurityGroup (req, res, next) {
  V1securitygroups.postPublicSecurityGroup(req.swagger.params, res, next);
};
