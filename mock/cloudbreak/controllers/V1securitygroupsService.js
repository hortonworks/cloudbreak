'use strict';

exports.deletePrivateSecurityGroup = function(args, res, next) {
    /**
     * delete private security group by name
     * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
     *
     * name String
     * no response value expected for this operation
     **/
    res.end();
}

exports.deletePublicSecurityGroup = function(args, res, next) {
    /**
     * delete public (owned) or private security group by name
     * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
     *
     * name String
     * no response value expected for this operation
     **/
    res.end();
}

exports.deleteSecurityGroup = function(args, res, next) {
    /**
     * delete security group by id
     * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
     *
     * id Long
     * no response value expected for this operation
     **/
    res.end();
}

exports.getPrivateSecurityGroup = function(args, res, next) {
    /**
     * retrieve a private security group by name
     * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
     *
     * name String
     * returns SecurityGroupResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "securityGroupId" : "aeiou",
        "owner" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "securityRules" : [ {
            "subnet" : "aeiou",
            "protocol" : "aeiou",
            "id" : 6,
            "ports" : "aeiou",
            "modifiable" : false
        } ],
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 0,
        "account" : "aeiou"
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.getPrivatesSecurityGroup = function(args, res, next) {
    /**
     * retrieve private security groups
     * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
     *
     * returns List
     **/
    var examples = {};
    examples['application/json'] = [ {
        "securityGroupId" : "aeiou",
        "owner" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "securityRules" : [ {
            "subnet" : "aeiou",
            "protocol" : "aeiou",
            "id" : 6,
            "ports" : "aeiou",
            "modifiable" : false
        } ],
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 0,
        "account" : "aeiou"
    } ];
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.getPublicSecurityGroup = function(args, res, next) {
    /**
     * retrieve a public or private (owned) security group by name
     * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
     *
     * name String
     * returns SecurityGroupResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "securityGroupId" : "aeiou",
        "owner" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "securityRules" : [ {
            "subnet" : "aeiou",
            "protocol" : "aeiou",
            "id" : 6,
            "ports" : "aeiou",
            "modifiable" : false
        } ],
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 0,
        "account" : "aeiou"
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.getPublicsSecurityGroup = function(args, res, next) {
  /**
   * retrieve public and private (owned) security groups
   * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] =
  [
    {
      "description":null,
      "securityGroupId":null,
      "cloudPlatform":"OPENSTACK",
      "name":"sgb4c28e84-fa47-4ce9-bf85-f1fabcb9c774",
      "id":1,
      "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "securityRules":
      [
        {
          "subnet":"0.0.0.0/0",
          "ports":"22",
          "protocol":"tcp",
          "modifiable":false,
          "id":1
        }
      ],
      "publicInAccount":false
    },{
      "description":null,
      "securityGroupId":null,
      "cloudPlatform":"OPENSTACK",
      "name":"sgcc9c4d11-0926-4e5e-b6c3-8206fbca2c5a",
      "id":2,
      "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "securityRules":
      [
        {
          "subnet":"0.0.0.0/0",
          "ports":"443",
          "protocol":"tcp",
          "modifiable":false,
          "id":4200
        },{
          "subnet":"0.0.0.0/0",
          "ports":"22",
          "protocol":"tcp",
          "modifiable":false,
          "id":4199
        },{
          "subnet":"0.0.0.0/0",
          "ports":"22",
          "protocol":"tcp",
          "modifiable":false,
          "id":4198
        },{
          "subnet":"0.0.0.0/0",
          "ports":"9443",
          "protocol":"tcp",
          "modifiable":false,
          "id":4197
          }
      ],
      "publicInAccount":false
    },{
      "description":null,
      "securityGroupId":null,
      "cloudPlatform":"AWS",
      "name":"sge019005d-5be5-46aa-b181-4f76ab7677a8",
      "id":3,
      "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "securityRules":
      [
        {
          "subnet":"0.0.0.0/0",
          "ports":"22",
          "protocol":"tcp",
          "modifiable":false,
          "id":4206
        }
      ],
      "publicInAccount":false
    },{
      "description":null,
      "securityGroupId":null,
      "cloudPlatform":"AWS",
      "name":"sg8cb22ed9-d776-4fc1-8b75-65e0fb6d5616",
      "id":4,
      "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "securityRules":
      [
        {
          "subnet":"0.0.0.0/0",
          "ports":"9443",
          "protocol":"tcp",
          "modifiable":false,
          "id":4202
        },{
          "subnet":"0.0.0.0/0",
          "ports":"22",
          "protocol":"tcp",
          "modifiable":false,
          "id":4203
        },{
          "subnet":"0.0.0.0/0",
          "ports":"443",
          "protocol":"tcp",
          "modifiable":false,
          "id":4205
        },{
          "subnet":"0.0.0.0/0",
          "ports":"22",
          "protocol":"tcp",
          "modifiable":false,
          "id":4204
        }
      ],
      "publicInAccount":false
    },{
      "description":null,
      "securityGroupId":null,
      "cloudPlatform":"AZURE",
      "name":"sgebd08127-2a83-4165-b944-c868894fe45a",
      "id":5,
      "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "securityRules":
      [
        {
          "subnet":"0.0.0.0/0",
          "ports":"443",
          "protocol":"tcp",
          "modifiable":false,
          "id":4211
        },{
          "subnet":"0.0.0.0/0",
          "ports":"22",
          "protocol":"tcp",
          "modifiable":false,
          "id":4210
        },{
          "subnet":"0.0.0.0/0",
          "ports":"9443",
          "protocol":"tcp",
          "modifiable":false,
          "id":4208
        },{
          "subnet":"0.0.0.0/0",
          "ports":"22",
          "protocol":"tcp",
          "modifiable":false,
          "id":4209
        }
      ],
      "publicInAccount":false
    },{
      "description":null,
      "securityGroupId":null,
      "cloudPlatform":"AZURE",
      "name":"sg17b54a58-abb4-4eaa-9558-4d9596a28ce1",
      "id":6,
      "owner":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "account":"a0b9ae1b-8dc5-45ad-bada-567170cac68f",
      "securityRules":
      [
        {
          "subnet":"0.0.0.0/0",
          "ports":"22",
          "protocol":"tcp",
          "modifiable":false,
          "id":4212
        }
      ],
      "publicInAccount":false
    }, {
      "description": null,
      "securityGroupId": null,
      "cloudPlatform": "GCP",
      "name": "sg23b6d523-fe70-48cb-a42d-5348f550e456",
      "id": 7,
      "owner": "8781acdb-4d87-4dff-814c-35c191031ad3",
      "account": "8781acdb-4d87-4dff-814c-35c191031ad3",
      "securityRules":
      [
        {
            "subnet": "0.0.0.0/0",
            "ports": "22",
            "protocol": "tcp",
            "modifiable": false,
            "id": 4808
        },
        {
            "subnet": "0.0.0.0/0",
            "ports": "22",
            "protocol": "tcp",
            "modifiable": false,
            "id": 4807
        },
        {
            "subnet": "0.0.0.0/0",
            "ports": "443",
            "protocol": "tcp",
            "modifiable": false,
            "id": 4809
        },
        {
            "subnet": "0.0.0.0/0",
            "ports": "9443",
            "protocol": "tcp",
            "modifiable": false,
            "id": 4806
        }
      ],
      "publicInAccount": false
  },{
    "description": null,
    "securityGroupId": null,
    "cloudPlatform": "GCP",
    "name": "sgfa2d1b3a-05e2-4e7c-aa66-f8160fa3471d",
    "id": 8,
    "owner": "8781acdb-4d87-4dff-814c-35c191031ad3",
    "account": "8781acdb-4d87-4dff-814c-35c191031ad3",
    "securityRules":
    [
      {
        "subnet": "0.0.0.0/0",
        "ports": "22",
        "protocol": "tcp",
        "modifiable": false,
        "id": 4810
      }
    ],
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

exports.getSecurityGroup = function(args, res, next) {
    /**
     * retrieve security group by id
     * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
     *
     * id Long
     * returns SecurityGroupResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "securityGroupId" : "aeiou",
        "owner" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "securityRules" : [ {
            "subnet" : "aeiou",
            "protocol" : "aeiou",
            "id" : 6,
            "ports" : "aeiou",
            "modifiable" : false
        } ],
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 0,
        "account" : "aeiou"
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.postPrivateSecurityGroup = function(args, res, next) {
    /**
     * create security group as private resource
     * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
     *
     * body SecurityGroupRequest  (optional)
     * returns SecurityGroupResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "securityGroupId" : "aeiou",
        "owner" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "securityRules" : [ {
            "subnet" : "aeiou",
            "protocol" : "aeiou",
            "id" : 6,
            "ports" : "aeiou",
            "modifiable" : false
        } ],
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 0,
        "account" : "aeiou"
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.postPublicSecurityGroup = function(args, res, next) {
    /**
     * create security group as public resource
     * Different inbound security rules(group) could be configured by using SecurityGroup resources and a group could be assigned to any Stack(cluster).
     *
     * body SecurityGroupRequest  (optional)
     * returns SecurityGroupResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "securityGroupId" : "aeiou",
        "owner" : "aeiou",
        "cloudPlatform" : "aeiou",
        "publicInAccount" : false,
        "securityRules" : [ {
            "subnet" : "aeiou",
            "protocol" : "aeiou",
            "id" : 6,
            "ports" : "aeiou",
            "modifiable" : false
        } ],
        "name" : "aeiou",
        "description" : "aeiou",
        "id" : 0,
        "account" : "aeiou"
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

