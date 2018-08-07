'use strict';


/**
 * retrive regions by type
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * body PlatformResourceRequestJson  (optional)
 * returns RegionResponse
 **/
exports.getRegionsByCredentialId = function(body) {
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
 * retrive vmtype properties by credential
 * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
 *
 * body PlatformResourceRequestJson  (optional)
 * returns PlatformVmtypesResponse
 **/
exports.getVmTypesByCredentialId = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "vmTypes" : {
    "key" : {
      "defaultVirtualMachine" : {
        "vmTypeMetaJson" : {
          "configs" : [ {
            "volumeParameterType" : "volumeParameterType",
            "minimumSize" : 0,
            "maximumNumber" : 5,
            "maximumSize" : 6,
            "minimumNumber" : 1
          }, {
            "volumeParameterType" : "volumeParameterType",
            "minimumSize" : 0,
            "maximumNumber" : 5,
            "maximumSize" : 6,
            "minimumNumber" : 1
          } ],
          "properties" : {
            "key" : "properties"
          }
        },
        "value" : "value"
      },
      "virtualMachines" : [ {
        "vmTypeMetaJson" : {
          "configs" : [ {
            "volumeParameterType" : "volumeParameterType",
            "minimumSize" : 0,
            "maximumNumber" : 5,
            "maximumSize" : 6,
            "minimumNumber" : 1
          }, {
            "volumeParameterType" : "volumeParameterType",
            "minimumSize" : 0,
            "maximumNumber" : 5,
            "maximumSize" : 6,
            "minimumNumber" : 1
          } ],
          "properties" : {
            "key" : "properties"
          }
        },
        "value" : "value"
      }, {
        "vmTypeMetaJson" : {
          "configs" : [ {
            "volumeParameterType" : "volumeParameterType",
            "minimumSize" : 0,
            "maximumNumber" : 5,
            "maximumSize" : 6,
            "minimumNumber" : 1
          }, {
            "volumeParameterType" : "volumeParameterType",
            "minimumSize" : 0,
            "maximumNumber" : 5,
            "maximumSize" : 6,
            "minimumNumber" : 1
          } ],
          "properties" : {
            "key" : "properties"
          }
        },
        "value" : "value"
      } ]
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

