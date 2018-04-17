'use strict';

var utils = require('../utils/writer.js');
var V1rdsconfigs = require('../service/V1rdsconfigsService');

module.exports.deletePrivateRds = function deletePrivateRds (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1rdsconfigs.deletePrivateRds(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicRds = function deletePublicRds (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1rdsconfigs.deletePublicRds(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deleteRds = function deleteRds (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1rdsconfigs.deleteRds(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateRds = function getPrivateRds (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1rdsconfigs.getPrivateRds(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesRds = function getPrivatesRds (req, res, next) {
  V1rdsconfigs.getPrivatesRds()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicRds = function getPublicRds (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1rdsconfigs.getPublicRds(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsRds = function getPublicsRds (req, res, next) {
  V1rdsconfigs.getPublicsRds()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getRds = function getRds (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1rdsconfigs.getRds(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateRds = function postPrivateRds (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1rdsconfigs.postPrivateRds(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicRds = function postPublicRds (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1rdsconfigs.postPublicRds(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.testRdsConnection = function testRdsConnection (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1rdsconfigs.testRdsConnection(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
