'use strict';

var url = require('url');

var V1alerts = require('./V1alertsService');

module.exports.createMetricAlerts = function createMetricAlerts (req, res, next) {
  V1alerts.createMetricAlerts(req.swagger.params, res, next);
};

module.exports.createPrometheusAlert = function createPrometheusAlert (req, res, next) {
  V1alerts.createPrometheusAlert(req.swagger.params, res, next);
};

module.exports.createTimeAlert = function createTimeAlert (req, res, next) {
  V1alerts.createTimeAlert(req.swagger.params, res, next);
};

module.exports.deleteMetricAlarm = function deleteMetricAlarm (req, res, next) {
  V1alerts.deleteMetricAlarm(req.swagger.params, res, next);
};

module.exports.deletePrometheusAlarm = function deletePrometheusAlarm (req, res, next) {
  V1alerts.deletePrometheusAlarm(req.swagger.params, res, next);
};

module.exports.deleteTimeAlert = function deleteTimeAlert (req, res, next) {
  V1alerts.deleteTimeAlert(req.swagger.params, res, next);
};

module.exports.getAlertDefinitions = function getAlertDefinitions (req, res, next) {
  V1alerts.getAlertDefinitions(req.swagger.params, res, next);
};

module.exports.getMetricAlerts = function getMetricAlerts (req, res, next) {
  V1alerts.getMetricAlerts(req.swagger.params, res, next);
};

module.exports.getPrometheusAlerts = function getPrometheusAlerts (req, res, next) {
  V1alerts.getPrometheusAlerts(req.swagger.params, res, next);
};

module.exports.getPrometheusDefinitions = function getPrometheusDefinitions (req, res, next) {
  V1alerts.getPrometheusDefinitions(req.swagger.params, res, next);
};

module.exports.getTimeAlerts = function getTimeAlerts (req, res, next) {
  V1alerts.getTimeAlerts(req.swagger.params, res, next);
};

module.exports.updateMetricAlerts = function updateMetricAlerts (req, res, next) {
  V1alerts.updateMetricAlerts(req.swagger.params, res, next);
};

module.exports.updatePrometheusAlert = function updatePrometheusAlert (req, res, next) {
  V1alerts.updatePrometheusAlert(req.swagger.params, res, next);
};

module.exports.updateTimeAlert = function updateTimeAlert (req, res, next) {
  V1alerts.updateTimeAlert(req.swagger.params, res, next);
};

module.exports.validateCronExpression = function validateCronExpression (req, res, next) {
  V1alerts.validateCronExpression(req.swagger.params, res, next);
};
