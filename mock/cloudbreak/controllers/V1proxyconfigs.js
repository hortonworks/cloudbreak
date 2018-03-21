'use strict';

var url = require('url');

var V1proxyconfigs = require('./V1proxyconfigsService');

module.exports.deletePrivateProxyConfig = function deletePrivateProxyConfig (req, res, next) {
  V1proxyconfigs.deletePrivateProxyConfig(req.swagger.params, res, next);
};

module.exports.deleteProxyConfig = function deleteProxyConfig (req, res, next) {
  V1proxyconfigs.deleteProxyConfig(req.swagger.params, res, next);
};

module.exports.deletePublicProxyConfig = function deletePublicProxyConfig (req, res, next) {
  V1proxyconfigs.deletePublicProxyConfig(req.swagger.params, res, next);
};

module.exports.getPrivateProxyConfig = function getPrivateProxyConfig (req, res, next) {
  V1proxyconfigs.getPrivateProxyConfig(req.swagger.params, res, next);
};

module.exports.getPrivatesProxyConfig = function getPrivatesProxyConfig (req, res, next) {
  V1proxyconfigs.getPrivatesProxyConfig(req.swagger.params, res, next);
};

module.exports.getProxyConfig = function getProxyConfig (req, res, next) {
  V1proxyconfigs.getProxyConfig(req.swagger.params, res, next);
};

module.exports.getPublicProxyConfig = function getPublicProxyConfig (req, res, next) {
  V1proxyconfigs.getPublicProxyConfig(req.swagger.params, res, next);
};

module.exports.getPublicsProxyConfig = function getPublicsProxyConfig (req, res, next) {
  V1proxyconfigs.getPublicsProxyConfig(req.swagger.params, res, next);
};

module.exports.postPrivateProxyConfig = function postPrivateProxyConfig (req, res, next) {
  V1proxyconfigs.postPrivateProxyConfig(req.swagger.params, res, next);
};

module.exports.postPublicProxyConfig = function postPublicProxyConfig (req, res, next) {
  V1proxyconfigs.postPublicProxyConfig(req.swagger.params, res, next);
};
