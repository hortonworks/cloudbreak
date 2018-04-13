'use strict';

exports.getAccountPreferencesEndpoint = function(args, res, next) {
  /**
   * retrieve account preferences for admin user
   * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
   *
   * returns AccountPreferencesResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "maxNumberOfNodesPerCluster" : 0,
  "userTimeToLive" : 0,
  "supportedExternalDatabases" : [ {
    "databases" : [ {
      "databaseName" : "aeiou",
      "jdbcPrefix" : "aeiou",
      "versions" : [ "aeiou" ],
      "displayName" : "aeiou"
    } ],
    "displayName" : "aeiou",
    "name" : "aeiou"
  } ],
  "maxNumberOfClustersPerUser" : 0,
  "clusterTimeToLive" : 0,
  "allowedInstanceTypes" : [ "aeiou" ],
  "maxNumberOfClusters" : 0,
  "defaultTags" : {
    "key" : "aeiou"
  },
  "platforms" : "aeiou",
  "smartsenseEnabled" : false
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.isPlatformSelectionDisabled = function(args, res, next) {
  /**
   * is platform selection disabled
   * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
   *
   * returns Map
   **/
  var examples = {};
  examples['application/json'] = require('../responses/accountpreferences/platforms.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.platformEnablement = function(args, res, next) {
  /**
   * is platform selection enabled
   * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
   *
   * returns Map
   **/
  var examples = {};
  examples['application/json'] = require('../responses/accountpreferences/platforms.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postAccountPreferencesEndpoint = function(args, res, next) {
  /**
   * post account preferences of admin user
   * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
   *
   * body AccountPreferencesRequest  (optional)
   * returns AccountPreferencesResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "maxNumberOfNodesPerCluster" : 0,
  "userTimeToLive" : 0,
  "supportedExternalDatabases" : [ {
    "databases" : [ {
      "databaseName" : "aeiou",
      "jdbcPrefix" : "aeiou",
      "versions" : [ "aeiou" ],
      "displayName" : "aeiou"
    } ],
    "displayName" : "aeiou",
    "name" : "aeiou"
  } ],
  "maxNumberOfClustersPerUser" : 0,
  "clusterTimeToLive" : 0,
  "allowedInstanceTypes" : [ "aeiou" ],
  "maxNumberOfClusters" : 0,
  "defaultTags" : {
    "key" : "aeiou"
  },
  "platforms" : "aeiou",
  "smartsenseEnabled" : false
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.putAccountPreferencesEndpoint = function(args, res, next) {
  /**
   * update account preferences of admin user
   * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
   *
   * body AccountPreferencesRequest  (optional)
   * returns AccountPreferencesResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "maxNumberOfNodesPerCluster" : 0,
  "userTimeToLive" : 0,
  "supportedExternalDatabases" : [ {
    "databases" : [ {
      "databaseName" : "aeiou",
      "jdbcPrefix" : "aeiou",
      "versions" : [ "aeiou" ],
      "displayName" : "aeiou"
    } ],
    "displayName" : "aeiou",
    "name" : "aeiou"
  } ],
  "maxNumberOfClustersPerUser" : 0,
  "clusterTimeToLive" : 0,
  "allowedInstanceTypes" : [ "aeiou" ],
  "maxNumberOfClusters" : 0,
  "defaultTags" : {
    "key" : "aeiou"
  },
  "platforms" : "aeiou",
  "smartsenseEnabled" : false
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.validateAccountPreferencesEndpoint = function(args, res, next) {
  /**
   * validate account preferences of all stacks
   * Account related preferences that could be managed by the account admins and different restrictions could be added to Cloudbreak resources.
   *
   * no response value expected for this operation
   **/
  res.end();
}

