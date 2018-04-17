'use strict';

var utils = require('../utils/writer.js');
var V1constraints = require('../service/V1constraintsService');

module.exports.deleteConstraint = function deleteConstraint (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1constraints.deleteConstraint(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateConstraint = function deletePrivateConstraint (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1constraints.deletePrivateConstraint(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicConstraint = function deletePublicConstraint (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1constraints.deletePublicConstraint(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getConstraint = function getConstraint (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1constraints.getConstraint(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateConstraint = function getPrivateConstraint (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1constraints.getPrivateConstraint(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesConstraint = function getPrivatesConstraint (req, res, next) {
  V1constraints.getPrivatesConstraint()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicConstraint = function getPublicConstraint (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1constraints.getPublicConstraint(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsConstraint = function getPublicsConstraint (req, res, next) {
  V1constraints.getPublicsConstraint()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateConstraint = function postPrivateConstraint (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1constraints.postPrivateConstraint(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicConstraint = function postPublicConstraint (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1constraints.postPublicConstraint(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
