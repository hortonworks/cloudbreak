'use strict';


/**
 * delete private RDS configuration by name
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateRds = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private RDS configuration by name
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicRds = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete RDS configuration by id
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteRds = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve a private RDS configuration by name
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * name String 
 * returns RDSConfigResponse
 **/
exports.getPrivateRds = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/rdsconfig/default-rds.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private RDS configurations
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * returns List
 **/
exports.getPrivatesRds = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/rdsconfig/default-rds.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) RDS configuration by name
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * name String 
 * returns RDSConfigResponse
 **/
exports.getPublicRds = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/rdsconfig/default-rds.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) RDS configurations
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * returns List
 **/
exports.getPublicsRds = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/rdsconfig/default-rds.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve RDS configuration by id
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * id Long 
 * returns RDSConfigResponse
 **/
exports.getRds = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/rdsconfig/default-rds.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create RDS configuration as private resource
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * body RdsConfig  (optional)
 * returns RDSConfigResponse
 **/
exports.postPrivateRds = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/rdsconfig/default-rds.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create RDS configuration as public resource
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * body RdsConfig  (optional)
 * returns RDSConfigResponse
 **/
exports.postPublicRds = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/rdsconfig/default-rds.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * test RDS connectivity
 * An RDS Configuration describe a connection to an external Relational Database Service that can be used as the Hive Metastore.
 *
 * body RdsTestRequest  (optional)
 * returns RdsTestResult
 **/
exports.testRdsConnection = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
        "connectionResult":"Failed to connect to RDS: The connection attempt failed."
    };
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

