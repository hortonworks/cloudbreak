'use strict';

var utils = require('../utils/writer.js');
var V1smartsensesubscriptions = require('../service/V1smartsensesubscriptionsService');

module.exports.deletePrivateSmartSenseSubscriptionBySubscriptionId = function deletePrivateSmartSenseSubscriptionBySubscriptionId (req, res, next) {
  var subscriptionId = req.swagger.params['subscriptionId'].value;
  V1smartsensesubscriptions.deletePrivateSmartSenseSubscriptionBySubscriptionId(subscriptionId)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicSmartSenseSubscriptionBySubscriptionId = function deletePublicSmartSenseSubscriptionBySubscriptionId (req, res, next) {
  var subscriptionId = req.swagger.params['subscriptionId'].value;
  V1smartsensesubscriptions.deletePublicSmartSenseSubscriptionBySubscriptionId(subscriptionId)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deleteSmartSenseSubscriptionById = function deleteSmartSenseSubscriptionById (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1smartsensesubscriptions.deleteSmartSenseSubscriptionById(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateSmartSenseSubscriptions = function getPrivateSmartSenseSubscriptions (req, res, next) {
  V1smartsensesubscriptions.getPrivateSmartSenseSubscriptions()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicSmartSenseSubscriptions = function getPublicSmartSenseSubscriptions (req, res, next) {
  V1smartsensesubscriptions.getPublicSmartSenseSubscriptions()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getSmartSenseSubscription = function getSmartSenseSubscription (req, res, next) {
  V1smartsensesubscriptions.getSmartSenseSubscription()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getSmartSenseSubscriptionById = function getSmartSenseSubscriptionById (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1smartsensesubscriptions.getSmartSenseSubscriptionById(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateSmartSenseSubscription = function postPrivateSmartSenseSubscription (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1smartsensesubscriptions.postPrivateSmartSenseSubscription(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicSmartSenseSubscription = function postPublicSmartSenseSubscription (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1smartsensesubscriptions.postPublicSmartSenseSubscription(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
