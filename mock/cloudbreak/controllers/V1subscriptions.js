'use strict';

var utils = require('../utils/writer.js');
var V1subscriptions = require('../service/V1subscriptionsService');

module.exports.subscribeSubscription = function subscribeSubscription (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1subscriptions.subscribeSubscription(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
