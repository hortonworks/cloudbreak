'use strict';

var url = require('url');

var Info = require('./InfoService');

module.exports.getCloudbreakInfo = function getCloudbreakInfo (req, res, next) {
    Info.getCloudbreakInfo(req.swagger.params, res, next);
};

module.exports.getCloudbreakHealth = function getCloudbreakHealth (req, res, next) {
    Info.getCloudbreakHealth(req.swagger.params, res, next);
};

module.exports.getPeriscopeHealth = function getPeriscopeHealth (req, res, next) {
    Info.getPeriscopeHealth(req.swagger.params, res, next);
};