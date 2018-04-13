'use strict';

exports.evictCurrentUserDetails = function(args, res, next) {
  /**
   * remove current user from cache
   * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
   *
   * returns User
   **/
  var examples = {};
  examples['application/json'] = {
  "username" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.evictUserDetails = function(args, res, next) {
  /**
   * remove user from cache (by username)
   * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
   *
   * id String 
   * body User  (optional)
   * returns String
   **/
  var examples = {};
  examples['application/json'] = "aeiou";
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getUserProfile = function(args, res, next) {
  /**
   * user related profile
   * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
   *
   * returns UserProfileResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/users/default-profile.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.hasResourcesUser = function(args, res, next) {
  /**
   * check that account user has any resources
   * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
   *
   * id String 
   * returns Boolean
   **/
  var examples = {};
  examples['application/json'] = true;
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.modifyProfile = function(args, res, next) {
  /**
   * modify user related profile
   * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
   *
   * body UserProfileRequest  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

