'use strict';


/**
 * delete private security group by name
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateSecurityGroup = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private security group by name
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicSecurityGroup = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete security group by id
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteSecurityGroup = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve a private security group by name
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * name String 
 * returns SecurityGroupResponse
 **/
exports.getPrivateSecurityGroup = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "securityGroupId" : "securityGroupId",
  "owner" : "owner",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  }, {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  } ],
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "account" : "account"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private security groups
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * returns List
 **/
exports.getPrivatesSecurityGroup = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "securityGroupId" : "securityGroupId",
  "owner" : "owner",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  }, {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  } ],
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "account" : "account"
}, {
  "securityGroupId" : "securityGroupId",
  "owner" : "owner",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  }, {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  } ],
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "account" : "account"
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) security group by name
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * name String 
 * returns SecurityGroupResponse
 **/
exports.getPublicSecurityGroup = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "securityGroupId" : "securityGroupId",
  "owner" : "owner",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  }, {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  } ],
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "account" : "account"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) security groups
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * returns List
 **/
exports.getPublicsSecurityGroup = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/securitygroups/securitygroups.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve security group by id
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * id Long 
 * returns SecurityGroupResponse
 **/
exports.getSecurityGroup = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "securityGroupId" : "securityGroupId",
  "owner" : "owner",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  }, {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  } ],
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "account" : "account"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create security group as private resource
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * body SecurityGroupRequest  (optional)
 * returns SecurityGroupResponse
 **/
exports.postPrivateSecurityGroup = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "securityGroupId" : "securityGroupId",
  "owner" : "owner",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  }, {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  } ],
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "account" : "account"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create security group as public resource
 * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
 *
 * body SecurityGroupRequest  (optional)
 * returns SecurityGroupResponse
 **/
exports.postPublicSecurityGroup = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "securityGroupId" : "securityGroupId",
  "owner" : "owner",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "securityRules" : [ {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  }, {
    "subnet" : "subnet",
    "protocol" : "protocol",
    "id" : 6,
    "ports" : "ports",
    "modifiable" : false
  } ],
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "account" : "account"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

