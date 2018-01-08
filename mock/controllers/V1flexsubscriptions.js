'use strict';

var url = require('url');

var V1flexsubscriptions = require('./V1flexsubscriptionsService');

module.exports.deleteFlexSubscriptionById = function deleteFlexSubscriptionById (req, res, next) {
  V1flexsubscriptions.deleteFlexSubscriptionById(req.swagger.params, res, next);
};

module.exports.deletePrivateFlexSubscriptionByName = function deletePrivateFlexSubscriptionByName (req, res, next) {
  V1flexsubscriptions.deletePrivateFlexSubscriptionByName(req.swagger.params, res, next);
};

module.exports.deletePublicFlexSubscriptionByName = function deletePublicFlexSubscriptionByName (req, res, next) {
  V1flexsubscriptions.deletePublicFlexSubscriptionByName(req.swagger.params, res, next);
};

module.exports.getFlexSubscriptionById = function getFlexSubscriptionById (req, res, next) {
  V1flexsubscriptions.getFlexSubscriptionById(req.swagger.params, res, next);
};

module.exports.getPrivateFlexSubscriptionByName = function getPrivateFlexSubscriptionByName (req, res, next) {
  V1flexsubscriptions.getPrivateFlexSubscriptionByName(req.swagger.params, res, next);
};

module.exports.getPrivateFlexSubscriptions = function getPrivateFlexSubscriptions (req, res, next) {
  V1flexsubscriptions.getPrivateFlexSubscriptions(req.swagger.params, res, next);
};

module.exports.getPublicFlexSubscriptionByName = function getPublicFlexSubscriptionByName (req, res, next) {
  V1flexsubscriptions.getPublicFlexSubscriptionByName(req.swagger.params, res, next);
};

module.exports.getPublicFlexSubscriptions = function getPublicFlexSubscriptions (req, res, next) {
  V1flexsubscriptions.getPublicFlexSubscriptions(req.swagger.params, res, next);
};

module.exports.postPrivateFlexSubscription = function postPrivateFlexSubscription (req, res, next) {
  V1flexsubscriptions.postPrivateFlexSubscription(req.swagger.params, res, next);
};

module.exports.postPublicFlexSubscription = function postPublicFlexSubscription (req, res, next) {
  V1flexsubscriptions.postPublicFlexSubscription(req.swagger.params, res, next);
};

module.exports.putDefaultFlexSubscriptionById = function putDefaultFlexSubscriptionById (req, res, next) {
  V1flexsubscriptions.putDefaultFlexSubscriptionById(req.swagger.params, res, next);
};

module.exports.putPublicDefaultFlexSubscriptionByName = function putPublicDefaultFlexSubscriptionByName (req, res, next) {
  V1flexsubscriptions.putPublicDefaultFlexSubscriptionByName(req.swagger.params, res, next);
};

module.exports.putPublicUsedForControllerFlexSubscriptionByName = function putPublicUsedForControllerFlexSubscriptionByName (req, res, next) {
  V1flexsubscriptions.putPublicUsedForControllerFlexSubscriptionByName(req.swagger.params, res, next);
};

module.exports.putUsedForControllerFlexSubscriptionById = function putUsedForControllerFlexSubscriptionById (req, res, next) {
  V1flexsubscriptions.putUsedForControllerFlexSubscriptionById(req.swagger.params, res, next);
};
