'use strict';

var utils = require('../utils/writer.js');
var V1topologies = require('../service/V1topologiesService');

module.exports.deleteTopology = function deleteTopology (req, res, next) {
  var id = req.swagger.params['id'].value;
  var forced = req.swagger.params['forced'].value;
  V1topologies.deleteTopology(id,forced)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsTopology = function getPublicsTopology (req, res, next) {
  V1topologies.getPublicsTopology()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getTopology = function getTopology (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1topologies.getTopology(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicTopology = function postPublicTopology (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1topologies.postPublicTopology(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
