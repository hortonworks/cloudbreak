'use strict';

exports.createRecommendation = function(args, res, next) {
  /**
   * creates a recommendation that advises cloud resources for the given blueprint
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * body RecommendationRequestJson  (optional)
   * returns RecommendationResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/connectors/recommendations.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getAccessConfigs = function(args, res, next) {
  /**
   * retrive access configs with properties
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * body PlatformResourceRequestJson  (optional)
   * returns PlatformAccessConfigsResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "accessConfigs" : [ {
    "name" : "aeiou",
    "id" : "aeiou",
    "properties" : {
      "key" : "{}"
    }
  } ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getDisktypeByType = function(args, res, next) {
  /**
   * retrive disks by type
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * type String 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ "aeiou" ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getDisktypes = function(args, res, next) {
  /**
   * retrive available disk types
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * returns PlatformDisksJson
   **/
  var examples = {};
  examples['application/json'] = require('../responses/connectors/disktypes.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getGatewaysCredentialId = function(args, res, next) {
  /**
   * retrive gateways with properties
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * body PlatformResourceRequestJson  (optional)
   * returns PlatformGatewaysResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/connectors/gateways.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getIpPoolsCredentialId = function(args, res, next) {
  /**
   * retrive ip pools with properties
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * body PlatformResourceRequestJson  (optional)
   * returns PlatformIpPoolsResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/connectors/ippools.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getOchestratorsByType = function(args, res, next) {
  /**
   * retrive orchestrators by type
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * type String 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ "aeiou" ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getOrchestratortypes = function(args, res, next) {
  /**
   * retrive available orchestrator types
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * returns PlatformOrchestratorsJson
   **/
  var examples = {};
  examples['application/json'] = {
  "defaults" : {
    "key" : "aeiou"
  },
  "orchestrators" : {
    "key" : [ "aeiou" ]
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPlatformNetworks = function(args, res, next) {
  /**
   * retrive network properties
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * body PlatformResourceRequestJson  (optional)
   * returns PlatformNetworksResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/connectors/networks.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPlatformSShKeys = function(args, res, next) {
  /**
   * retrive sshkeys properties
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * body PlatformResourceRequestJson  (optional)
   * returns PlatformSshKeysResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/connectors/sshkeys.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPlatformSecurityGroups = function(args, res, next) {
  /**
   * retrive securitygroups properties
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * body PlatformResourceRequestJson  (optional)
   * returns PlatformSecurityGroupsResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "securityGroups" : {
    "key" : [ {
      "groupName" : "aeiou",
      "groupId" : "aeiou",
      "properties" : {
        "key" : "{}"
      }
    } ]
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPlatformVariantByType = function(args, res, next) {
  /**
   * retrive a platform variant by type
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * type String 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ "aeiou" ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPlatformVariants = function(args, res, next) {
  /**
   * retrive available platform variants
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * returns PlatformVariantsJson
   **/
  var examples = {};
  examples['application/json'] = {
  "platformToVariants" : {
    "key" : [ "aeiou" ]
  },
  "defaultVariants" : {
    "key" : "aeiou"
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPlatforms = function(args, res, next) {
  /**
   * retrive available platforms
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * extended Boolean  (optional)
   * returns Map
   **/
  var examples = {};
  examples['application/json'] = {
  "key" : "{}"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getRegionAvByType = function(args, res, next) {
  /**
   * retrive availability zones by type
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * type String 
   * returns Map
   **/
  var examples = {};
  examples['application/json'] = {
  "key" : [ "aeiou" ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getRegionRByType = function(args, res, next) {
  /**
   * retrive regions by type
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * type String 
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ "aeiou" ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getRegions = function(args, res, next) {
  /**
   * retrive available regions
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * returns PlatformRegionsJson
   **/
  var examples = {};
  examples['application/json'] = require('../responses/connectors/regions.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getSpecialProperties = function(args, res, next) {
  /**
   * retrive special properties
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * returns SpecialParametersJson
   **/
  var examples = {};
  examples['application/json'] = {
  "platformSpecificSpecialParameters" : {
    "key" : {
      "key" : true
    }
  },
  "specialParameters" : {
    "key" : true
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getTagSpecifications = function(args, res, next) {
  /**
   * retrive tag specifications
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * returns TagSpecificationsJson
   **/
  var examples = {};
  examples['application/json'] = {
  "specifications" : {
    "key" : {
      "key" : "{}"
    }
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

