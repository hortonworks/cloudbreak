'use strict';

var url = require('url');

var V1clusters = require('./V1clustersService');

module.exports.addCluster = function addCluster (req, res, next) {
  V1clusters.addCluster(req.swagger.params, res, next);
};

module.exports.deleteCluster = function deleteCluster (req, res, next) {
  V1clusters.deleteCluster(req.swagger.params, res, next);
};

module.exports.getCluster = function getCluster (req, res, next) {
  V1clusters.getCluster(req.swagger.params, res, next);
};

module.exports.getClusters = function getClusters (req, res, next) {
  V1clusters.getClusters(req.swagger.params, res, next);
};

module.exports.modifyCluster = function modifyCluster (req, res, next) {
  V1clusters.modifyCluster(req.swagger.params, res, next);
};

module.exports.setAutoscaleState = function setAutoscaleState (req, res, next) {
  V1clusters.setAutoscaleState(req.swagger.params, res, next);
};

module.exports.setState = function setState (req, res, next) {
  V1clusters.setState(req.swagger.params, res, next);
};
