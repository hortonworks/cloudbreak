'use strict';

var utils = require('../utils/writer.js');
var V1connectors = require('../service/V1connectorsService');

module.exports.createRecommendation = function createRecommendation (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1connectors.createRecommendation(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getAccessConfigs = function getAccessConfigs (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1connectors.getAccessConfigs(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getDisktypeByType = function getDisktypeByType (req, res, next) {
  var type = req.swagger.params['type'].value;
  V1connectors.getDisktypeByType(type)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getDisktypes = function getDisktypes (req, res, next) {
  V1connectors.getDisktypes()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getGatewaysCredentialId = function getGatewaysCredentialId (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1connectors.getGatewaysCredentialId(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getIpPoolsCredentialId = function getIpPoolsCredentialId (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1connectors.getIpPoolsCredentialId(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getOchestratorsByType = function getOchestratorsByType (req, res, next) {
  var type = req.swagger.params['type'].value;
  V1connectors.getOchestratorsByType(type)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getOrchestratortypes = function getOrchestratortypes (req, res, next) {
  V1connectors.getOrchestratortypes()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPlatformNetworks = function getPlatformNetworks (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1connectors.getPlatformNetworks(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPlatformSShKeys = function getPlatformSShKeys (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1connectors.getPlatformSShKeys(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPlatformSecurityGroups = function getPlatformSecurityGroups (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1connectors.getPlatformSecurityGroups(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPlatformVariantByType = function getPlatformVariantByType (req, res, next) {
  var type = req.swagger.params['type'].value;
  V1connectors.getPlatformVariantByType(type)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPlatformVariants = function getPlatformVariants (req, res, next) {
  V1connectors.getPlatformVariants()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPlatforms = function getPlatforms (req, res, next) {
  var extended = req.swagger.params['extended'].value;
  V1connectors.getPlatforms(extended)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getRegionAvByType = function getRegionAvByType (req, res, next) {
  var type = req.swagger.params['type'].value;
  V1connectors.getRegionAvByType(type)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getRegionRByType = function getRegionRByType (req, res, next) {
  var type = req.swagger.params['type'].value;
  V1connectors.getRegionRByType(type)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getRegions = function getRegions (req, res, next) {
  V1connectors.getRegions()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getSpecialProperties = function getSpecialProperties (req, res, next) {
  V1connectors.getSpecialProperties()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getTagSpecifications = function getTagSpecifications (req, res, next) {
  V1connectors.getTagSpecifications()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
