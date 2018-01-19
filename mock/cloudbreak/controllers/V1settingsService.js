'use strict';

exports.getAllSettings = function(args, res, next) {
  /**
   * retrive all available settings
   * Collecting Cloudbreak specific resource settings.
   *
   * returns Map
   **/
  var examples = {};
  examples['application/json'] = {
  "key" : {
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

exports.getDatabaseConfigSettings = function(args, res, next) {
  /**
   * retrive available Ambari database settings
   * Collecting Cloudbreak specific resource settings.
   *
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

exports.getRecipeSettings = function(args, res, next) {
  /**
   * retrive available recipe settings
   * Collecting Cloudbreak specific resource settings.
   *
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

