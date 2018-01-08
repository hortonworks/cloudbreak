'use strict';

var url = require('url');

var V1securityrules = require('./V1securityrulesService');

module.exports.getDefaultSecurityRules = function getDefaultSecurityRules (req, res, next) {
  V1securityrules.getDefaultSecurityRules(req.swagger.params, res, next);
};
