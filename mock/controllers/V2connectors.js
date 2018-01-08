'use strict';

var url = require('url');

var V2connectors = require('./V2connectorsService');

module.exports.getRegionsByCredentialId = function getRegionsByCredentialId (req, res, next) {
  V2connectors.getRegionsByCredentialId(req.swagger.params, res, next);
};

module.exports.getVmTypesByCredentialId = function getVmTypesByCredentialId (req, res, next) {
  V2connectors.getVmTypesByCredentialId(req.swagger.params, res, next);
};
