'use strict';

var url = require('url');

var V1mpacks = require('./V1mpacksService');

module.exports.deleteManagementPack = function deleteManagementPack (req, res, next) {
  V1mpacks.deleteManagementPack(req.swagger.params, res, next);
};

module.exports.deletePrivateManagementPack = function deletePrivateManagementPack (req, res, next) {
  V1mpacks.deletePrivateManagementPack(req.swagger.params, res, next);
};

module.exports.deletePublicManagementPack = function deletePublicManagementPack (req, res, next) {
  V1mpacks.deletePublicManagementPack(req.swagger.params, res, next);
};

module.exports.getManagementPack = function getManagementPack (req, res, next) {
  V1mpacks.getManagementPack(req.swagger.params, res, next);
};

module.exports.getPrivateManagementPack = function getPrivateManagementPack (req, res, next) {
  V1mpacks.getPrivateManagementPack(req.swagger.params, res, next);
};

module.exports.getPrivateManagementPacks = function getPrivateManagementPacks (req, res, next) {
  V1mpacks.getPrivateManagementPacks(req.swagger.params, res, next);
};

module.exports.getPublicManagementPack = function getPublicManagementPack (req, res, next) {
  V1mpacks.getPublicManagementPack(req.swagger.params, res, next);
};

module.exports.getPublicManagementPacks = function getPublicManagementPacks (req, res, next) {
  V1mpacks.getPublicManagementPacks(req.swagger.params, res, next);
};

module.exports.postPrivateManagementPack = function postPrivateManagementPack (req, res, next) {
  V1mpacks.postPrivateManagementPack(req.swagger.params, res, next);
};

module.exports.postPublicManagementPack = function postPublicManagementPack (req, res, next) {
  V1mpacks.postPublicManagementPack(req.swagger.params, res, next);
};
