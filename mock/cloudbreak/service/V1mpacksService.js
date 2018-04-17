'use strict';


/**
 * delete management pack by id
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteManagementPack = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete private management pack by name
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateManagementPack = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private management pack by name
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicManagementPack = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve management pack by id
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * id Long 
 * returns ManagementPackResponse
 **/
exports.getManagementPack = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a private management pack by name
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * name String 
 * returns ManagementPackResponse
 **/
exports.getPrivateManagementPack = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private management packs
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * returns List
 **/
exports.getPrivateManagementPacks = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
}, {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) management pack by name
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * name String 
 * returns ManagementPackResponse
 **/
exports.getPublicManagementPack = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) management packs
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * returns List
 **/
exports.getPublicManagementPacks = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
}, {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create management pack as private resource
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * body ManagementPackRequest  (optional)
 * returns ManagementPackResponse
 **/
exports.postPrivateManagementPack = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create management pack as public resource
 * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
 *
 * body ManagementPackRequest  (optional)
 * returns ManagementPackResponse
 **/
exports.postPublicManagementPack = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

