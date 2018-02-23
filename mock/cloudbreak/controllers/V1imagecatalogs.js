'use strict';

var url = require('url');

var V1imagecatalogs = require('./V1imagecatalogsService');

module.exports.deletePublicImageCatalogByName = function deletePublicImageCatalogByName (req, res, next) {
    V1imagecatalogs.deletePublicImageCatalogByName(req.swagger.params, res, next);
};

module.exports.getImageCatalogRequestFromName = function getImageCatalogRequestFromName (req, res, next) {
    V1imagecatalogs.getImageCatalogRequestFromName(req.swagger.params, res, next);
};

module.exports.getImagesByProvider = function getImagesByProvider (req, res, next) {
    V1imagecatalogs.getImagesByProvider(req.swagger.params, res, next);
};

module.exports.getPublicImageCatalogsByName = function getPublicImageCatalogsByName (req, res, next) {
  V1imagecatalogs.getPublicImageCatalogsByName(req.swagger.params, res, next);
};

module.exports.getPublicImagesByProviderAndCustomImageCatalog = function getPublicImagesByProviderAndCustomImageCatalog (req, res, next) {
    V1imagecatalogs.getPublicImagesByProviderAndCustomImageCatalog(req.swagger.params, res, next);
};

module.exports.getPublicsImageCatalogs = function getPublicsImageCatalogs (req, res, next) {
    V1imagecatalogs.getPublicsImageCatalogs(req.swagger.params, res, next);
};

module.exports.postPrivateImageCatalog = function postPrivateImageCatalog (req, res, next) {
    V1imagecatalogs.postPrivateImageCatalog(req.swagger.params, res, next);
};

module.exports.postPublicImageCatalog = function postPublicImageCatalog (req, res, next) {
    V1imagecatalogs.postPublicImageCatalog(req.swagger.params, res, next);
};

module.exports.putPublicImageCatalog = function putPublicImageCatalog (req, res, next) {
    V1imagecatalogs.putPublicImageCatalog(req.swagger.params, res, next);
};

module.exports.putSetDefaultImageCatalogByName = function putSetDefaultImageCatalogByName (req, res, next) {
    V1imagecatalogs.putSetDefaultImageCatalogByName(req.swagger.params, res, next);
};
