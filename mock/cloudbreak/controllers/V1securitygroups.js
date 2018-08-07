'use strict';

var utils = require('../utils/writer.js');
var V1securitygroups = require('../service/V1securitygroupsService');

module.exports.deletePrivateSecurityGroup = function deletePrivateSecurityGroup (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1securitygroups.deletePrivateSecurityGroup(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicSecurityGroup = function deletePublicSecurityGroup (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1securitygroups.deletePublicSecurityGroup(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deleteSecurityGroup = function deleteSecurityGroup (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1securitygroups.deleteSecurityGroup(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateSecurityGroup = function getPrivateSecurityGroup (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1securitygroups.getPrivateSecurityGroup(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesSecurityGroup = function getPrivatesSecurityGroup (req, res, next) {
  V1securitygroups.getPrivatesSecurityGroup()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicSecurityGroup = function getPublicSecurityGroup (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1securitygroups.getPublicSecurityGroup(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsSecurityGroup = function getPublicsSecurityGroup (req, res, next) {
  V1securitygroups.getPublicsSecurityGroup()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getSecurityGroup = function getSecurityGroup (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1securitygroups.getSecurityGroup(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateSecurityGroup = function postPrivateSecurityGroup (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1securitygroups.postPrivateSecurityGroup(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicSecurityGroup = function postPublicSecurityGroup (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1securitygroups.postPublicSecurityGroup(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
