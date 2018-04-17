'use strict';

var utils = require('../utils/writer.js');
var V1events = require('../service/V1eventsService');

module.exports.getEvents = function getEvents (req, res, next) {
  var since = req.swagger.params['since'].value;
  V1events.getEvents(since)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getEventsBySTackId = function getEventsBySTackId (req, res, next) {
  var stackId = req.swagger.params['stackId'].value;
  V1events.getEventsBySTackId(stackId)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getStructuredEvents = function getStructuredEvents (req, res, next) {
  var stackId = req.swagger.params['stackId'].value;
  V1events.getStructuredEvents(stackId)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getStructuredEventsZip = function getStructuredEventsZip (req, res, next) {
  var stackId = req.swagger.params['stackId'].value;
  V1events.getStructuredEventsZip(stackId)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
