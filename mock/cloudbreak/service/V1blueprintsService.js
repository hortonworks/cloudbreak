'use strict';


/**
 * delete blueprint by id
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteBlueprint = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete private blueprint by name
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateBlueprint = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private blueprint by name
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicBlueprint = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve blueprint by id
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * id Long 
 * returns BlueprintResponse
 **/
exports.getBlueprint = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/blueprints/default-blueprint.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve validation request by blueprint name
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * id Long 
 * returns BlueprintRequest
 **/
exports.getBlueprintRequestFromId = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/blueprints/default-blueprint.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a private blueprint by name
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * name String 
 * returns BlueprintResponse
 **/
exports.getPrivateBlueprint = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/blueprints/default-blueprint.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private blueprints
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * returns List
 **/
exports.getPrivatesBlueprint = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/blueprints/qa-blueprints.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) blueprint by name
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * name String 
 * returns BlueprintResponse
 **/
exports.getPublicBlueprint = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/blueprints/default-blueprint.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) blueprints
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * returns List
 **/
exports.getPublicsBlueprint = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/blueprints/qa-blueprints.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create blueprint as private resource
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * body BlueprintRequest  (optional)
 * returns BlueprintResponse
 **/
exports.postPrivateBlueprint = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "public" : false,
  "ambariBlueprint" : "e30=",
  "inputs" : [ {
    "referenceConfiguration" : "referenceConfiguration",
    "name" : "name",
    "description" : "description"
  }, {
    "referenceConfiguration" : "referenceConfiguration",
    "name" : "name",
    "description" : "description"
  } ],
  "hostGroupCount" : 6,
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "tags" : {
    "key" : "{}"
  },
  "status" : "DEFAULT"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create blueprint as public resource
 * Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.
 *
 * body BlueprintRequest  (optional)
 * returns BlueprintResponse
 **/
exports.postPublicBlueprint = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "public" : false,
  "ambariBlueprint" : "e30=",
  "inputs" : [ {
    "referenceConfiguration" : "referenceConfiguration",
    "name" : "name",
    "description" : "description"
  }, {
    "referenceConfiguration" : "referenceConfiguration",
    "name" : "name",
    "description" : "description"
  } ],
  "hostGroupCount" : 6,
  "name" : "name",
  "description" : "description",
  "id" : 0,
  "tags" : {
    "key" : "{}"
  },
  "status" : "DEFAULT"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

