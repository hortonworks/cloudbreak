'use strict';

var url = require('url');

var V1stacks = require('./V1stacksService');

module.exports.deleteInstanceStack = function deleteInstanceStack (req, res, next) {
  V1stacks.deleteInstanceStack(req.swagger.params, res, next);
};

module.exports.deletePrivateStack = function deletePrivateStack (req, res, next) {
  V1stacks.deletePrivateStack(req.swagger.params, res, next);
};

module.exports.deletePublicStack = function deletePublicStack (req, res, next) {
  V1stacks.deletePublicStack(req.swagger.params, res, next);
};

module.exports.deleteStack = function deleteStack (req, res, next) {
  V1stacks.deleteStack(req.swagger.params, res, next);
};

module.exports.getAllStack = function getAllStack (req, res, next) {
  V1stacks.getAllStack(req.swagger.params, res, next);
};

module.exports.getCertificateStack = function getCertificateStack (req, res, next) {
  V1stacks.getCertificateStack(req.swagger.params, res, next);
};

module.exports.getPrivateStack = function getPrivateStack (req, res, next) {
  V1stacks.getPrivateStack(req.swagger.params, res, next);
};

module.exports.getPrivatesStack = function getPrivatesStack (req, res, next) {
  V1stacks.getPrivatesStack(req.swagger.params, res, next);
};

module.exports.getPublicStack = function getPublicStack (req, res, next) {
  V1stacks.getPublicStack(req.swagger.params, res, next);
};

module.exports.getPublicsStack = function getPublicsStack (req, res, next) {
  V1stacks.getPublicsStack(req.swagger.params, res, next);
};

module.exports.getStack = function getStack (req, res, next) {
  V1stacks.getStack(req.swagger.params, res, next);
};

module.exports.getStackForAmbari = function getStackForAmbari (req, res, next) {
  V1stacks.getStackForAmbari(req.swagger.params, res, next);
};

module.exports.postPrivateStack = function postPrivateStack (req, res, next) {
  V1stacks.postPrivateStack(req.swagger.params, res, next);
};

module.exports.postPublicStack = function postPublicStack (req, res, next) {
  V1stacks.postPublicStack(req.swagger.params, res, next);
};

module.exports.putStack = function putStack (req, res, next) {
  V1stacks.putStack(req.swagger.params, res, next);
};

module.exports.statusStack = function statusStack (req, res, next) {
  V1stacks.statusStack(req.swagger.params, res, next);
};

module.exports.validateStack = function validateStack (req, res, next) {
  V1stacks.validateStack(req.swagger.params, res, next);
};

module.exports.variantsStack = function variantsStack (req, res, next) {
  V1stacks.variantsStack(req.swagger.params, res, next);
};
