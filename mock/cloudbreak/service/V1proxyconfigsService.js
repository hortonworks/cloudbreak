'use strict';


/**
 * delete private proxy configuration by name
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateProxyConfig = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete proxy configuration by id
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteProxyConfig = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private proxy configuration by name
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicProxyConfig = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve a private proxy configuration by name
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * name String 
 * returns ProxyConfigResponse
 **/
exports.getPrivateProxyConfig = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/proxyconfig/default-proxy.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private proxy configurations
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * returns List
 **/
exports.getPrivatesProxyConfig = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/proxyconfig/default-proxy.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve proxy configuration by id
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * id Long 
 * returns ProxyConfigResponse
 **/
exports.getProxyConfig = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/proxyconfig/default-proxy.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) proxy configuration by name
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * name String 
 * returns ProxyConfigResponse
 **/
exports.getPublicProxyConfig = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/proxyconfig/default-proxy.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) proxy configurations
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * returns List
 **/
exports.getPublicsProxyConfig = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/proxyconfig/default-proxy.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create proxy configuration as private resource
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * body ProxyConfigRequest  (optional)
 * returns ProxyConfigResponse
 **/
exports.postPrivateProxyConfig = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/proxyconfig/default-proxy.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create proxy configuration as public resource
 * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
 *
 * body ProxyConfigRequest  (optional)
 * returns ProxyConfigResponse
 **/
exports.postPublicProxyConfig = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/proxyconfig/default-proxy.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

