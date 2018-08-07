'use strict';


/**
 * delete cluster template by id
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteClusterTemplate = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete private cluster template by name
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateClusterTemplate = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private cluster template by name
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicClusterTemplate = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve cluster template by id
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * id Long 
 * returns ClusterTemplateResponse
 **/
exports.getClusterTemplate = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "template" : "template",
  "name" : "name",
  "id" : 0,
  "type" : "QUICK_START"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a private cluster template by name
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * name String 
 * returns ClusterTemplateResponse
 **/
exports.getPrivateClusterTemplate = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "template" : "template",
  "name" : "name",
  "id" : 0,
  "type" : "QUICK_START"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private cluster templates
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * returns List
 **/
exports.getPrivatesClusterTemplate = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "template" : "template",
  "name" : "name",
  "id" : 0,
  "type" : "QUICK_START"
}, {
  "template" : "template",
  "name" : "name",
  "id" : 0,
  "type" : "QUICK_START"
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) cluster template by name
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * name String 
 * returns ClusterTemplateResponse
 **/
exports.getPublicClusterTemplate = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "template" : "template",
  "name" : "name",
  "id" : 0,
  "type" : "QUICK_START"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) cluster template
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * returns List
 **/
exports.getPublicsClusterTemplate = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "template" : "template",
  "name" : "name",
  "id" : 0,
  "type" : "QUICK_START"
}, {
  "template" : "template",
  "name" : "name",
  "id" : 0,
  "type" : "QUICK_START"
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create cluster template as private resource
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * body ClusterTemplateRequest  (optional)
 * returns ClusterTemplateResponse
 **/
exports.postPrivateClusterTemplate = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "template" : "template",
  "name" : "name",
  "id" : 0,
  "type" : "QUICK_START"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create cluster template as public resource
 * Cluster templates are stored cluster configurations, which configurations are reusable any time
 *
 * body ClusterTemplateRequest  (optional)
 * returns ClusterTemplateResponse
 **/
exports.postPublicClusterTemplate = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "template" : "template",
  "name" : "name",
  "id" : 0,
  "type" : "QUICK_START"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

