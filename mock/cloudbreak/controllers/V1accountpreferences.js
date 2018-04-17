'use strict';

var utils = require('../utils/writer.js');
var V1accountpreferences = require('../service/V1accountpreferencesService');

module.exports.getAccountPreferencesEndpoint = function getAccountPreferencesEndpoint (req, res, next) {
  V1accountpreferences.getAccountPreferencesEndpoint()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.isPlatformSelectionDisabled = function isPlatformSelectionDisabled (req, res, next) {
  V1accountpreferences.isPlatformSelectionDisabled()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.platformEnablement = function platformEnablement (req, res, next) {
  V1accountpreferences.platformEnablement()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postAccountPreferencesEndpoint = function postAccountPreferencesEndpoint (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1accountpreferences.postAccountPreferencesEndpoint(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putAccountPreferencesEndpoint = function putAccountPreferencesEndpoint (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1accountpreferences.putAccountPreferencesEndpoint(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.validateAccountPreferencesEndpoint = function validateAccountPreferencesEndpoint (req, res, next) {
  V1accountpreferences.validateAccountPreferencesEndpoint()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
