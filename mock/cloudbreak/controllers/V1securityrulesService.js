'use strict';

exports.getDefaultSecurityRules = function(args, res, next) {
  /**
   * get default security rules
   * Security Rules operations
   *
   * returns SecurityRulesResponse
   **/
  var examples = {};
  examples['application/json'] =
  {
   "core":[

   ],
   "gateway":[
      {
         "subnet":"0.0.0.0/0",
         "ports":"22",
         "protocol":"tcp",
         "modifiable":false
      },
      {
         "subnet":"0.0.0.0/0",
         "ports":"9443",
         "protocol":"tcp",
         "modifiable":false
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

