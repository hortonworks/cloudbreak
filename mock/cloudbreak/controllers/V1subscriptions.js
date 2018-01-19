'use strict';

var url = require('url');

var V1subscriptions = require('./V1subscriptionsService');

module.exports.subscribeSubscription = function subscribeSubscription (req, res, next) {
  V1subscriptions.subscribeSubscription(req.swagger.params, res, next);
};
