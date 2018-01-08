'use strict';

var url = require('url');

var V1usages = require('./V1usagesService');

module.exports.getAccountUsage = function getAccountUsage (req, res, next) {
  V1usages.getAccountUsage(req.swagger.params, res, next);
};

module.exports.getDailyFlexUsage = function getDailyFlexUsage (req, res, next) {
  V1usages.getDailyFlexUsage(req.swagger.params, res, next);
};

module.exports.getDeployerUsage = function getDeployerUsage (req, res, next) {
  V1usages.getDeployerUsage(req.swagger.params, res, next);
};

module.exports.getLatestFlexUsage = function getLatestFlexUsage (req, res, next) {
  V1usages.getLatestFlexUsage(req.swagger.params, res, next);
};

module.exports.getUserUsage = function getUserUsage (req, res, next) {
  V1usages.getUserUsage(req.swagger.params, res, next);
};
