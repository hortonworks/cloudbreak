'use strict';

var utils = require('../utils/writer.js');
var V1clustertemplates = require('../service/V1clustertemplatesService');

module.exports.deleteClusterTemplate = function deleteClusterTemplate (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1clustertemplates.deleteClusterTemplate(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateClusterTemplate = function deletePrivateClusterTemplate (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1clustertemplates.deletePrivateClusterTemplate(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicClusterTemplate = function deletePublicClusterTemplate (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1clustertemplates.deletePublicClusterTemplate(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getClusterTemplate = function getClusterTemplate (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1clustertemplates.getClusterTemplate(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateClusterTemplate = function getPrivateClusterTemplate (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1clustertemplates.getPrivateClusterTemplate(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesClusterTemplate = function getPrivatesClusterTemplate (req, res, next) {
  V1clustertemplates.getPrivatesClusterTemplate()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicClusterTemplate = function getPublicClusterTemplate (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1clustertemplates.getPublicClusterTemplate(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsClusterTemplate = function getPublicsClusterTemplate (req, res, next) {
  V1clustertemplates.getPublicsClusterTemplate()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateClusterTemplate = function postPrivateClusterTemplate (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1clustertemplates.postPrivateClusterTemplate(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicClusterTemplate = function postPublicClusterTemplate (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1clustertemplates.postPublicClusterTemplate(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
