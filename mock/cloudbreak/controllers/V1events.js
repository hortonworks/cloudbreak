'use strict';

var url = require('url');

var V1events = require('./V1eventsService');

module.exports.getEvents = function getEvents (req, res, next) {
  V1events.getEvents(req.swagger.params, res, next);
};

module.exports.getEventsBySTackId = function getEventsBySTackId (req, res, next) {
  V1events.getEventsBySTackId(req.swagger.params, res, next);
};

module.exports.getStructuredEvents = function getStructuredEvents (req, res, next) {
  V1events.getStructuredEvents(req.swagger.params, res, next);
};

module.exports.getStructuredEventsZip = function getStructuredEventsZip (req, res, next) {
  V1events.getStructuredEventsZip(req.swagger.params, res, next);
};
