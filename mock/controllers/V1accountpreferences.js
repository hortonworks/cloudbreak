'use strict';

var url = require('url');

var V1accountpreferences = require('./V1accountpreferencesService');

module.exports.getAccountPreferencesEndpoint = function getAccountPreferencesEndpoint (req, res, next) {
  V1accountpreferences.getAccountPreferencesEndpoint(req.swagger.params, res, next);
};

module.exports.isPlatformSelectionDisabled = function isPlatformSelectionDisabled (req, res, next) {
  V1accountpreferences.isPlatformSelectionDisabled(req.swagger.params, res, next);
};

module.exports.platformEnablement = function platformEnablement (req, res, next) {
  V1accountpreferences.platformEnablement(req.swagger.params, res, next);
};

module.exports.postAccountPreferencesEndpoint = function postAccountPreferencesEndpoint (req, res, next) {
  V1accountpreferences.postAccountPreferencesEndpoint(req.swagger.params, res, next);
};

module.exports.putAccountPreferencesEndpoint = function putAccountPreferencesEndpoint (req, res, next) {
  V1accountpreferences.putAccountPreferencesEndpoint(req.swagger.params, res, next);
};

module.exports.validateAccountPreferencesEndpoint = function validateAccountPreferencesEndpoint (req, res, next) {
  V1accountpreferences.validateAccountPreferencesEndpoint(req.swagger.params, res, next);
};
