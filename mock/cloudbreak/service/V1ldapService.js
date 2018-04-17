'use strict';


/**
 * delete LDAP config by id
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteLdap = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete private LDAP config by name
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateLdap = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private LDAP config by name
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicLdap = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve LDAP config by id
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * id Long 
 * returns LdapConfigResponse
 **/
exports.getLdap = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a private LDAP config by name
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * name String 
 * returns LdapConfigResponse
 **/
exports.getPrivateLdap = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private LDAP configs
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * returns List
 **/
exports.getPrivatesLdap = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) LDAP config by name
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * name String 
 * returns LdapConfigResponse
 **/
exports.getPublicLdap = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) LDAP configs
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * returns List
 **/
exports.getPublicsLdap = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * test that the connection could be established of an existing or new LDAP config
 * 
 *
 * body LDAPTestRequest  (optional)
 * returns LdapTestResult
 **/
exports.postLdapConnectionTest = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
        "connectionResult":"Failed to connect to LDAP server: hwxad-1a2bcd3e45678f90.elb.eu-west-1.amazonaws.com:123"
    };
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create LDAP config as private resource
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * body LdapConfigRequest  (optional)
 * returns LdapConfigResponse
 **/
exports.postPrivateLdap = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create LDAP config as public resource
 * LDAP server integration enables the user to provide a central place to store usernames and passwords for the users of his/her clusters.
 *
 * body LdapConfigRequest  (optional)
 * returns LdapConfigResponse
 **/
exports.postPublicLdap = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/ldapconfig/default-ldap.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

