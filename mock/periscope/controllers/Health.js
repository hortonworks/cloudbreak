'use strict';

var url = require('url');

var Info = require('./HealthService');

module.exports.getPeriscopeHealth = function getPeriscopeHealth (req, res, next) {
    Info.getPeriscopeHealth(req.swagger.params, res, next);
};