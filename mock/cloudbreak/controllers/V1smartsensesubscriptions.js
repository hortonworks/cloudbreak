'use strict';

var url = require('url');

var V1smartsensesubscriptions = require('./V1smartsensesubscriptionsService');

module.exports.deletePrivateSmartSenseSubscriptionBySubscriptionId = function deletePrivateSmartSenseSubscriptionBySubscriptionId (req, res, next) {
  V1smartsensesubscriptions.deletePrivateSmartSenseSubscriptionBySubscriptionId(req.swagger.params, res, next);
};

module.exports.deletePublicSmartSenseSubscriptionBySubscriptionId = function deletePublicSmartSenseSubscriptionBySubscriptionId (req, res, next) {
  V1smartsensesubscriptions.deletePublicSmartSenseSubscriptionBySubscriptionId(req.swagger.params, res, next);
};

module.exports.deleteSmartSenseSubscriptionById = function deleteSmartSenseSubscriptionById (req, res, next) {
  V1smartsensesubscriptions.deleteSmartSenseSubscriptionById(req.swagger.params, res, next);
};

module.exports.getPrivateSmartSenseSubscriptions = function getPrivateSmartSenseSubscriptions (req, res, next) {
  V1smartsensesubscriptions.getPrivateSmartSenseSubscriptions(req.swagger.params, res, next);
};

module.exports.getPublicSmartSenseSubscriptions = function getPublicSmartSenseSubscriptions (req, res, next) {
  V1smartsensesubscriptions.getPublicSmartSenseSubscriptions(req.swagger.params, res, next);
};

module.exports.getSmartSenseSubscription = function getSmartSenseSubscription (req, res, next) {
  V1smartsensesubscriptions.getSmartSenseSubscription(req.swagger.params, res, next);
};

module.exports.getSmartSenseSubscriptionById = function getSmartSenseSubscriptionById (req, res, next) {
  V1smartsensesubscriptions.getSmartSenseSubscriptionById(req.swagger.params, res, next);
};

module.exports.postPrivateSmartSenseSubscription = function postPrivateSmartSenseSubscription (req, res, next) {
  V1smartsensesubscriptions.postPrivateSmartSenseSubscription(req.swagger.params, res, next);
};

module.exports.postPublicSmartSenseSubscription = function postPublicSmartSenseSubscription (req, res, next) {
  V1smartsensesubscriptions.postPublicSmartSenseSubscription(req.swagger.params, res, next);
};
