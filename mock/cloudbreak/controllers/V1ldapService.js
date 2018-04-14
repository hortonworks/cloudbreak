'use strict';

exports.deleteLdap = function(args, res, next) {
  /**
   * delete LDAP config by id
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * id Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePrivateLdap = function(args, res, next) {
  /**
   * delete private LDAP config by name
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicLdap = function(args, res, next) {
  /**
   * delete public (owned) or private LDAP config by name
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getLdap = function(args, res, next) {
  /**
   * retrieve LDAP config by id
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * id Long 
   * returns LdapConfigResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivateLdap = function(args, res, next) {
  /**
   * retrieve a private LDAP config by name
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * name String 
   * returns LdapConfigResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesLdap = function(args, res, next) {
  /**
   * retrieve private LDAP configs
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicLdap = function(args, res, next) {
  /**
   * retrieve a public or private (owned) LDAP config by name
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * name String 
   * returns LdapConfigResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsLdap = function(args, res, next) {
  /**
   * retrieve public and private (owned) LDAP configs
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postLdapConnectionTest = function(args, res, next) {
  /**
   * test that the connection could be established of an existing or new LDAP config
   * 
   *
   * body LDAPTestRequest  (optional)
   * returns LdapTestResult
   **/
  var examples = {};
  examples['application/json'] = {
    "connectionResult":"Failed to connect to LDAP server: hwxad-1a2bcd3e45678f90.elb.eu-west-1.amazonaws.com:123"
  };
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateLdap = function(args, res, next) {
  /**
   * create LDAP config as private resource
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * body LdapConfigRequest  (optional)
   * returns LdapConfigResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicLdap = function(args, res, next) {
  /**
   * create LDAP config as public resource
   * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
   *
   * body LdapConfigRequest  (optional)
   * returns LdapConfigResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

