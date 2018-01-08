'use strict';

exports.deleteTopology = function(args, res, next) {
  /**
   * delete topology by id
   * A topology gives system administrators an easy way to associate compute nodes with data centers and racks.
   *
   * id Long 
   * forced Boolean  (optional)
   * no response value expected for this operation
   **/
  res.end();
}

exports.getPublicsTopology = function(args, res, next) {
  /**
   * retrieve topoligies
   * A topology gives system administrators an easy way to associate compute nodes with data centers and racks.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "cloudPlatform" : "aeiou",
  "nodes" : {
    "key" : "aeiou"
  },
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getTopology = function(args, res, next) {
  /**
   * retrieve topology by id
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * id Long 
   * returns TopologyResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cloudPlatform" : "aeiou",
  "nodes" : {
    "key" : "aeiou"
  },
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicTopology = function(args, res, next) {
  /**
   * create topology as public resource
   * A topology gives system administrators an easy way to associate compute nodes with data centers and racks.
   *
   * body TopologyRequest  (optional)
   * returns TopologyResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "cloudPlatform" : "aeiou",
  "nodes" : {
    "key" : "aeiou"
  },
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 0
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

