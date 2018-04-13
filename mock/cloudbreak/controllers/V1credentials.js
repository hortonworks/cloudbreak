'use strict';

var url = require('url');

var V1credentials = require('./V1credentialsService');

module.exports.deleteCredential = function deleteCredential (req, res, next) {
  V1credentials.deleteCredential(req.swagger.params, res, next);
};

module.exports.deletePrivateCredential = function deletePrivateCredential (req, res, next) {
  V1credentials.deletePrivateCredential(req.swagger.params, res, next);
};

module.exports.deletePublicCredential = function deletePublicCredential (req, res, next) {
  V1credentials.deletePublicCredential(req.swagger.params, res, next);
};

module.exports.getCredential = function getCredential (req, res, next) {
  V1credentials.getCredential(req.swagger.params, res, next);
};

module.exports.getPrivateCredential = function getPrivateCredential (req, res, next) {
  V1credentials.getPrivateCredential(req.swagger.params, res, next);
};

module.exports.getPrivatesCredential = function getPrivatesCredential (req, res, next) {
  V1credentials.getPrivatesCredential(req.swagger.params, res, next);
};

module.exports.getPublicCredential = function getPublicCredential (req, res, next) {
  V1credentials.getPublicCredential(req.swagger.params, res, next);
};

module.exports.getPublicsCredential = function getPublicsCredential (req, res, next) {
  V1credentials.getPublicsCredential(req.swagger.params, res, next);
};

module.exports.postPrivateCredential = function postPrivateCredential (req, res, next) {
  V1credentials.postPrivateCredential(req.swagger.params, res, next);
};

module.exports.postPublicCredential = function postPublicCredential (req, res, next) {
  V1credentials.postPublicCredential(req.swagger.params, res, next);
};

module.exports.privateInteractiveLoginCredential = function privateInteractiveLoginCredential (req, res, next) {
  V1credentials.privateInteractiveLoginCredential(req.swagger.params, res, next);
};

module.exports.publicInteractiveLoginCredential = function publicInteractiveLoginCredential (req, res, next) {
  V1credentials.publicInteractiveLoginCredential(req.swagger.params, res, next);
};

module.exports.putPrivateCredential = function putPrivateCredential (req, res, next) {
  V1credentials.putPrivateCredential(req.swagger.params, res, next);
};

module.exports.putPublicCredential = function putPublicCredential (req, res, next) {
  V1credentials.putPublicCredential(req.swagger.params, res, next);
};
