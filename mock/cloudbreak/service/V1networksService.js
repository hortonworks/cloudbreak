'use strict';


/**
 * delete network by id
 * Provider specific network settings could be configured by using Network resources.
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteNetwork = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete private network by name
 * Provider specific network settings could be configured by using Network resources.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateNetwork = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private network by name
 * Provider specific network settings could be configured by using Network resources.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicNetwork = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve network by id
 * Provider specific network settings could be configured by using Network resources.
 *
 * id Long 
 * returns NetworkResponse
 **/
exports.getNetwork = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "subnetCIDR" : "subnetCIDR",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "name",
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a private network by name
 * Provider specific network settings could be configured by using Network resources.
 *
 * name String 
 * returns NetworkResponse
 **/
exports.getPrivateNetwork = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "subnetCIDR" : "subnetCIDR",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "name",
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private networks
 * Provider specific network settings could be configured by using Network resources.
 *
 * returns List
 **/
exports.getPrivatesNetwork = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "subnetCIDR" : "subnetCIDR",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "name",
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
}, {
  "subnetCIDR" : "subnetCIDR",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "name",
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) network by name
 * Provider specific network settings could be configured by using Network resources.
 *
 * name String 
 * returns NetworkResponse
 **/
exports.getPublicNetwork = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "subnetCIDR" : "subnetCIDR",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "name",
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) networks
 * Provider specific network settings could be configured by using Network resources.
 *
 * returns List
 **/
exports.getPublicsNetwork = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/networks/networks.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create network as private resource
 * Provider specific network settings could be configured by using Network resources.
 *
 * body NetworkRequest  (optional)
 * returns NetworkResponse
 **/
exports.postPrivateNetwork = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "subnetCIDR" : "subnetCIDR",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "name",
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create network as public resource
 * Provider specific network settings could be configured by using Network resources.
 *
 * body NetworkRequest  (optional)
 * returns NetworkResponse
 **/
exports.postPublicNetwork = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "subnetCIDR" : "subnetCIDR",
  "cloudPlatform" : "cloudPlatform",
  "publicInAccount" : false,
  "topologyId" : 0,
  "name" : "name",
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

