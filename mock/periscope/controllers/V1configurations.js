'use strict';

var url = require('url');

var V1configurations = require('./V1configurationsService');

module.exports.getScalingConfiguration = function getScalingConfiguration (req, res, next) {
  V1configurations.getScalingConfiguration(req.swagger.params, res, next);
};

module.exports.setScalingConfiguration = function setScalingConfiguration (req, res, next) {
  V1configurations.setScalingConfiguration(req.swagger.params, res, next);
};
