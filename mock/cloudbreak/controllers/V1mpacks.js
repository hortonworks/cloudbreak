'use strict';

var utils = require('../utils/writer.js');
var V1mpacks = require('../service/V1mpacksService');

module.exports.deleteManagementPack = function deleteManagementPack (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1mpacks.deleteManagementPack(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateManagementPack = function deletePrivateManagementPack (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1mpacks.deletePrivateManagementPack(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicManagementPack = function deletePublicManagementPack (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1mpacks.deletePublicManagementPack(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getManagementPack = function getManagementPack (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1mpacks.getManagementPack(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateManagementPack = function getPrivateManagementPack (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1mpacks.getPrivateManagementPack(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateManagementPacks = function getPrivateManagementPacks (req, res, next) {
  V1mpacks.getPrivateManagementPacks()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicManagementPack = function getPublicManagementPack (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1mpacks.getPublicManagementPack(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicManagementPacks = function getPublicManagementPacks (req, res, next) {
  V1mpacks.getPublicManagementPacks()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateManagementPack = function postPrivateManagementPack (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1mpacks.postPrivateManagementPack(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicManagementPack = function postPublicManagementPack (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1mpacks.postPublicManagementPack(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
