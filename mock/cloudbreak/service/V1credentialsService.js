'use strict';


/**
 * delete credential by id
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteCredential = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete private credential by name
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateCredential = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private credential by name
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicCredential = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve credential by id
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * id Long 
 * returns CredentialResponse
 **/
exports.getCredential = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/credentials/openstack.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a private credential by name
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * name String 
 * returns CredentialResponse
 **/
exports.getPrivateCredential = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/credentials/openstack.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private credentials
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * returns List
 **/
exports.getPrivatesCredential = function() {
  return new Promise(function(resolve, reject) {
    var openstack_data = require('../responses/credentials/openstack.json');
    var aws_data = require('../responses/credentials/aws.json');
    var azure_data = require('../responses/credentials/azure.json');
    var gcp_data = require('../responses/credentials/gcp.json');
    var response_array = [];

    response_array.push(openstack_data,aws_data,azure_data,gcp_data);
    var examples = {};
    examples['application/json'] = response_array;
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) credential by name
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * name String 
 * returns CredentialResponse
 **/
exports.getPublicCredential = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/credentials/openstack.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) credentials
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * returns List
 **/
exports.getPublicsCredential = function() {
  return new Promise(function(resolve, reject) {
    var openstack_data = require('../responses/credentials/openstack.json');
    var aws_data = require('../responses/credentials/aws.json');
    var azure_data = require('../responses/credentials/azure.json');
    var gcp_data = require('../responses/credentials/gcp.json');
    var response_array = [];

    response_array.push(openstack_data,aws_data,azure_data,gcp_data);
    var examples = {};
    examples['application/json'] = response_array;
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create credential as private resource
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * body CredentialRequest  (optional)
 * returns CredentialResponse
 **/
exports.postPrivateCredential = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cloudPlatform" : "cloudPlatform",
  "public" : false,
  "name" : "name",
  "topologyId" : 0,
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
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
 * create credential as public resource
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * body CredentialRequest  (optional)
 * returns CredentialResponse
 **/
exports.postPublicCredential = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cloudPlatform" : "cloudPlatform",
  "public" : false,
  "name" : "name",
  "topologyId" : 0,
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
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
 * interactive login
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * body CredentialRequest  (optional)
 * returns Map
 **/
exports.privateInteractiveLoginCredential = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "key" : ""
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * interactive login
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * body CredentialRequest  (optional)
 * returns Map
 **/
exports.publicInteractiveLoginCredential = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "key" : ""
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * modify private credential resource
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * body CredentialRequest  (optional)
 * returns CredentialResponse
 **/
exports.putPrivateCredential = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cloudPlatform" : "cloudPlatform",
  "public" : false,
  "name" : "name",
  "topologyId" : 0,
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
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
 * modify public credential resource
 * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
 *
 * body CredentialRequest  (optional)
 * returns CredentialResponse
 **/
exports.putPublicCredential = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "cloudPlatform" : "cloudPlatform",
  "public" : false,
  "name" : "name",
  "topologyId" : 0,
  "description" : "description",
  "id" : 6,
  "parameters" : {
    "key" : "{}"
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

