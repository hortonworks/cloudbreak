'use strict';

exports.deletePrivateProxyConfig = function(args, res, next) {
  /**
   * delete private proxy configuration by name
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * name String
   * no response value expected for this operation
   **/
  res.end();
}

exports.deleteProxyConfig = function(args, res, next) {
  /**
   * delete proxy configuration by id
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * id Long
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicProxyConfig = function(args, res, next) {
  /**
   * delete public (owned) or private proxy configuration by name
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * name String
   * no response value expected for this operation
   **/
  res.end();
}

exports.getPrivateProxyConfig = function(args, res, next) {
  /**
   * retrieve a private proxy configuration by name
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * name String
   * returns ProxyConfigResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "protocol" : "aeiou",
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 123456789,
  "serverPort" : 123,
  "userName" : "aeiou",
  "serverHost" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesProxyConfig = function(args, res, next) {
  /**
   * retrieve private proxy configurations
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "protocol" : "aeiou",
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 123456789,
  "serverPort" : 123,
  "userName" : "aeiou",
  "serverHost" : "aeiou"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getProxyConfig = function(args, res, next) {
  /**
   * retrieve proxy configuration by id
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * id Long
   * returns ProxyConfigResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "protocol" : "aeiou",
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 123456789,
  "serverPort" : 123,
  "userName" : "aeiou",
  "serverHost" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicProxyConfig = function(args, res, next) {
  /**
   * retrieve a public or private (owned) proxy configuration by name
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * name String
   * returns ProxyConfigResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "protocol" : "aeiou",
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 123456789,
  "serverPort" : 123,
  "userName" : "aeiou",
  "serverHost" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsProxyConfig = function(args, res, next) {
  /**
   * retrieve public and private (owned) proxy configurations
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "protocol" : "aeiou",
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 123456789,
  "serverPort" : 123,
  "userName" : "aeiou",
  "serverHost" : "aeiou"
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateProxyConfig = function(args, res, next) {
  /**
   * create proxy configuration as private resource
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * body ProxyConfigRequest  (optional)
   * returns ProxyConfigResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "protocol" : "aeiou",
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 123456789,
  "serverPort" : 123,
  "userName" : "aeiou",
  "serverHost" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicProxyConfig = function(args, res, next) {
  /**
   * create proxy configuration as public resource
   * An proxy Configuration describe a connection to an external proxy server which provides internet access cluster members. It's applied for package manager and Ambari too
   *
   * body ProxyConfigRequest  (optional)
   * returns ProxyConfigResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "protocol" : "aeiou",
  "name" : "aeiou",
  "description" : "aeiou",
  "id" : 123456789,
  "serverPort" : 123,
  "userName" : "aeiou",
  "serverHost" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}
