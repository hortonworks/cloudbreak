'use strict';

exports.getRegionsByCredentialId = function(args, res, next) {
  /**
   * retrive regions by type
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * body PlatformResourceRequestJson  (optional)
   * returns RegionResponse
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

exports.getVmTypesByCredentialId = function(args, res, next) {
  /**
   * retrive vmtype properties by credential
   * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
   *
   * body PlatformResourceRequestJson  (optional)
   * returns PlatformVmtypesResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "vmTypes" : {
    "key" : {
      "defaultVirtualMachine" : "",
      "virtualMachines" : [ {
        "vmTypeMetaJson" : {
          "configs" : [ {
            "volumeParameterType" : "aeiou",
            "minimumSize" : 0,
            "maximumNumber" : 5,
            "maximumSize" : 6,
            "minimumNumber" : 1
          } ],
          "properties" : {
            "key" : "aeiou"
          }
        },
        "value" : "aeiou"
      } ]
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

