'use strict';

exports.getDefaultSecurityRules = function(args, res, next) {
  /**
   * get default security rules
   * Security Rules operations
   *
   * returns SecurityRulesResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/securityrules/securityrules.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

