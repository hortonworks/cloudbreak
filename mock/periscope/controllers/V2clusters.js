'use strict';

var url = require('url');

var V2clusters = require('./V2clustersService');

module.exports.deleteByCloudbreakCluster = function deleteByCloudbreakCluster (req, res, next) {
  V2clusters.deleteByCloudbreakCluster(req.swagger.params, res, next);
};

module.exports.disableAutoscaleStateByCloudbreakCluster = function disableAutoscaleStateByCloudbreakCluster (req, res, next) {
  V2clusters.disableAutoscaleStateByCloudbreakCluster(req.swagger.params, res, next);
};

module.exports.enableAutoscaleStateByCloudbreakCluster = function enableAutoscaleStateByCloudbreakCluster (req, res, next) {
  V2clusters.enableAutoscaleStateByCloudbreakCluster(req.swagger.params, res, next);
};

module.exports.getByCloudbreakCluster = function getByCloudbreakCluster (req, res, next) {
  V2clusters.getByCloudbreakCluster(req.swagger.params, res, next);
};

module.exports.modifyByCloudbreakCluster = function modifyByCloudbreakCluster (req, res, next) {
  V2clusters.modifyByCloudbreakCluster(req.swagger.params, res, next);
};

module.exports.runByCloudbreakCluster = function runByCloudbreakCluster (req, res, next) {
  V2clusters.runByCloudbreakCluster(req.swagger.params, res, next);
};

module.exports.suspendByCloudbreakCluster = function suspendByCloudbreakCluster (req, res, next) {
  V2clusters.suspendByCloudbreakCluster(req.swagger.params, res, next);
};
