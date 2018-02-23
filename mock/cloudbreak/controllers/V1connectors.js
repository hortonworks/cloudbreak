'use strict';

var url = require('url');

var V1connectors = require('./V1connectorsService');

module.exports.createRecommendation = function createRecommendation (req, res, next) {
  V1connectors.createRecommendation(req.swagger.params, res, next);
};

module.exports.getAccessConfigs = function getAccessConfigs (req, res, next) {
  V1connectors.getAccessConfigs(req.swagger.params, res, next);
};

module.exports.getDisktypeByType = function getDisktypeByType (req, res, next) {
  V1connectors.getDisktypeByType(req.swagger.params, res, next);
};

module.exports.getDisktypes = function getDisktypes (req, res, next) {
  V1connectors.getDisktypes(req.swagger.params, res, next);
};

module.exports.getGatewaysCredentialId = function getGatewaysCredentialId (req, res, next) {
  V1connectors.getGatewaysCredentialId(req.swagger.params, res, next);
};

module.exports.getIpPoolsCredentialId = function getIpPoolsCredentialId (req, res, next) {
  V1connectors.getIpPoolsCredentialId(req.swagger.params, res, next);
};

module.exports.getOchestratorsByType = function getOchestratorsByType (req, res, next) {
  V1connectors.getOchestratorsByType(req.swagger.params, res, next);
};

module.exports.getOrchestratortypes = function getOrchestratortypes (req, res, next) {
  V1connectors.getOrchestratortypes(req.swagger.params, res, next);
};

module.exports.getPlatformNetworks = function getPlatformNetworks (req, res, next) {
  V1connectors.getPlatformNetworks(req.swagger.params, res, next);
};

module.exports.getPlatformSShKeys = function getPlatformSShKeys (req, res, next) {
  V1connectors.getPlatformSShKeys(req.swagger.params, res, next);
};

module.exports.getPlatformSecurityGroups = function getPlatformSecurityGroups (req, res, next) {
  V1connectors.getPlatformSecurityGroups(req.swagger.params, res, next);
};

module.exports.getPlatformVariantByType = function getPlatformVariantByType (req, res, next) {
  V1connectors.getPlatformVariantByType(req.swagger.params, res, next);
};

module.exports.getPlatformVariants = function getPlatformVariants (req, res, next) {
  V1connectors.getPlatformVariants(req.swagger.params, res, next);
};

module.exports.getPlatforms = function getPlatforms (req, res, next) {
  V1connectors.getPlatforms(req.swagger.params, res, next);
};

module.exports.getRegionAvByType = function getRegionAvByType (req, res, next) {
  V1connectors.getRegionAvByType(req.swagger.params, res, next);
};

module.exports.getRegionRByType = function getRegionRByType (req, res, next) {
  V1connectors.getRegionRByType(req.swagger.params, res, next);
};

module.exports.getRegions = function getRegions (req, res, next) {
  V1connectors.getRegions(req.swagger.params, res, next);
};

module.exports.getSpecialProperties = function getSpecialProperties (req, res, next) {
  V1connectors.getSpecialProperties(req.swagger.params, res, next);
};

module.exports.getTagSpecifications = function getTagSpecifications (req, res, next) {
  V1connectors.getTagSpecifications(req.swagger.params, res, next);
};
