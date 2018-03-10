'use strict';

exports.checkClientVersion = function(args, res, next) {
  /**
   * checks the client version
   * 
   *
   * version String 
   * returns VersionCheckResult
   **/
  var examples = {};
  examples['application/json'] = {
  "versionCheckOk" : true,
  "message" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.createRDSDatabaseUtil = function(args, res, next) {
  /**
   * create a database connection parameters
   * 
   *
   * body RDSBuildRequest  (optional)
   * target List  (optional)
   * returns RdsBuildResult
   **/
  var examples = {};
  examples['application/json'] = {
  "results" : {
    "key" : "aeiou"
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.testAmbariDatabaseUtil = function(args, res, next) {
  /**
   * tests a database connection parameters
   * 
   *
   * body AmbariDatabaseDetails  (optional)
   * returns AmbariDatabaseTestResult
   **/
  var examples = {};
  examples['application/json'] = {
  "error" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.testLdapConnectionByIdUtil = function(args, res, next) {
  /**
   * tests an already exists LDAP connection
   * 
   *
   * id Long 
   * returns RdsTestResult
   **/
  var examples = {};
  examples['application/json'] = {
  "connectionResult" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.testLdapConnectionUtil = function(args, res, next) {
  /**
   * tests an LDAP connection
   * 
   *
   * body LdapValidationRequest  (optional)
   * returns RdsTestResult
   **/
  var examples = {};
  examples['application/json'] = {
  "connectionResult" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.testRdsConnectionByIdUtil = function(args, res, next) {
  /**
   * tests an already exists RDS connection
   * 
   *
   * id Long 
   * returns RdsTestResult
   **/
  var examples = {};
  examples['application/json'] = {
  "connectionResult" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.testRdsConnectionUtil = function(args, res, next) {
  /**
   * tests an RDS connection
   * 
   *
   * body RDSConfig  (optional)
   * returns RdsTestResult
   **/
  var examples = {};
  examples['application/json'] = {
  "connectionResult" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

