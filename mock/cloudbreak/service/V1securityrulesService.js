'use strict';


/**
 * get default security rules
 * Security Rules operations
 *
 * returns SecurityRulesResponse
 **/
exports.getDefaultSecurityRules = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/securityrules/securityrules.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

