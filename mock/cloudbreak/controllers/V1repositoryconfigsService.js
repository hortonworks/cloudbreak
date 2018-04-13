'use strict';

exports.postRepositoryConfigsValidation = function(args, res, next) {
  /**
   * validate repository configs fields, check their availability
   * Repository configs validation related operations
   *
   * body RepoConfigValidationRequest  (optional)
   * returns RepoConfigValidationResponse
   **/
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
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

