'use strict';

var url = require('url');

var V1users = require('./V1usersService');

module.exports.evictCurrentUserDetails = function evictCurrentUserDetails (req, res, next) {
  V1users.evictCurrentUserDetails(req.swagger.params, res, next);
};

module.exports.evictUserDetails = function evictUserDetails (req, res, next) {
  V1users.evictUserDetails(req.swagger.params, res, next);
};

module.exports.getUserProfile = function getUserProfile (req, res, next) {
  V1users.getUserProfile(req.swagger.params, res, next);
};

module.exports.hasResourcesUser = function hasResourcesUser (req, res, next) {
  V1users.hasResourcesUser(req.swagger.params, res, next);
};

module.exports.modifyProfile = function modifyProfile (req, res, next) {
  V1users.modifyProfile(req.swagger.params, res, next);
};
