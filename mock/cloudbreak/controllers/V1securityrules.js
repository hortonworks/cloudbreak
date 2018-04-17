'use strict';

var utils = require('../utils/writer.js');
var V1securityrules = require('../service/V1securityrulesService');

module.exports.getDefaultSecurityRules = function getDefaultSecurityRules (req, res, next) {
  V1securityrules.getDefaultSecurityRules()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
