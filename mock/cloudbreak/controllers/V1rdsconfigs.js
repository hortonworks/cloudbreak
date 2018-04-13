'use strict';

var url = require('url');

var V1rdsconfigs = require('./V1rdsconfigsService');

module.exports.deletePrivateRds = function deletePrivateRds (req, res, next) {
  V1rdsconfigs.deletePrivateRds(req.swagger.params, res, next);
};

module.exports.deletePublicRds = function deletePublicRds (req, res, next) {
  V1rdsconfigs.deletePublicRds(req.swagger.params, res, next);
};

module.exports.deleteRds = function deleteRds (req, res, next) {
  V1rdsconfigs.deleteRds(req.swagger.params, res, next);
};

module.exports.getPrivateRds = function getPrivateRds (req, res, next) {
  V1rdsconfigs.getPrivateRds(req.swagger.params, res, next);
};

module.exports.getPrivatesRds = function getPrivatesRds (req, res, next) {
  V1rdsconfigs.getPrivatesRds(req.swagger.params, res, next);
};

module.exports.getPublicRds = function getPublicRds (req, res, next) {
  V1rdsconfigs.getPublicRds(req.swagger.params, res, next);
};

module.exports.getPublicsRds = function getPublicsRds (req, res, next) {
  V1rdsconfigs.getPublicsRds(req.swagger.params, res, next);
};

module.exports.getRds = function getRds (req, res, next) {
  V1rdsconfigs.getRds(req.swagger.params, res, next);
};

module.exports.postPrivateRds = function postPrivateRds (req, res, next) {
  V1rdsconfigs.postPrivateRds(req.swagger.params, res, next);
};

module.exports.postPublicRds = function postPublicRds (req, res, next) {
  V1rdsconfigs.postPublicRds(req.swagger.params, res, next);
};

module.exports.testRdsConnection = function testRdsConnection (req, res, next) {
  V1rdsconfigs.testRdsConnection(req.swagger.params, res, next);
};
