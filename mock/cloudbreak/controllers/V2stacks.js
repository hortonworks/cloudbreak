'use strict';

var url = require('url');

var V2stacks = require('./V2stacksService');

module.exports.deleteInstanceStackV2 = function deleteInstanceStackV2 (req, res, next) {
  V2stacks.deleteInstanceStackV2(req.swagger.params, res, next);
};

module.exports.deletePrivateStackV2 = function deletePrivateStackV2 (req, res, next) {
  V2stacks.deletePrivateStackV2(req.swagger.params, res, next);
};

module.exports.deletePublicStackV2 = function deletePublicStackV2 (req, res, next) {
  V2stacks.deletePublicStackV2(req.swagger.params, res, next);
};

module.exports.deleteStackV2 = function deleteStackV2 (req, res, next) {
  V2stacks.deleteStackV2(req.swagger.params, res, next);
};

module.exports.getAllStackV2 = function getAllStackV2 (req, res, next) {
  V2stacks.getAllStackV2(req.swagger.params, res, next);
};

module.exports.getCertificateStackV2 = function getCertificateStackV2 (req, res, next) {
  V2stacks.getCertificateStackV2(req.swagger.params, res, next);
};

module.exports.getClusterRequestFromName = function getClusterRequestFromName (req, res, next) {
  V2stacks.getClusterRequestFromName(req.swagger.params, res, next);
};

module.exports.getPrivateStackV2 = function getPrivateStackV2 (req, res, next) {
  V2stacks.getPrivateStackV2(req.swagger.params, res, next);
};

module.exports.getPrivatesStackV2 = function getPrivatesStackV2 (req, res, next) {
  V2stacks.getPrivatesStackV2(req.swagger.params, res, next);
};

module.exports.getPublicStackV2 = function getPublicStackV2 (req, res, next) {
  V2stacks.getPublicStackV2(req.swagger.params, res, next);
};

module.exports.getPublicsStackV2 = function getPublicsStackV2 (req, res, next) {
  V2stacks.getPublicsStackV2(req.swagger.params, res, next);
};

module.exports.getStackForAmbariV2 = function getStackForAmbariV2 (req, res, next) {
  V2stacks.getStackForAmbariV2(req.swagger.params, res, next);
};

module.exports.getStackV2 = function getStackV2 (req, res, next) {
  V2stacks.getStackV2(req.swagger.params, res, next);
};

module.exports.postPrivateStackV2 = function postPrivateStackV2 (req, res, next) {
  V2stacks.postPrivateStackV2(req.swagger.params, res, next);
};

module.exports.postPublicStackV2 = function postPublicStackV2 (req, res, next) {
  V2stacks.postPublicStackV2(req.swagger.params, res, next);
};

module.exports.postPublicStackV2ForBlueprint = function postPublicStackV2ForBlueprint (req, res, next) {
  V2stacks.postPublicStackV2ForBlueprint(req.swagger.params, res, next);
};

module.exports.putpasswordStackV2 = function putpasswordStackV2 (req, res, next) {
  V2stacks.putpasswordStackV2(req.swagger.params, res, next);
};

module.exports.putreinstallStackV2 = function putreinstallStackV2 (req, res, next) {
  V2stacks.putreinstallStackV2(req.swagger.params, res, next);
};

module.exports.putrepairStackV2 = function putrepairStackV2 (req, res, next) {
  V2stacks.putrepairStackV2(req.swagger.params, res, next);
};

module.exports.putscalingStackV2 = function putscalingStackV2 (req, res, next) {
  V2stacks.putscalingStackV2(req.swagger.params, res, next);
};

module.exports.putstartStackV2 = function putstartStackV2 (req, res, next) {
  V2stacks.putstartStackV2(req.swagger.params, res, next);
};

module.exports.putstopStackV2 = function putstopStackV2 (req, res, next) {
  V2stacks.putstopStackV2(req.swagger.params, res, next);
};

module.exports.putsyncStackV2 = function putsyncStackV2 (req, res, next) {
  V2stacks.putsyncStackV2(req.swagger.params, res, next);
};

module.exports.statusStackV2 = function statusStackV2 (req, res, next) {
  V2stacks.statusStackV2(req.swagger.params, res, next);
};

module.exports.validateStackV2 = function validateStackV2 (req, res, next) {
  V2stacks.validateStackV2(req.swagger.params, res, next);
};

module.exports.variantsStackV2 = function variantsStackV2 (req, res, next) {
  V2stacks.variantsStackV2(req.swagger.params, res, next);
};
