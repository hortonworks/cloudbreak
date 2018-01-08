'use strict';

var url = require('url');

var V1topologies = require('./V1topologiesService');

module.exports.deleteTopology = function deleteTopology (req, res, next) {
  V1topologies.deleteTopology(req.swagger.params, res, next);
};

module.exports.getPublicsTopology = function getPublicsTopology (req, res, next) {
  V1topologies.getPublicsTopology(req.swagger.params, res, next);
};

module.exports.getTopology = function getTopology (req, res, next) {
  V1topologies.getTopology(req.swagger.params, res, next);
};

module.exports.postPublicTopology = function postPublicTopology (req, res, next) {
  V1topologies.postPublicTopology(req.swagger.params, res, next);
};
