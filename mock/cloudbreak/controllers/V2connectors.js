'use strict';

var utils = require('../utils/writer.js');
var V2connectors = require('../service/V2connectorsService');

module.exports.getRegionsByCredentialId = function getRegionsByCredentialId (req, res, next) {
  var body = req.swagger.params['body'].value;
  V2connectors.getRegionsByCredentialId(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getVmTypesByCredentialId = function getVmTypesByCredentialId (req, res, next) {
  var body = req.swagger.params['body'].value;
  V2connectors.getVmTypesByCredentialId(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
