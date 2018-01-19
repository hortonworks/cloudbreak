'use strict';

var url = require('url');

var V1history = require('./V1historyService');

module.exports.getHistory = function getHistory (req, res, next) {
  V1history.getHistory(req.swagger.params, res, next);
};

module.exports.getHistoryById = function getHistoryById (req, res, next) {
  V1history.getHistoryById(req.swagger.params, res, next);
};
