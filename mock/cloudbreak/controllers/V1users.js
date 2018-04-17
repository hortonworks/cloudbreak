'use strict';

var utils = require('../utils/writer.js');
var V1users = require('../service/V1usersService');

module.exports.evictCurrentUserDetails = function evictCurrentUserDetails (req, res, next) {
  V1users.evictCurrentUserDetails()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.evictUserDetails = function evictUserDetails (req, res, next) {
  var id = req.swagger.params['id'].value;
  var body = req.swagger.params['body'].value;
  V1users.evictUserDetails(id,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getUserProfile = function getUserProfile (req, res, next) {
  V1users.getUserProfile()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.hasResourcesUser = function hasResourcesUser (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1users.hasResourcesUser(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.modifyProfile = function modifyProfile (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1users.modifyProfile(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
