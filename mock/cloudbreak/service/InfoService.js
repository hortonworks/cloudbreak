'use strict';

exports.getCloudbreakInfo = function() {
    return new Promise(function(resolve, reject) {
        var examples = {};
        examples['application/json'] =
            {
                "app":
                    {
                        "name":"cloudbreak",
                        "version":"MOCK"
                    }
            };
        if (Object.keys(examples).length > 0) {
            resolve(examples[Object.keys(examples)[0]]);
        } else {
            resolve();
        }
    });
}

exports.getCloudbreakHealth = function() {
    /**
     * retrieve Cloudbreak information for admin user
     *
     * returns Health
     **/

    return new Promise(function(resolve, reject) {
        var examples = {};
        examples['application/json'] =
            {
                "status":"UP"
            };
        if (Object.keys(examples).length > 0) {
            resolve(examples[Object.keys(examples)[0]]);
        } else {
            resolve();
        }
    });
}