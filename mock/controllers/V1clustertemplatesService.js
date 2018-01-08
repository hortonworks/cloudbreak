'use strict';

exports.deleteClusterTemplate = function(args, res, next) {
  /**
   * delete cluster template by id
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * id Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePrivateClusterTemplate = function(args, res, next) {
  /**
   * delete private cluster template by name
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicClusterTemplate = function(args, res, next) {
  /**
   * delete public (owned) or private cluster template by name
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getClusterTemplate = function(args, res, next) {
  /**
   * retrieve cluster template by id
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * id Long 
   * returns ClusterTemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "template" : "aeiou",
  "name" : "aeiou",
  "id" : 0,
  "type" : "QUICK_START"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivateClusterTemplate = function(args, res, next) {
  /**
   * retrieve a private cluster template by name
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * name String 
   * returns ClusterTemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "template" : "aeiou",
  "name" : "aeiou",
  "id" : 0,
  "type" : "QUICK_START"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesClusterTemplate = function(args, res, next) {
  /**
   * retrieve private cluster templates
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "template" : "aeiou",
  "name" : "aeiou",
  "id" : 0,
  "type" : "QUICK_START"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicClusterTemplate = function(args, res, next) {
  /**
   * retrieve a public or private (owned) cluster template by name
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * name String 
   * returns ClusterTemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "template" : "aeiou",
  "name" : "aeiou",
  "id" : 0,
  "type" : "QUICK_START"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsClusterTemplate = function(args, res, next) {
  /**
   * retrieve public and private (owned) cluster template
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "template" : "aeiou",
  "name" : "aeiou",
  "id" : 0,
  "type" : "QUICK_START"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateClusterTemplate = function(args, res, next) {
  /**
   * create cluster template as private resource
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * body ClusterTemplateRequest  (optional)
   * returns ClusterTemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "template" : "aeiou",
  "name" : "aeiou",
  "id" : 0,
  "type" : "QUICK_START"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicClusterTemplate = function(args, res, next) {
  /**
   * create cluster template as public resource
   * Cluster templates are stored cluster configurations, which configurations are reusable any time
   *
   * body ClusterTemplateRequest  (optional)
   * returns ClusterTemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "template" : "aeiou",
  "name" : "aeiou",
  "id" : 0,
  "type" : "QUICK_START"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

