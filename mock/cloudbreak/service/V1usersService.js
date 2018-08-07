'use strict';


/**
 * remove current user from cache
 * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
 *
 * returns User
 **/
exports.evictCurrentUserDetails = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "username" : "username"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * remove user from cache (by username)
 * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
 *
 * id String 
 * body User  (optional)
 * returns String
 **/
exports.evictUserDetails = function(id,body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = "";
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * user related profile
 * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
 *
 * returns UserProfileResponse
 **/
exports.getUserProfile = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/users/default-profile.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * check that account user has any resources
 * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
 *
 * id String 
 * returns Boolean
 **/
exports.hasResourcesUser = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = true;
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * modify user related profile
 * Users can be invited under an account by the administrator, and all resources (e.g. resources, networks, blueprints, credentials, clusters) can be shared across account users
 *
 * body UserProfileRequest  (optional)
 * no response value expected for this operation
 **/
exports.modifyProfile = function(body) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}

