'use strict';

var url = require('url');

var V1clusters = require('./V1clustersService');

module.exports.deleteCluster = function deleteCluster (req, res, next) {
  V1clusters.deleteCluster(req.swagger.params, res, next);
};

module.exports.failureReportCluster = function failureReportCluster (req, res, next) {
  V1clusters.failureReportCluster(req.swagger.params, res, next);
};

module.exports.getCluster = function getCluster (req, res, next) {
  V1clusters.getCluster(req.swagger.params, res, next);
};

module.exports.getConfigsCluster = function getConfigsCluster (req, res, next) {
  V1clusters.getConfigsCluster(req.swagger.params, res, next);
};

module.exports.getFullCluster = function getFullCluster (req, res, next) {
  V1clusters.getFullCluster(req.swagger.params, res, next);
};

module.exports.getPrivateCluster = function getPrivateCluster (req, res, next) {
  V1clusters.getPrivateCluster(req.swagger.params, res, next);
};

module.exports.getPublicCluster = function getPublicCluster (req, res, next) {
  V1clusters.getPublicCluster(req.swagger.params, res, next);
};

module.exports.postCluster = function postCluster (req, res, next) {
  V1clusters.postCluster(req.swagger.params, res, next);
};

module.exports.putCluster = function putCluster (req, res, next) {
  V1clusters.putCluster(req.swagger.params, res, next);
};

module.exports.repairCluster = function repairCluster (req, res, next) {
  V1clusters.repairCluster(req.swagger.params, res, next);
};

module.exports.upgradeCluster = function upgradeCluster (req, res, next) {
  V1clusters.upgradeCluster(req.swagger.params, res, next);
};
