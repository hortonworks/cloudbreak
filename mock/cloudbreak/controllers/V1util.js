'use strict';

var url = require('url');

var V1util = require('./V1utilService');

module.exports.checkClientVersion = function checkClientVersion (req, res, next) {
  V1util.checkClientVersion(req.swagger.params, res, next);
};

module.exports.createRDSDatabaseUtil = function createRDSDatabaseUtil (req, res, next) {
  V1util.createRDSDatabaseUtil(req.swagger.params, res, next);
};

module.exports.testAmbariDatabaseUtil = function testAmbariDatabaseUtil (req, res, next) {
  V1util.testAmbariDatabaseUtil(req.swagger.params, res, next);
};

module.exports.testLdapConnectionByIdUtil = function testLdapConnectionByIdUtil (req, res, next) {
  V1util.testLdapConnectionByIdUtil(req.swagger.params, res, next);
};

module.exports.testLdapConnectionUtil = function testLdapConnectionUtil (req, res, next) {
  V1util.testLdapConnectionUtil(req.swagger.params, res, next);
};

module.exports.testRdsConnectionByIdUtil = function testRdsConnectionByIdUtil (req, res, next) {
  V1util.testRdsConnectionByIdUtil(req.swagger.params, res, next);
};

module.exports.testRdsConnectionUtil = function testRdsConnectionUtil (req, res, next) {
  V1util.testRdsConnectionUtil(req.swagger.params, res, next);
};
