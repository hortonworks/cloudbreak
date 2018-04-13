'use strict';

exports.deletePrivateTemplate = function(args, res, next) {
  /**
   * delete private template by name
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicTemplate = function(args, res, next) {
  /**
   * delete public (owned) or private template by name
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deleteTemplate = function(args, res, next) {
  /**
   * delete template by id
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * id Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getPrivateTemplate = function(args, res, next) {
  /**
   * retrieve a private template by name
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * name String 
   * returns TemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesTemplate = function(args, res, next) {
  /**
   * retrieve private templates
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicTemplate = function(args, res, next) {
  /**
   * retrieve a public or private (owned) template by name
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * name String 
   * returns TemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsTemplate = function(args, res, next) {
  /**
   * retrieve public and private (owned) templates
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/templates/qa-templates.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getTemplate = function(args, res, next) {
  /**
   * retrieve template by id
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * id Long 
   * returns TemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

