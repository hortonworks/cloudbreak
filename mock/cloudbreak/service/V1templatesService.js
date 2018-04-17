'use strict';


/**
 * delete private template by name
 * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateTemplate = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private template by name
 * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicTemplate = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete template by id
 * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteTemplate = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve a private template by name
 * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
 *
 * name String 
 * returns TemplateResponse
 **/
exports.getPrivateTemplate = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "volumeType" : "volumeType",
  "cloudPlatform" : "cloudPlatform",
  "public" : false,
  "instanceType" : "instanceType",
  "customInstanceType" : {
    "memory" : 5,
    "cpus" : 9
  },
  "topologyId" : 9,
  "name" : "name",
  "description" : "description",
  "volumeCount" : 8,
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 9
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private templates
 * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
 *
 * returns List
 **/
exports.getPrivatesTemplate = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "volumeType" : "volumeType",
  "cloudPlatform" : "cloudPlatform",
  "public" : false,
  "instanceType" : "instanceType",
  "customInstanceType" : {
    "memory" : 5,
    "cpus" : 9
  },
  "topologyId" : 9,
  "name" : "name",
  "description" : "description",
  "volumeCount" : 8,
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 9
}, {
  "volumeType" : "volumeType",
  "cloudPlatform" : "cloudPlatform",
  "public" : false,
  "instanceType" : "instanceType",
  "customInstanceType" : {
    "memory" : 5,
    "cpus" : 9
  },
  "topologyId" : 9,
  "name" : "name",
  "description" : "description",
  "volumeCount" : 8,
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 9
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) template by name
 * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
 *
 * name String 
 * returns TemplateResponse
 **/
exports.getPublicTemplate = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "volumeType" : "volumeType",
  "cloudPlatform" : "cloudPlatform",
  "public" : false,
  "instanceType" : "instanceType",
  "customInstanceType" : {
    "memory" : 5,
    "cpus" : 9
  },
  "topologyId" : 9,
  "name" : "name",
  "description" : "description",
  "volumeCount" : 8,
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 9
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) templates
 * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
 *
 * returns List
 **/
exports.getPublicsTemplate = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/templates/qa-templates.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve template by id
 * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
 *
 * id Long 
 * returns TemplateResponse
 **/
exports.getTemplate = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "volumeType" : "volumeType",
  "cloudPlatform" : "cloudPlatform",
  "public" : false,
  "instanceType" : "instanceType",
  "customInstanceType" : {
    "memory" : 5,
    "cpus" : 9
  },
  "topologyId" : 9,
  "name" : "name",
  "description" : "description",
  "volumeCount" : 8,
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 9
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

