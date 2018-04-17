'use strict';


/**
 * retrive all available settings
 * Collecting Cloudbreak specific resource settings.
 *
 * returns Map
 **/
exports.getAllSettings = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "key" : {
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
 * retrive available Ambari database settings
 * Collecting Cloudbreak specific resource settings.
 *
 * returns Map
 **/
exports.getDatabaseConfigSettings = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "key" : "{}"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrive available recipe settings
 * Collecting Cloudbreak specific resource settings.
 *
 * returns Map
 **/
exports.getRecipeSettings = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "key" : "{}"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

