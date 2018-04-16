'use strict';

var url = require('url');

var V1util = require('./V1utilService');

module.exports.checkClientVersion = function checkClientVersion (req, res, next) {
  V1util.checkClientVersion(req.swagger.params, res, next);
};

module.exports.createRDSDatabaseUtil = function createRDSDatabaseUtil (req, res, next) {
  V1util.createRDSDatabaseUtil(req.swagger.params, res, next);
};

module.exports.getStackMatrixUtil = function getStackMatrixUtil (req, res, next) {
  V1util.getStackMatrixUtil(req.swagger.params, res, next);
};

module.exports.testAmbariDatabaseUtil = function testAmbariDatabaseUtil (req, res, next) {
  V1util.testAmbariDatabaseUtil(req.swagger.params, res, next);
};
