'use strict';

var utils = require('../utils/writer.js');
var V1flexsubscriptions = require('../service/V1flexsubscriptionsService');

module.exports.deleteFlexSubscriptionById = function deleteFlexSubscriptionById (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1flexsubscriptions.deleteFlexSubscriptionById(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateFlexSubscriptionByName = function deletePrivateFlexSubscriptionByName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1flexsubscriptions.deletePrivateFlexSubscriptionByName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicFlexSubscriptionByName = function deletePublicFlexSubscriptionByName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1flexsubscriptions.deletePublicFlexSubscriptionByName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getFlexSubscriptionById = function getFlexSubscriptionById (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1flexsubscriptions.getFlexSubscriptionById(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateFlexSubscriptionByName = function getPrivateFlexSubscriptionByName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1flexsubscriptions.getPrivateFlexSubscriptionByName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateFlexSubscriptions = function getPrivateFlexSubscriptions (req, res, next) {
  V1flexsubscriptions.getPrivateFlexSubscriptions()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicFlexSubscriptionByName = function getPublicFlexSubscriptionByName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1flexsubscriptions.getPublicFlexSubscriptionByName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicFlexSubscriptions = function getPublicFlexSubscriptions (req, res, next) {
  V1flexsubscriptions.getPublicFlexSubscriptions()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateFlexSubscription = function postPrivateFlexSubscription (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1flexsubscriptions.postPrivateFlexSubscription(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicFlexSubscription = function postPublicFlexSubscription (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1flexsubscriptions.postPublicFlexSubscription(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putDefaultFlexSubscriptionById = function putDefaultFlexSubscriptionById (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1flexsubscriptions.putDefaultFlexSubscriptionById(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putPublicDefaultFlexSubscriptionByName = function putPublicDefaultFlexSubscriptionByName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1flexsubscriptions.putPublicDefaultFlexSubscriptionByName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putPublicUsedForControllerFlexSubscriptionByName = function putPublicUsedForControllerFlexSubscriptionByName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1flexsubscriptions.putPublicUsedForControllerFlexSubscriptionByName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putUsedForControllerFlexSubscriptionById = function putUsedForControllerFlexSubscriptionById (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1flexsubscriptions.putUsedForControllerFlexSubscriptionById(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
