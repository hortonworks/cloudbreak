'use strict';

var utils = require('../utils/writer.js');
var V1proxyconfigs = require('../service/V1proxyconfigsService');

module.exports.deletePrivateProxyConfig = function deletePrivateProxyConfig (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1proxyconfigs.deletePrivateProxyConfig(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deleteProxyConfig = function deleteProxyConfig (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1proxyconfigs.deleteProxyConfig(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicProxyConfig = function deletePublicProxyConfig (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1proxyconfigs.deletePublicProxyConfig(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateProxyConfig = function getPrivateProxyConfig (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1proxyconfigs.getPrivateProxyConfig(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesProxyConfig = function getPrivatesProxyConfig (req, res, next) {
  V1proxyconfigs.getPrivatesProxyConfig()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getProxyConfig = function getProxyConfig (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1proxyconfigs.getProxyConfig(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicProxyConfig = function getPublicProxyConfig (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1proxyconfigs.getPublicProxyConfig(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsProxyConfig = function getPublicsProxyConfig (req, res, next) {
  V1proxyconfigs.getPublicsProxyConfig()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateProxyConfig = function postPrivateProxyConfig (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1proxyconfigs.postPrivateProxyConfig(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicProxyConfig = function postPublicProxyConfig (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1proxyconfigs.postPublicProxyConfig(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
