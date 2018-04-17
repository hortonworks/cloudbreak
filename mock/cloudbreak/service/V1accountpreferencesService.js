'use strict';


/**
 * retrieve account preferences for admin user
 * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
 *
 * returns AccountPreferencesResponse
 **/
exports.getAccountPreferencesEndpoint = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/accountpreferences/accountpreferences.json')
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * is platform selection disabled
 * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
 *
 * returns Map
 **/
exports.isPlatformSelectionDisabled = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/accountpreferences/platforms.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * is platform selection enabled
 * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
 *
 * returns Map
 **/
exports.platformEnablement = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/accountpreferences/platforms.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * post account preferences of admin user
 * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
 *
 * body AccountPreferencesRequest  (optional)
 * returns AccountPreferencesResponse
 **/
exports.postAccountPreferencesEndpoint = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "maxNumberOfNodesPerCluster" : 0,
  "userTimeToLive" : 0,
  "supportedExternalDatabases" : [ {
    "databases" : [ {
      "databaseName" : "databaseName",
      "jdbcPrefix" : "jdbcPrefix",
      "versions" : [ "versions", "versions" ],
      "displayName" : "displayName"
    }, {
      "databaseName" : "databaseName",
      "jdbcPrefix" : "jdbcPrefix",
      "versions" : [ "versions", "versions" ],
      "displayName" : "displayName"
    } ],
    "displayName" : "displayName",
    "name" : "name"
  }, {
    "databases" : [ {
      "databaseName" : "databaseName",
      "jdbcPrefix" : "jdbcPrefix",
      "versions" : [ "versions", "versions" ],
      "displayName" : "displayName"
    }, {
      "databaseName" : "databaseName",
      "jdbcPrefix" : "jdbcPrefix",
      "versions" : [ "versions", "versions" ],
      "displayName" : "displayName"
    } ],
    "displayName" : "displayName",
    "name" : "name"
  } ],
  "maxNumberOfClustersPerUser" : 0,
  "clusterTimeToLive" : 0,
  "allowedInstanceTypes" : [ "allowedInstanceTypes", "allowedInstanceTypes" ],
  "maxNumberOfClusters" : 0,
  "defaultTags" : {
    "key" : "defaultTags"
  },
  "platforms" : "platforms",
  "smartsenseEnabled" : false
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * update account preferences of admin user
 * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
 *
 * body AccountPreferencesRequest  (optional)
 * returns AccountPreferencesResponse
 **/
exports.putAccountPreferencesEndpoint = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "maxNumberOfNodesPerCluster" : 0,
  "userTimeToLive" : 0,
  "supportedExternalDatabases" : [ {
    "databases" : [ {
      "databaseName" : "databaseName",
      "jdbcPrefix" : "jdbcPrefix",
      "versions" : [ "versions", "versions" ],
      "displayName" : "displayName"
    }, {
      "databaseName" : "databaseName",
      "jdbcPrefix" : "jdbcPrefix",
      "versions" : [ "versions", "versions" ],
      "displayName" : "displayName"
    } ],
    "displayName" : "displayName",
    "name" : "name"
  }, {
    "databases" : [ {
      "databaseName" : "databaseName",
      "jdbcPrefix" : "jdbcPrefix",
      "versions" : [ "versions", "versions" ],
      "displayName" : "displayName"
    }, {
      "databaseName" : "databaseName",
      "jdbcPrefix" : "jdbcPrefix",
      "versions" : [ "versions", "versions" ],
      "displayName" : "displayName"
    } ],
    "displayName" : "displayName",
    "name" : "name"
  } ],
  "maxNumberOfClustersPerUser" : 0,
  "clusterTimeToLive" : 0,
  "allowedInstanceTypes" : [ "allowedInstanceTypes", "allowedInstanceTypes" ],
  "maxNumberOfClusters" : 0,
  "defaultTags" : {
    "key" : "defaultTags"
  },
  "platforms" : "platforms",
  "smartsenseEnabled" : false
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * validate account preferences of all stacks
 * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
 *
 * no response value expected for this operation
 **/
exports.validateAccountPreferencesEndpoint = function() {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}

