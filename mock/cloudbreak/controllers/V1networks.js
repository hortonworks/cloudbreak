'use strict';

var utils = require('../utils/writer.js');
var V1networks = require('../service/V1networksService');

module.exports.deleteNetwork = function deleteNetwork (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1networks.deleteNetwork(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateNetwork = function deletePrivateNetwork (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1networks.deletePrivateNetwork(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicNetwork = function deletePublicNetwork (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1networks.deletePublicNetwork(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getNetwork = function getNetwork (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1networks.getNetwork(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateNetwork = function getPrivateNetwork (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1networks.getPrivateNetwork(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesNetwork = function getPrivatesNetwork (req, res, next) {
  V1networks.getPrivatesNetwork()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicNetwork = function getPublicNetwork (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1networks.getPublicNetwork(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsNetwork = function getPublicsNetwork (req, res, next) {
  V1networks.getPublicsNetwork()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateNetwork = function postPrivateNetwork (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1networks.postPrivateNetwork(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicNetwork = function postPublicNetwork (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1networks.postPublicNetwork(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
