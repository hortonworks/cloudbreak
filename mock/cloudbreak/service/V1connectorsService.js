'use strict';


/**
 * creates a recommendation that advises cloud resources for the given blueprint
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * body RecommendationRequestJson  (optional)
 * returns RecommendationResponse
 **/
exports.createRecommendation = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/connectors/recommendations.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive access configs with properties
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * body PlatformResourceRequestJson  (optional)
 * returns PlatformAccessConfigsResponse
 **/
exports.getAccessConfigs = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "accessConfigs" : [ {
    "name" : "name",
    "id" : "id",
    "properties" : {
      "key" : "{}"
    }
  }, {
    "name" : "name",
    "id" : "id",
    "properties" : {
      "key" : "{}"
    }
  } ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive disks by type
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * type String 
 * returns List
 **/
exports.getDisktypeByType = function(type) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ "", "" ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive available disk types
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * returns PlatformDisksJson
 **/
exports.getDisktypes = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/connectors/disktypes.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive gateways with properties
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * body PlatformResourceRequestJson  (optional)
 * returns PlatformGatewaysResponse
 **/
exports.getGatewaysCredentialId = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/connectors/gateways.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive ip pools with properties
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * body PlatformResourceRequestJson  (optional)
 * returns PlatformIpPoolsResponse
 **/
exports.getIpPoolsCredentialId = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/connectors/ippools.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive orchestrators by type
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * type String 
 * returns List
 **/
exports.getOchestratorsByType = function(type) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ "", "" ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive available orchestrator types
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * returns PlatformOrchestratorsJson
 **/
exports.getOrchestratortypes = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "defaults" : {
    "key" : "defaults"
  },
  "orchestrators" : {
    "key" : [ "orchestrators", "orchestrators" ]
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive network properties
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * body PlatformResourceRequestJson  (optional)
 * returns PlatformNetworksResponse
 **/
exports.getPlatformNetworks = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/connectors/networks.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive sshkeys properties
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * body PlatformResourceRequestJson  (optional)
 * returns PlatformSshKeysResponse
 **/
exports.getPlatformSShKeys = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/connectors/sshkeys.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive securitygroups properties
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * body PlatformResourceRequestJson  (optional)
 * returns PlatformSecurityGroupsResponse
 **/
exports.getPlatformSecurityGroups = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "securityGroups" : {
    "key" : [ {
      "groupName" : "groupName",
      "groupId" : "groupId",
      "properties" : {
        "key" : "{}"
      }
    }, {
      "groupName" : "groupName",
      "groupId" : "groupId",
      "properties" : {
        "key" : "{}"
      }
    } ]
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive a platform variant by type
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * type String 
 * returns List
 **/
exports.getPlatformVariantByType = function(type) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ "", "" ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive available platform variants
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * returns PlatformVariantsJson
 **/
exports.getPlatformVariants = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "platformToVariants" : {
    "key" : [ "platformToVariants", "platformToVariants" ]
  },
  "defaultVariants" : {
    "key" : "defaultVariants"
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive available platforms
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * extended Boolean  (optional)
 * returns Map
 **/
exports.getPlatforms = function(extended) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "key" : "{}"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive availability zones by type
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * type String 
 * returns Map
 **/
exports.getRegionAvByType = function(type) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "key" : [ "", "" ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive regions by type
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * type String 
 * returns List
 **/
exports.getRegionRByType = function(type) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ "", "" ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive available regions
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * returns PlatformRegionsJson
 **/
exports.getRegions = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/connectors/regions.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive special properties
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * returns SpecialParametersJson
 **/
exports.getSpecialProperties = function() {
  return new Promise(function(resolve, reject) {
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
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive tag specifications
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * returns TagSpecificationsJson
 **/
exports.getTagSpecifications = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "specifications" : {
    "key" : {
      "key" : "{}"
    }
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

