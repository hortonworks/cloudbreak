'use strict';

exports.deleteBlueprint = function(args, res, next) {
  /**
   * delete blueprint by id
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * id Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePrivateBlueprint = function(args, res, next) {
  /**
   * delete private blueprint by name
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicBlueprint = function(args, res, next) {
  /**
   * delete public (owned) or private blueprint by name
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getBlueprint = function(args, res, next) {
  /**
   * retrieve blueprint by id
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * id Long 
   * returns BlueprintResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/blueprints/default-blueprint.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getBlueprintRequestFromId = function(args, res, next) {
  /**
   * retrieve validation request by blueprint name
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * id Long 
   * returns BlueprintRequest
   **/
  var examples = {};
  examples['application/json'] = require('../responses/blueprints/default-blueprint.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivateBlueprint = function(args, res, next) {
  /**
   * retrieve a private blueprint by name
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * name String 
   * returns BlueprintResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/blueprints/default-blueprint.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesBlueprint = function(args, res, next) {
  /**
   * retrieve private blueprints
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/blueprints/qa-blueprints.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicBlueprint = function(args, res, next) {
  /**
   * retrieve a public or private (owned) blueprint by name
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * name String 
   * returns BlueprintResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/blueprints/default-blueprint.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsBlueprint = function(args, res, next) {
  /**
   * retrieve public and private (owned) blueprints
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/blueprints/qa-blueprints.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateBlueprint = function(args, res, next) {
  /**
   * create blueprint as private resource
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * body BlueprintRequest  (optional)
   * returns BlueprintResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "public" : false,
  "ambariBlueprint" : "aeiou",
  "inputs" : [ {
    "referenceConfiguration" : "aeiou",
    "name" : "aeiou",
    "description" : "aeiou"
  } ],
  "hostGroupCount" : 6,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "tags" : {
    "key" : "{}"
  },
  "status" : "DEFAULT"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicBlueprint = function(args, res, next) {
  /**
   * create blueprint as public resource
   * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
   *
   * body BlueprintRequest  (optional)
   * returns BlueprintResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "public" : false,
  "ambariBlueprint" : "aeiou",
  "inputs" : [ {
    "referenceConfiguration" : "aeiou",
    "name" : "aeiou",
    "description" : "aeiou"
  } ],
  "hostGroupCount" : 6,
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0,
  "tags" : {
    "key" : "{}"
  },
  "status" : "DEFAULT"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

