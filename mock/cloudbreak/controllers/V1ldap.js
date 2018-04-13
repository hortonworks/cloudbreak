'use strict';

var url = require('url');

var V1ldap = require('./V1ldapService');

module.exports.deleteLdap = function deleteLdap (req, res, next) {
  V1ldap.deleteLdap(req.swagger.params, res, next);
};

module.exports.deletePrivateLdap = function deletePrivateLdap (req, res, next) {
  V1ldap.deletePrivateLdap(req.swagger.params, res, next);
};

module.exports.deletePublicLdap = function deletePublicLdap (req, res, next) {
  V1ldap.deletePublicLdap(req.swagger.params, res, next);
};

module.exports.getLdap = function getLdap (req, res, next) {
  V1ldap.getLdap(req.swagger.params, res, next);
};

module.exports.getPrivateLdap = function getPrivateLdap (req, res, next) {
  V1ldap.getPrivateLdap(req.swagger.params, res, next);
};

module.exports.getPrivatesLdap = function getPrivatesLdap (req, res, next) {
  V1ldap.getPrivatesLdap(req.swagger.params, res, next);
};

module.exports.getPublicLdap = function getPublicLdap (req, res, next) {
  V1ldap.getPublicLdap(req.swagger.params, res, next);
};

module.exports.getPublicsLdap = function getPublicsLdap (req, res, next) {
  V1ldap.getPublicsLdap(req.swagger.params, res, next);
};

module.exports.postLdapConnectionTest = function postLdapConnectionTest (req, res, next) {
  V1ldap.postLdapConnectionTest(req.swagger.params, res, next);
};

module.exports.postPrivateLdap = function postPrivateLdap (req, res, next) {
  V1ldap.postPrivateLdap(req.swagger.params, res, next);
};

module.exports.postPublicLdap = function postPublicLdap (req, res, next) {
  V1ldap.postPublicLdap(req.swagger.params, res, next);
};
