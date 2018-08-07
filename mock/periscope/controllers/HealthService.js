'use strict';

exports.getPeriscopeHealth = function(args, res, next) {
    /**
     * retrieve Periscope information for admin user
     *
     * returns Health
     **/
    var examples = {};
    examples['application/json'] =
    {
        "status":"UP"
    };
    if (Object.keys(examples).length > 0) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    } else {
        res.end();
    }
}