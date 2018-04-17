'use strict';


/**
 * delete topology by id
 * A topology gives system administrators an easy way to associate compute nodes with data centers and racks.
 *
 * id Long 
 * forced Boolean  (optional)
 * no response value expected for this operation
 **/
exports.deleteTopology = function(id,forced) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve topoligies
 * A topology gives system administrators an easy way to associate compute nodes with data centers and racks.
 *
 * returns List
 **/
exports.getPublicsTopology = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "cloudPlatform" : "cloudPlatform",
  "nodes" : {
    "key" : "nodes"
  },
  "name" : "name",
  "description" : "description",
  "id" : 0
}, {
  "cloudPlatform" : "cloudPlatform",
  "nodes" : {
    "key" : "nodes"
  },
  "name" : "name",
  "description" : "description",
  "id" : 0
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve topology by id
 * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
 *
 * id Long 
 * returns TopologyResponse
 **/
exports.getTopology = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cloudPlatform" : "cloudPlatform",
  "nodes" : {
    "key" : "nodes"
  },
  "name" : "name",
  "description" : "description",
  "id" : 0
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create topology as public resource
 * A topology gives system administrators an easy way to associate compute nodes with data centers and racks.
 *
 * body TopologyRequest  (optional)
 * returns TopologyResponse
 **/
exports.postPublicTopology = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cloudPlatform" : "cloudPlatform",
  "nodes" : {
    "key" : "nodes"
  },
  "name" : "name",
  "description" : "description",
  "id" : 0
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

