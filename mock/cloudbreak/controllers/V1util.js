'use strict';

var utils = require('../utils/writer.js');
var V1util = require('../service/V1utilService');

module.exports.checkClientVersion = function checkClientVersion (req, res, next) {
  var version = req.swagger.params['version'].value;
  V1util.checkClientVersion(version)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.createRDSDatabaseUtil = function createRDSDatabaseUtil (req, res, next) {
  var body = req.swagger.params['body'].value;
  var target = req.swagger.params['target'].value;
  V1util.createRDSDatabaseUtil(body,target)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getStackMatrixUtil = function getStackMatrixUtil (req, res, next) {
  V1util.getStackMatrixUtil()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.testAmbariDatabaseUtil = function testAmbariDatabaseUtil (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1util.testAmbariDatabaseUtil(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
