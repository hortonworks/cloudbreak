'use strict';

exports.deleteNetwork = function(args, res, next) {
  /**
   * delete network by id
   * Provider specific network settings could be configured by using Network resources.
   *
   * id Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePrivateNetwork = function(args, res, next) {
  /**
   * delete private network by name
   * Provider specific network settings could be configured by using Network resources.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicNetwork = function(args, res, next) {
  /**
   * delete public (owned) or private network by name
   * Provider specific network settings could be configured by using Network resources.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getNetwork = function(args, res, next) {
  /**
   * retrieve network by id
   * Provider specific network settings could be configured by using Network resources.
   *
   * id Long 
   * returns NetworkResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "subnetCIDR" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivateNetwork = function(args, res, next) {
  /**
   * retrieve a private network by name
   * Provider specific network settings could be configured by using Network resources.
   *
   * name String 
   * returns NetworkResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "subnetCIDR" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesNetwork = function(args, res, next) {
  /**
   * retrieve private networks
   * Provider specific network settings could be configured by using Network resources.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "subnetCIDR" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicNetwork = function(args, res, next) {
  /**
   * retrieve a public or private (owned) network by name
   * Provider specific network settings could be configured by using Network resources.
   *
   * name String 
   * returns NetworkResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "subnetCIDR" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsNetwork = function(args, res, next) {
  /**
   * retrieve public and private (owned) networks
   * Provider specific network settings could be configured by using Network resources.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/networks/networks.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateNetwork = function(args, res, next) {
  /**
   * create network as private resource
   * Provider specific network settings could be configured by using Network resources.
   *
   * body NetworkRequest  (optional)
   * returns NetworkResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "subnetCIDR" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicNetwork = function(args, res, next) {
  /**
   * create network as public resource
   * Provider specific network settings could be configured by using Network resources.
   *
   * body NetworkRequest  (optional)
   * returns NetworkResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "subnetCIDR" : "aeiou",
  "cloudPlatform" : "aeiou",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

