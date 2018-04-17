'use strict';

var utils = require('../utils/writer.js');
var V1imagecatalogs = require('../service/V1imagecatalogsService');

module.exports.deletePublicImageCatalogByName = function deletePublicImageCatalogByName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1imagecatalogs.deletePublicImageCatalogByName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getImageCatalogRequestFromName = function getImageCatalogRequestFromName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1imagecatalogs.getImageCatalogRequestFromName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getImagesByProvider = function getImagesByProvider (req, res, next) {
  var platform = req.swagger.params['platform'].value;
  V1imagecatalogs.getImagesByProvider(platform)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicImageCatalogsByName = function getPublicImageCatalogsByName (req, res, next) {
  var name = req.swagger.params['name'].value;
  var withImages = req.swagger.params['withImages'].value;
  V1imagecatalogs.getPublicImageCatalogsByName(name,withImages)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicImagesByProviderAndCustomImageCatalog = function getPublicImagesByProviderAndCustomImageCatalog (req, res, next) {
  var name = req.swagger.params['name'].value;
  var platform = req.swagger.params['platform'].value;
  V1imagecatalogs.getPublicImagesByProviderAndCustomImageCatalog(name,platform)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsImageCatalogs = function getPublicsImageCatalogs (req, res, next) {
  V1imagecatalogs.getPublicsImageCatalogs()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateImageCatalog = function postPrivateImageCatalog (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1imagecatalogs.postPrivateImageCatalog(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicImageCatalog = function postPublicImageCatalog (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1imagecatalogs.postPublicImageCatalog(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putPublicImageCatalog = function putPublicImageCatalog (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1imagecatalogs.putPublicImageCatalog(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putSetDefaultImageCatalogByName = function putSetDefaultImageCatalogByName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1imagecatalogs.putSetDefaultImageCatalogByName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
