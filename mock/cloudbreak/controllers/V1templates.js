'use strict';

var utils = require('../utils/writer.js');
var V1templates = require('../service/V1templatesService');

module.exports.deletePrivateTemplate = function deletePrivateTemplate (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1templates.deletePrivateTemplate(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicTemplate = function deletePublicTemplate (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1templates.deletePublicTemplate(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deleteTemplate = function deleteTemplate (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1templates.deleteTemplate(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateTemplate = function getPrivateTemplate (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1templates.getPrivateTemplate(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesTemplate = function getPrivatesTemplate (req, res, next) {
  V1templates.getPrivatesTemplate()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicTemplate = function getPublicTemplate (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1templates.getPublicTemplate(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsTemplate = function getPublicsTemplate (req, res, next) {
  V1templates.getPublicsTemplate()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getTemplate = function getTemplate (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1templates.getTemplate(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
