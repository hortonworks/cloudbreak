'use strict';

var url = require('url');

var V1repositoryconfigs = require('./V1repositoryconfigsService');

module.exports.postRepositoryConfigsValidation = function postRepositoryConfigsValidation (req, res, next) {
  V1repositoryconfigs.postRepositoryConfigsValidation(req.swagger.params, res, next);
};
