'use strict';

var utils = require('../utils/writer.js');
var Info = require('../service/InfoService');

module.exports.getCloudbreakInfo = function getCloudbreakInfo (req, res, next) {
    Info.getCloudbreakInfo()
        .then(function (response) {
            utils.writeJson(res, response);
        })
        .catch(function (response) {
            utils.writeJson(res, response);
        });
};

module.exports.getCloudbreakHealth = function getCloudbreakHealth (req, res, next) {
    Info.getCloudbreakHealth()
        .then(function (response) {
            utils.writeJson(res, response);
        })
        .catch(function (response) {
            utils.writeJson(res, response);
        });
};


