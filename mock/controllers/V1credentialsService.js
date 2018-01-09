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

exports.getPrivatesCredential = function(args, res, next) {
  /**
   * retrieve private credentials
   * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "cloudPlatform" : "aeiou",
  "public" : false,
  "name" : "aeiou",
  "topologyId" : 0,
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

