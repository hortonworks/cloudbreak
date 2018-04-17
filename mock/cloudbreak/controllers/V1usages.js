'use strict';

var utils = require('../utils/writer.js');
var V1usages = require('../service/V1usagesService');

module.exports.getAccountUsage = function getAccountUsage (req, res, next) {
  var since = req.swagger.params['since'].value;
  var filterenddate = req.swagger.params['filterenddate'].value;
  var user = req.swagger.params['user'].value;
  var cloud = req.swagger.params['cloud'].value;
  var zone = req.swagger.params['zone'].value;
  V1usages.getAccountUsage(since,filterenddate,user,cloud,zone)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getDailyFlexUsage = function getDailyFlexUsage (req, res, next) {
  V1usages.getDailyFlexUsage()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getDeployerUsage = function getDeployerUsage (req, res, next) {
  var since = req.swagger.params['since'].value;
  var filterenddate = req.swagger.params['filterenddate'].value;
  var user = req.swagger.params['user'].value;
  var account = req.swagger.params['account'].value;
  var cloud = req.swagger.params['cloud'].value;
  var zone = req.swagger.params['zone'].value;
  V1usages.getDeployerUsage(since,filterenddate,user,account,cloud,zone)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getLatestFlexUsage = function getLatestFlexUsage (req, res, next) {
  V1usages.getLatestFlexUsage()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getUserUsage = function getUserUsage (req, res, next) {
  var since = req.swagger.params['since'].value;
  var filterenddate = req.swagger.params['filterenddate'].value;
  var cloud = req.swagger.params['cloud'].value;
  var zone = req.swagger.params['zone'].value;
  V1usages.getUserUsage(since,filterenddate,cloud,zone)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
