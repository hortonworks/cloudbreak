'use strict';

var url = require('url');

var V1policies = require('./V1policiesService');

module.exports.addScalingPolicy = function addScalingPolicy (req, res, next) {
  V1policies.addScalingPolicy(req.swagger.params, res, next);
};

module.exports.deleteScalingPolicy = function deleteScalingPolicy (req, res, next) {
  V1policies.deleteScalingPolicy(req.swagger.params, res, next);
};

module.exports.getScalingPolicies = function getScalingPolicies (req, res, next) {
  V1policies.getScalingPolicies(req.swagger.params, res, next);
};

module.exports.updateScalingPolicy = function updateScalingPolicy (req, res, next) {
  V1policies.updateScalingPolicy(req.swagger.params, res, next);
};
