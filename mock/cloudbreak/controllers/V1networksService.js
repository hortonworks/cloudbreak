'use strict';

exports.deleteNetwork = function(args, res, next) {
    /**
     * delete network by id
     * Provider specific network settings could be configured by using Network resources.
     *
     * id Long
     * no response value expected for this operation
     **/
    res.end();
}

exports.deletePrivateNetwork = function(args, res, next) {
    /**
     * delete private network by name
     * Provider specific network settings could be configured by using Network resources.
     *
     * name String
     * no response value expected for this operation
     **/
    res.end();
}

exports.deletePublicNetwork = function(args, res, next) {
    /**
     * delete public (owned) or private network by name
     * Provider specific network settings could be configured by using Network resources.
     *
     * name String
     * no response value expected for this operation
     **/
    res.end();
}

exports.getNetwork = function(args, res, next) {
    /**
     * retrieve network by id
     * Provider specific network settings could be configured by using Network resources.
     *
     * id Long
     * returns NetworkResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "subnetCIDR" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "topologyId" : 0,
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 6,
        "parameters" : {
            "key" : "{}"
        }
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.getPrivateNetwork = function(args, res, next) {
    /**
     * retrieve a private network by name
     * Provider specific network settings could be configured by using Network resources.
     *
     * name String
     * returns NetworkResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "subnetCIDR" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "topologyId" : 0,
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 6,
        "parameters" : {
            "key" : "{}"
        }
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.getPrivatesNetwork = function(args, res, next) {
    /**
     * retrieve private networks
     * Provider specific network settings could be configured by using Network resources.
     *
     * returns List
     **/
    var examples = {};
    examples['application/json'] = [ {
        "subnetCIDR" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "topologyId" : 0,
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 6,
        "parameters" : {
            "key" : "{}"
        }
    } ];
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.getPublicNetwork = function(args, res, next) {
    /**
     * retrieve a public or private (owned) network by name
     * Provider specific network settings could be configured by using Network resources.
     *
     * name String
     * returns NetworkResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "subnetCIDR" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "topologyId" : 0,
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 6,
        "parameters" : {
            "key" : "{}"
        }
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.getPublicsNetwork = function(args, res, next) {
  /**
   * retrieve public and private (owned) networks
   * Provider specific network settings could be configured by using Network resources.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] =
  [
    {
      "description":null,
      "subnetCIDR":null,
      "cloudPlatform":"OPENSTACK",
      "parameters":
      {
        "publicNetId":null,
        "routerId":null,
        "internetGatewayId":null,
        "subnetId":"0404bf21-db5f-4987-8576-e65a4a99b14e",
        "networkingOption":"self-service",
        "networkId":"a5ad7a1d-d3a6-4180-8d61-07a23f6fb449"
      },
      "topologyId":null,
      "name":"ncc1f02de-0ff6-404b-ad4e-526cfb620d87",
      "id":1,
      "publicInAccount":false
    },{
      "description":null,
      "subnetCIDR":"10.0.0.0/16",
      "cloudPlatform":"AWS",
      "parameters":
      {
        "publicNetId":null,
        "routerId":null,
        "internetGatewayId":null,
        "networkId":null,
        "vpcId":null
      },
      "topologyId":null,
      "name":"n7c98594f-1afa-4e79-8c77-7697a7d4346f",
      "id":2,
      "publicInAccount":false
    },{
      "description":null,
      "subnetCIDR":"10.0.0.0/16",
      "cloudPlatform":"AZURE",
      "parameters":
      {
        "publicNetId":null,
        "routerId":null,
        "internetGatewayId":null,
        "noPublicIp":false,
        "noFirewallRules":false,
        "networkId":null,
        "vpcId":null
      },
      "topologyId":null,
      "name":"nceddf172-7b62-41cc-b9ef-e6bdf15b08ea",
      "id":3,
      "publicInAccount":false
    },{
      "description": null,
      "subnetCIDR": "10.0.0.0/16",
      "cloudPlatform": "GCP",
      "parameters": {
          "subnetId": "gcpcluster-20180119120325",
          "publicNetId": null,
          "routerId": null,
          "internetGatewayId": null,
          "vpcId": null,
          "noFirewallRules": false,
          "networkId": "gcpcluster-20180119120314",
          "noPublicIp": false
      },
      "topologyId": null,
      "name": "n5001012c-3a70-4c3e-b944-2f8703354dcc",
      "id": 4,
      "publicInAccount": false
    }
  ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateNetwork = function(args, res, next) {
    /**
     * create network as private resource
     * Provider specific network settings could be configured by using Network resources.
     *
     * body NetworkRequest  (optional)
     * returns NetworkResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "subnetCIDR" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "topologyId" : 0,
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 6,
        "parameters" : {
            "key" : "{}"
        }
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.postPublicNetwork = function(args, res, next) {
    /**
     * create network as public resource
     * Provider specific network settings could be configured by using Network resources.
     *
     * body NetworkRequest  (optional)
     * returns NetworkResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "subnetCIDR" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "topologyId" : 0,
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 6,
        "parameters" : {
            "key" : "{}"
        }
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

