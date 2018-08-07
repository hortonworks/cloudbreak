'use strict';

var utils = require('../utils/writer.js');
var V1ldap = require('../service/V1ldapService');

module.exports.deleteLdap = function deleteLdap (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1ldap.deleteLdap(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateLdap = function deletePrivateLdap (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1ldap.deletePrivateLdap(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicLdap = function deletePublicLdap (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1ldap.deletePublicLdap(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getLdap = function getLdap (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1ldap.getLdap(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateLdap = function getPrivateLdap (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1ldap.getPrivateLdap(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesLdap = function getPrivatesLdap (req, res, next) {
  V1ldap.getPrivatesLdap()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicLdap = function getPublicLdap (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1ldap.getPublicLdap(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsLdap = function getPublicsLdap (req, res, next) {
  V1ldap.getPublicsLdap()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postLdapConnectionTest = function postLdapConnectionTest (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1ldap.postLdapConnectionTest(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateLdap = function postPrivateLdap (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1ldap.postPrivateLdap(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicLdap = function postPublicLdap (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1ldap.postPublicLdap(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
