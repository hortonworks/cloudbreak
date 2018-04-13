'use strict';

exports.deletePrivateSecurityGroup = function(args, res, next) {
  /**
   * delete private security group by name
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicSecurityGroup = function(args, res, next) {
  /**
   * delete public (owned) or private security group by name
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deleteSecurityGroup = function(args, res, next) {
  /**
   * delete security group by id
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * id Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getPrivateSecurityGroup = function(args, res, next) {
  /**
   * retrieve a private security group by name
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * name String 
   * returns SecurityGroupResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "securityGroupId" : "aeiou",
  "owner" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "aeiou",
    "protocol" : "aeiou",
    "id" : 6,
    "ports" : "aeiou",
    "modifiable" : false
  } ],
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "account" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesSecurityGroup = function(args, res, next) {
  /**
   * retrieve private security groups
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "securityGroupId" : "aeiou",
  "owner" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "aeiou",
    "protocol" : "aeiou",
    "id" : 6,
    "ports" : "aeiou",
    "modifiable" : false
  } ],
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "account" : "aeiou"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicSecurityGroup = function(args, res, next) {
  /**
   * retrieve a public or private (owned) security group by name
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * name String 
   * returns SecurityGroupResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "securityGroupId" : "aeiou",
  "owner" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "aeiou",
    "protocol" : "aeiou",
    "id" : 6,
    "ports" : "aeiou",
    "modifiable" : false
  } ],
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "account" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsSecurityGroup = function(args, res, next) {
  /**
   * retrieve public and private (owned) security groups
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/securitygroups/securitygroups.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getSecurityGroup = function(args, res, next) {
  /**
   * retrieve security group by id
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * id Long 
   * returns SecurityGroupResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "securityGroupId" : "aeiou",
  "owner" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "aeiou",
    "protocol" : "aeiou",
    "id" : 6,
    "ports" : "aeiou",
    "modifiable" : false
  } ],
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "account" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateSecurityGroup = function(args, res, next) {
  /**
   * create security group as private resource
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * body SecurityGroupRequest  (optional)
   * returns SecurityGroupResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "securityGroupId" : "aeiou",
  "owner" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "aeiou",
    "protocol" : "aeiou",
    "id" : 6,
    "ports" : "aeiou",
    "modifiable" : false
  } ],
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "account" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicSecurityGroup = function(args, res, next) {
  /**
   * create security group as public resource
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * body SecurityGroupRequest  (optional)
   * returns SecurityGroupResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "securityGroupId" : "aeiou",
  "owner" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "aeiou",
    "protocol" : "aeiou",
    "id" : 6,
    "ports" : "aeiou",
    "modifiable" : false
  } ],
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "account" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

