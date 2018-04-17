'use strict';


/**
 * validate repository configs fields, check their availability
 * Repository configs validation related operations
 *
 * body RepoConfigValidationRequest  (optional)
 * returns RepoConfigValidationResponse
 **/
exports.postRepositoryConfigsValidation = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "utilsBaseURL" : true,
  "ambariGpgKeyUrl" : true,
  "mpackUrl" : true,
  "stackBaseURL" : true,
  "versionDefinitionFileUrl" : true,
  "ambariBaseUrl" : true
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

