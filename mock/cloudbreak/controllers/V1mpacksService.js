'use strict';

exports.deleteManagementPack = function(args, res, next) {
  /**
   * delete management pack by id
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * id Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePrivateManagementPack = function(args, res, next) {
  /**
   * delete private management pack by name
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicManagementPack = function(args, res, next) {
  /**
   * delete public (owned) or private management pack by name
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getManagementPack = function(args, res, next) {
  /**
   * retrieve management pack by id
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * id Long 
   * returns ManagementPackResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "aeiou",
  "purgeList" : [ "aeiou" ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivateManagementPack = function(args, res, next) {
  /**
   * retrieve a private management pack by name
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * name String 
   * returns ManagementPackResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "aeiou",
  "purgeList" : [ "aeiou" ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivateManagementPacks = function(args, res, next) {
  /**
   * retrieve private management packs
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "aeiou",
  "purgeList" : [ "aeiou" ]
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicManagementPack = function(args, res, next) {
  /**
   * retrieve a public or private (owned) management pack by name
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * name String 
   * returns ManagementPackResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "aeiou",
  "purgeList" : [ "aeiou" ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicManagementPacks = function(args, res, next) {
  /**
   * retrieve public and private (owned) management packs
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "aeiou",
  "purgeList" : [ "aeiou" ]
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateManagementPack = function(args, res, next) {
  /**
   * create management pack as private resource
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * body ManagementPackRequest  (optional)
   * returns ManagementPackResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "aeiou",
  "purgeList" : [ "aeiou" ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicManagementPack = function(args, res, next) {
  /**
   * create management pack as public resource
   * An Apache Ambari Management Pack (Mpack) can bundle multiple service definitions, stack definitions, stack add-on service definitions, view definitions services so that releasing these artifacts don’t enforce an Apache Ambari release.
   *
   * body ManagementPackRequest  (optional)
   * returns ManagementPackResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "public" : false,
  "name" : "aeiou",
  "description" : "aeiou",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "aeiou",
  "purgeList" : [ "aeiou" ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

