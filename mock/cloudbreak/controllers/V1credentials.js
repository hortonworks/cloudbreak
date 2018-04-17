'use strict';

var utils = require('../utils/writer.js');
var V1credentials = require('../service/V1credentialsService');

module.exports.deleteCredential = function deleteCredential (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1credentials.deleteCredential(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateCredential = function deletePrivateCredential (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1credentials.deletePrivateCredential(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicCredential = function deletePublicCredential (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1credentials.deletePublicCredential(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getCredential = function getCredential (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1credentials.getCredential(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateCredential = function getPrivateCredential (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1credentials.getPrivateCredential(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesCredential = function getPrivatesCredential (req, res, next) {
  V1credentials.getPrivatesCredential()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicCredential = function getPublicCredential (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1credentials.getPublicCredential(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsCredential = function getPublicsCredential (req, res, next) {
  V1credentials.getPublicsCredential()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateCredential = function postPrivateCredential (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1credentials.postPrivateCredential(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicCredential = function postPublicCredential (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1credentials.postPublicCredential(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.privateInteractiveLoginCredential = function privateInteractiveLoginCredential (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1credentials.privateInteractiveLoginCredential(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.publicInteractiveLoginCredential = function publicInteractiveLoginCredential (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1credentials.publicInteractiveLoginCredential(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putPrivateCredential = function putPrivateCredential (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1credentials.putPrivateCredential(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putPublicCredential = function putPublicCredential (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1credentials.putPublicCredential(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
