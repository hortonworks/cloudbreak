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
  examples['application/json'] =
	{
	"recommendations":{

	},
	"virtualMachines":[
	  {
	     "value":"hcube.small",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"2048",
	           "Cpu":"1"
	        }
	     }
	  },
	  {
	     "value":"re.etcd",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"8192",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"hwqe.logsearch",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"65536",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"re.medium",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"8192",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"m2.xlarge",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"m1.medium",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"4096",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"windows.normal",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"2048",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"m1.xxlarge",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"32768",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"hcube.medium",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"8192",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"re.nexus2",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"4"
	        }
	     }
	  },
	  {
	     "value":"hwqe.xxlarge",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"65536",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"re-jenkins-master-2",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"32768",
	           "Cpu":"16"
	        }
	     }
	  },
	  {
	     "value":"hw.perf",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"m1.smaller",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"1024",
	           "Cpu":"1"
	        }
	     }
	  },
	  {
	     "value":"m1.xlarge",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"cloudbreak",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"8192",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"windows-ad",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"4"
	        }
	     }
	  },
	  {
	     "value":"hive.ptest.large",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"4"
	        }
	     }
	  },
	  {
	     "value":"m1.large",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"8192",
	           "Cpu":"4"
	        }
	     }
	  },
	  {
	     "value":"re.protex",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"65536",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"m1.small",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"2048",
	           "Cpu":"1"
	        }
	     }
	  },
	  {
	     "value":"hive.ptest.xlarge",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"24764",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"hive.ptest.medium",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"8192",
	           "Cpu":"4"
	        }
	     }
	  },
	  {
	     "value":"re.jenkins.slave2",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"4"
	        }
	     }
	  },
	  {
	     "value":"m1.tiny",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"512",
	           "Cpu":"1"
	        }
	     }
	  },
	  {
	     "value":"cloudbreak.large",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"solr.cloud",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"32768",
	           "Cpu":"4"
	        }
	     }
	  },
	  {
	     "value":"jpf.test",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"4096",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"testing",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"10",
	           "Cpu":"1"
	        }
	     }
	  },
	  {
	     "value":"re.jenkins.master",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"hive.ptest.x2large",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"32768",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"re.protex2",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"65536",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"hwqe.xlarge.sles",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"hwqe.large.sles",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"nifi.qe.infra",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"32768",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"oracle-12c",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"65536",
	           "Cpu":"16"
	        }
	     }
	  },
	  {
	     "value":"re.jenkins.slave",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"4"
	        }
	     }
	  },
	  {
	     "value":"re.hive.ptest",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"48000",
	           "Cpu":"16"
	        }
	     }
	  },
	  {
	     "value":"hp.code",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"65536",
	           "Cpu":"8"
	        }
	     }
	  },
	  {
	     "value":"m1.micro",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"64",
	           "Cpu":"64"
	        }
	     }
	  },
	  {
	     "value":"hwqe.medium",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"8192",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"hwqe.xlarge",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"hwqe.slave",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"4012",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"hwqe.large",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"20480",
	           "Cpu":"2"
	        }
	     }
	  },
	  {
	     "value":"re.nexus",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"4"
	        }
	     }
	  },
	  {
	     "value":"re.xlarge",
	     "vmTypeMetaJson":{
	        "configs":[
	           {
	              "volumeParameterType":"MAGNETIC",
	              "minimumSize":10,
	              "maximumSize":1023,
	              "minimumNumber":0,
	              "maximumNumber":100
	           }
	        ],
	        "properties":{
	           "Memory":"16384",
	           "Cpu":"4"
	        }
	     }
	  }
	],
	"diskResponses":[
	  {
	     "type":"MAGNETIC",
	     "name":"HDD",
	     "displayName":"HDD"
	  }
	]
	};
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
  examples['application/json'] = {
  "diskTypes":
    { "AZURE":
        [ "Standard_LRS",
          "Standard_GRS",
          "Premium_LRS"],
      "OPENSTACK":
        [ "HDD" ],
      "GCP":
        [ "pd-ssd",
          "pd-standard" ],
      "AWS":
        [ "standard",
          "ephemeral",
          "gp2",
          "st1" ],
      "YARN":
        [],
      "MOCK":
        [ "magnetic",
          "ssd",
          "ephemeral" ]
    },
  "defaultDisks":
    { "AZURE":"Standard_LRS",
      "OPENSTACK":"HDD",
      "GCP":"pd-standard",
      "AWS":"standard",
      "YARN":"",
      "MOCK":"magnetic" },
  "diskMappings":
    { "AZURE":
        { "Standard_LRS":"MAGNETIC",
          "Premium_LRS":"MAGNETIC",
          "Standard_GRS":"MAGNETIC" },
      "OPENSTACK":
        { "HDD":"MAGNETIC"  },
      "GCP":
        { "pd-standard":"MAGNETIC",
          "pd-ssd":"SSD"  },
      "AWS":
        { "standard":"MAGNETIC",
          "st1":"ST1",
          "gp2":"SSD",
          "ephemeral":"EPHEMERAL" },
      "YARN":
        {},
      "MOCK":
        { "ssd":"SSD",
          "magnetic":"MAGNETIC",
          "ephemeral":"EPHEMERAL" }
    },
  "displayNames":
    { "AZURE":
        { "Standard_LRS":"Locally-redundant storage",
          "Premium_LRS":"Premium locally-redundant storage",
          "Standard_GRS":"Geo-redundant storage"  },
      "OPENSTACK":
        { "HDD":"HDD" },
      "GCP":
        { "pd-standard":"Standard persistent disks (HDD)",
          "pd-ssd":"Solid-state persistent disks (SSD)" },
      "AWS":
        { "standard":"Magnetic","st1":"Throughput Optimized HDD",
          "gp2":"General Purpose (SSD)",
          "ephemeral":"Ephemeral" },
      "YARN":{},
      "MOCK":{}
  }
};
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
  examples['application/json'] =
  {
  	"gateways":
  	{
  		"hw-re":[],
  		"LRI":[],
  		"nova":[]
  	}
  };
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
  examples['application/json'] =
  {
  	"ippools":
  	{
  		"hw-re":[],
  		"LRI":[],
  		"nova":[]
  	}
  };
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
   * returns Map
   **/
  var examples = {};
  examples['application/json'] =
  {
  	"RegionOne":
  	[
      {
         "name":"PROVIDER_NET_172.22.64.0/18",
         "id":"a5ad7a1d-d3a6-4180-8d61-07a23f6fb449",
         "subnets":{
            "0404bf21-db5f-4987-8576-e65a4a99b14e":"PROVIDER_SUBNET_172.22.64.0/18"
         },
         "properties":{
            "providerPhyNet":null,
            "providerSegID":null,
            "tenantId":"e68fc02fc33c497385a96b3d8b9e8b16",
            "networkType":null
         }
      }
    ]
  };
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
   * returns Map
   **/
  var examples = {};
  examples['application/json'] =
  {
    "RegionOne":
    [
      { "name":"consul-keypair",
        "properties":
        { "createdAt":null,
          "fingerprint":"b9:2e:e8:ab:e6:47:4f:03:1c:af:a5:13:bf:4d:5b:7c",
          "id":null,
          "publicKey":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
        }
      },
      { "name":"seq-master",
        "properties":
        { "createdAt":null,
          "fingerprint":"b9:2e:e8:ab:e6:47:4f:03:1c:af:a5:13:bf:4d:5b:7c",
          "id":null,
          "publicKey":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
        }
      },
      {
        "name":"dominika-kp",
        "properties":
        { "createdAt":null,
          "fingerprint":"99:8c:ab:54:0c:a9:c8:2b:10:0e:ce:a6:c1:52:7b:d9",
          "id":null,
          "publicKey":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCS0iTjaiF5YeEaoMVh37I64zKsekW6pQR/Fc5onHfA6AVce5YpT2T63NlWUApTnzlqKFpMdlpL7uvnebvpS3iz/qTcbwtlItGgHmAZCbGK1uYggn5cCt//Gk2Ytvz6ip6B2GiGjCukSQosQfhhLXjZjI7x9sFm2oU5BVbjm1UiePtNn9IHfgXoGiEP9a0tNtzGte+XGD+1FyMs6TGLmHivWAQLXlFVtXZGE19ag0uO/0rJ+D2s+5lRnrOmcy2k5E55LvMqZRNAtdZS/ctD3v/pkPOSeQy9N3RU65RCJhYNQWt2zSpVIG699y5Ks3uVLUAnxVIHOzFXG2Z45J/6w6rR"
        }
      },
      {
        "name":"gabo-consul-keypair",
        "properties":
        { "createdAt":null,
          "fingerprint":"b9:2e:e8:ab:e6:47:4f:03:1c:af:a5:13:bf:4d:5b:7c",
          "id":null,
          "publicKey":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
        }
      },
      {
        "name":"gabo",
        "properties":
        { "createdAt":null,
          "fingerprint":"88:6b:29:2b:ea:3c:83:d3:e0:72:ec:b9:75:c7:e1:4a",
          "id":null,
          "publicKey":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDG769Bxc1rfGJrIBowJLOJlXnBHMPTZBQ2dz6jua69O9iQzDKwDc6w2U15WdNWd3GELOZzNLeUEDT0IxbC34+IUIxaPj4djhrFIRldShn5KorvjMxm0Tyi7g2B+qv/Av/fExYxkkzhDHJxGi2AcBG+jY82Wgtu0gxlODhQNtax0e44W10Jmzh1GcpHh2v3Hk6LIO159teM3Rt3f64D7QGRtNzT9NDv34UGGkEnZG5yP8N0wffGd440L1d/kBZRKhDOfDCGjHSK4rVkLYa9+Td2PU2064W1589A9UQH+bwS82oD7xnoPk1tv+O7lFOAmCe80trc7o/ECBxCtQoGqsnJ2OqUn3FPsq+nxUDR/uqbA4oDskF1enOCqzkruMPGp0oABZeV+p9BHPKHRWwO2oysYRaAhx+RwpD2Fi94dCSkgDNTUpsXA+oCsVmceJ8XP1g6cYKploaQSMUUFm7cvQQOdEVqDXwt99hIwtiX+d1/rZRJAweoAB1ac8qAwELDOboKg9YhWHfoRzx63YjiEReE2IgeElr+d/3Y6YgNgU0GhLXQL10dkRvjoMrbpWrjUvQZwuat4qietCemsqrOabtNMWB3JntLcPqEdCNOkf2t5vESv7lhSbOHMsVsskVLjSZqvW97r0iex4+I7de914cBWMoXq/iW1Nz+/HTZHYemEw== gkozma@hortonworks.com"
        }
      }
    ]
  };
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
     * returns Map
     **/
    var examples = {};
    examples['application/json'] = {
        "key" : [ {
            "groupName" : "aeiou",
            "groupId" : "aeiou",
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
  examples['application/json'] =
  {
    "regions":
    [
      "RegionOne"
    ],
    "displayNames":
    {
      "RegionOne":"RegionOne"
    },
    "availabilityZones":
    {
      "RegionOne":
      [
        "nova","hw-re","LRI"
      ]
    },
    "defaultRegion":"RegionOne"
  };
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

exports.getVmTypes = function(args, res, next) {
    /**
     * retrive available vm types
     * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
     *
     * extended Boolean  (optional)
     * returns PlatformVirtualMachinesJson
     **/
    var examples = {};
    examples['application/json'] = {
        "defaultVmTypePerZones" : {
            "key" : {
                "key" : "aeiou"
            }
        },
        "defaultVirtualMachines" : {
            "key" : "aeiou"
        },
        "virtualMachines" : {
            "key" : [ {
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
        },
        "vmTypesPerZones" : {
            "key" : {
                "key" : [ "" ]
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

exports.getVmTypesByType = function(args, res, next) {
    /**
     * retrive available vm types
     * Each cloud provider has it's own specific resources like instance types and disk types. These endpoints are collecting them.
     *
     * type String
     * extended Boolean  (optional)
     * returns PlatformVirtualMachinesJson
     **/
    var examples = {};
    examples['application/json'] = {
        "defaultVmTypePerZones" : {
            "key" : {
                "key" : "aeiou"
            }
        },
        "defaultVirtualMachines" : {
            "key" : "aeiou"
        },
        "virtualMachines" : {
            "key" : [ {
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
        },
        "vmTypesPerZones" : {
            "key" : {
                "key" : [ "" ]
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

