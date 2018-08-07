'use strict';

var utils = require('../utils/writer.js');
var V1repositoryconfigs = require('../service/V1repositoryconfigsService');

module.exports.postRepositoryConfigsValidation = function postRepositoryConfigsValidation (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1repositoryconfigs.postRepositoryConfigsValidation(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
