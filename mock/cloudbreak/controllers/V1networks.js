'use strict';

var url = require('url');

var V1networks = require('./V1networksService');

module.exports.deleteNetwork = function deleteNetwork (req, res, next) {
  V1networks.deleteNetwork(req.swagger.params, res, next);
};

module.exports.deletePrivateNetwork = function deletePrivateNetwork (req, res, next) {
  V1networks.deletePrivateNetwork(req.swagger.params, res, next);
};

module.exports.deletePublicNetwork = function deletePublicNetwork (req, res, next) {
  V1networks.deletePublicNetwork(req.swagger.params, res, next);
};

module.exports.getNetwork = function getNetwork (req, res, next) {
  V1networks.getNetwork(req.swagger.params, res, next);
};

module.exports.getPrivateNetwork = function getPrivateNetwork (req, res, next) {
  V1networks.getPrivateNetwork(req.swagger.params, res, next);
};

module.exports.getPrivatesNetwork = function getPrivatesNetwork (req, res, next) {
  V1networks.getPrivatesNetwork(req.swagger.params, res, next);
};

module.exports.getPublicNetwork = function getPublicNetwork (req, res, next) {
  V1networks.getPublicNetwork(req.swagger.params, res, next);
};

module.exports.getPublicsNetwork = function getPublicsNetwork (req, res, next) {
  V1networks.getPublicsNetwork(req.swagger.params, res, next);
};

module.exports.postPrivateNetwork = function postPrivateNetwork (req, res, next) {
  V1networks.postPrivateNetwork(req.swagger.params, res, next);
};

module.exports.postPublicNetwork = function postPublicNetwork (req, res, next) {
  V1networks.postPublicNetwork(req.swagger.params, res, next);
};
