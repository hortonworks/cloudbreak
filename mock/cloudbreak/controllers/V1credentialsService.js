'use strict';

exports.deleteCredential = function(args, res, next) {
    /**
     * delete credential by id
     * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
     *
     * id Long
     * no response value expected for this operation
     **/
    res.end();
}

exports.deletePrivateCredential = function(args, res, next) {
    /**
     * delete private credential by name
     * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
     *
     * name String
     * no response value expected for this operation
     **/
    res.end();
}

exports.deletePublicCredential = function(args, res, next) {
    /**
     * delete public (owned) or private credential by name
     * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
     *
     * name String
     * no response value expected for this operation
     **/
    res.end();
}

exports.getCredential = function(args, res, next) {
  /**
   * retrieve credential by id
   * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
   *
   * id Long
   * returns CredentialResponse
   **/
  var examples = {};
  examples['application/json'] = {
      "name":"openstack",
      "cloudPlatform":"OPENSTACK",
      "parameters":
      {
          "facing":"internal",
          "endpoint":"http://openstack.eng.com:3000/v2.0",
          "selector":"cb-keystone-v2",
          "keystoneVersion":"cb-keystone-v2",
          "userName":"cloudbreak",
          "tenantName":"cloudbreak"
      },
      "description":"",
      "topologyId":null,
      "id":1,
      "public":false
  };
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getCredentialRequestFromName = function(args, res, next) {
    /**
     * retrieve credential request by credential name
     * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
     *
     * name String
     * returns CredentialRequest
     **/
    var examples = {};
    examples['application/json'] = {
        "cloudPlatform" : "aeiou",
        "name" : args.name.value,
        "topologyId" : 0,
        "description" : "aeiou",
        "parameters" : {
            "key" : "{}"
        }
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        switch(args.name.value) {
            case 'amazon':
                var responseJson = {
                    "name": args.name.value,
                    "cloudPlatform": "AWS",
                    "parameters": {
                        "roleArn": "arn:aws:iam::755047402263:role/aws-cluster-3938-S3AccessRole-1WONDIIEGB2GR",
                        "selector": "role-based"
                    }
                };
                res.end(JSON.stringify(responseJson));
                break;
            case 'openstack':
                var responseJson = {
                    "name": args.name.value,
                    "cloudPlatform": "OPENSTACK",
                    "parameters": {
                        "password": "GezcN4Mcj4f0Yfnw/Sz0UCHtCql3YERk",
                        "endpoint": "http://openstack.eng.com:3000/v2.0",
                        "keystoneVersion": "cb-keystone-v2",
                        "tenantName": "cloudbreak",
                        "facing": "internal",
                        "selector": "cb-keystone-v2",
                        "userName": "HIpti2qS7tkVD67MNqmwB2RzV3+vEAvn"
                    },
                    "description": ""
                };
                res.end(JSON.stringify(responseJson));
                break;
            case 'azure':
                var responseJson = {
                    "name": args.name.value,
                    "cloudPlatform": "AZURE",
                    "parameters": {
                        "accessKey": "a12b1234-1234-12aa-3bcc-4d5e6f78900g",
                        "tenantId": "a12b1234-1234-12aa-3bcc-4d5e6f78900g",
                        "selector": "app-based",
                        "subscriptionId": "a12b1234-1234-12aa-3bcc-4d5e6f78900g"
                    }
                };
                res.end(JSON.stringify(responseJson));
                break;
            case 'google':
                var responseJson = {
                    "name": args.name.value,
                    "cloudPlatform": "GCP",
                    "parameters": {
                        "serviceAccountId": "1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com",
                        "projectId": "cloudbreak",
                        "serviceAccountPrivateKey": "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"
                    },
                    "description": ""
                };
                res.end(JSON.stringify(responseJson));
                break;
            default:
                res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
        }
    } else {
      res.end();
    }
}

exports.getPrivateCredential = function(args, res, next) {
  /**
   * retrieve a private credential by name
   * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
   *
   * name String
   * returns CredentialResponse
   **/
  var examples = {};
  examples['application/json'] = {
      "name":"openstack",
      "cloudPlatform":"OPENSTACK",
      "parameters":
      {
          "facing":"internal",
          "endpoint":"http://openstack.eng.com:3000/v2.0",
          "selector":"cb-keystone-v2",
          "keystoneVersion":"cb-keystone-v2",
          "userName":"cloudbreak",
          "tenantName":"cloudbreak"
      },
      "description":"",
      "topologyId":null,
      "id":1,
      "public":false
  };
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesCredential = function(args, res, next) {
  /**
   * retrieve private credentials
   * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] =
  [
      {
        "name":"openstack",
        "cloudPlatform":"OPENSTACK",
        "parameters":
        {
          "facing":"internal",
          "endpoint":"http://openstack.eng.com:3000/v2.0",
          "selector":"cb-keystone-v2",
          "keystoneVersion":"cb-keystone-v2",
          "userName":"cloudbreak",
          "tenantName":"cloudbreak"
        },
        "description":"",
        "topologyId":null,
        "id":1,
        "public":false
      },{
        "name":"azure",
        "cloudPlatform":"AZURE",
        "parameters":
        {
          "tenantId":"a12b1234-1234-12aa-3bcc-4d5e6f78900g",
          "spDisplayName":null,
          "subscriptionId":"a12b1234-1234-12aa-3bcc-4d5e6f78900g",
          "roleType":null,
          "accessKey":"a12b1234-1234-12aa-3bcc-4d5e6f78900g"
        },
        "description":"",
        "topologyId":null,
        "id":2,
        "public":false
      },{
        "name":"google",
        "cloudPlatform":"GCP",
        "parameters":
        {
          "serviceAccountId":"1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com",
          "projectId":"cloudbreak"
        },
        "description":"",
        "topologyId":null,
        "id":3,
        "public":false
      },{
        "name":"amazon",
        "cloudPlatform":"AWS",
        "parameters":
        {
          "smartSenseId":"null",
          "selector":"role-based"
        },
        "description":"",
        "topologyId":null,
        "id":4,
        "public":false
      }
  ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicCredential = function(args, res, next) {
  /**
   * retrieve a public or private (owned) credential by name
   * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
   *
   * name String
   * returns CredentialResponse
   **/
  var examples = {};
  examples['application/json'] = {
      "name":"openstack",
      "cloudPlatform":"OPENSTACK",
      "parameters":
      {
          "facing":"internal",
          "endpoint":"http://openstack.eng.com:3000/v2.0",
          "selector":"cb-keystone-v2",
          "keystoneVersion":"cb-keystone-v2",
          "userName":"cloudbreak",
          "tenantName":"cloudbreak"
      },
      "description":"",
      "topologyId":null,
      "id":1,
      "public":false
  };
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsCredential = function(args, res, next) {
  /**
   * retrieve public and private (owned) credentials
   * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] =
  [
    {
      "name":"openstack",
      "cloudPlatform":"OPENSTACK",
      "parameters":
      {
        "facing":"internal",
        "endpoint":"http://openstack.eng.com:3000/v2.0",
        "selector":"cb-keystone-v2",
        "keystoneVersion":"cb-keystone-v2",
        "userName":"cloudbreak",
        "tenantName":"cloudbreak"
      },
      "description":"",
      "topologyId":null,
      "id":1,
      "public":false
    },{
      "name":"azure",
      "cloudPlatform":"AZURE",
      "parameters":
      {
        "tenantId":"a12b1234-1234-12aa-3bcc-4d5e6f78900g",
        "spDisplayName":null,
        "subscriptionId":"a12b1234-1234-12aa-3bcc-4d5e6f78900g",
        "roleType":null,
        "accessKey":"a12b1234-1234-12aa-3bcc-4d5e6f78900g"
      },
      "description":"",
      "topologyId":null,
      "id":2,
      "public":false
    },{
      "name":"google",
      "cloudPlatform":"GCP",
      "parameters":
      {
        "serviceAccountId":"1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com",
        "projectId":"cloudbreak"
      },
      "description":"",
      "topologyId":null,
      "id":3,
      "public":false
    },{
      "name":"amazon",
      "cloudPlatform":"AWS",
      "parameters":
      {
        "smartSenseId":"null",
        "selector":"role-based"
      },
      "description":"",
      "topologyId":null,
      "id":4,
      "public":false
    }
  ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateCredential = function(args, res, next) {
    /**
     * create credential as private resource
     * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
     *
     * body CredentialRequest  (optional)
     * returns CredentialResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "cloudPlatform" : "aeiou",
        "public" : false,
        "name" : "aeiou",
        "topologyId" : 0,
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

exports.postPublicCredential = function(args, res, next) {
    /**
     * create credential as public resource
     * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
     *
     * body CredentialRequest  (optional)
     * returns CredentialResponse
     **/
    var examples = {};
    examples['application/json'] = {
        "cloudPlatform" : "aeiou",
        "public" : false,
        "name" : "aeiou",
        "topologyId" : 0,
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

exports.privateInteractiveLoginCredential = function(args, res, next) {
    /**
     * interactive login
     * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
     *
     * body CredentialRequest  (optional)
     * returns Map
     **/
    var examples = {};
    examples['application/json'] = {
        "key" : "aeiou"
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

exports.publicInteractiveLoginCredential = function(args, res, next) {
    /**
     * interactive login
     * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
     *
     * body CredentialRequest  (optional)
     * returns Map
     **/
    var examples = {};
    examples['application/json'] = {
        "key" : "aeiou"
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}

