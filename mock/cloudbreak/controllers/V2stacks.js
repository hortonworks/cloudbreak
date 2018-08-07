'use strict';

var utils = require('../utils/writer.js');
var V2stacks = require('../service/V2stacksService');

module.exports.deleteInstanceStackV2 = function deleteInstanceStackV2 (req, res, next) {
  var stackId = req.swagger.params['stackId'].value;
  var instanceId = req.swagger.params['instanceId'].value;
  V2stacks.deleteInstanceStackV2(stackId,instanceId)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateStackV2 = function deletePrivateStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  var forced = req.swagger.params['forced'].value;
  var deleteDependencies = req.swagger.params['deleteDependencies'].value;
  V2stacks.deletePrivateStackV2(name,forced,deleteDependencies)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicStackV2 = function deletePublicStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  var forced = req.swagger.params['forced'].value;
  var deleteDependencies = req.swagger.params['deleteDependencies'].value;
  V2stacks.deletePublicStackV2(name,forced,deleteDependencies)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deleteStackV2 = function deleteStackV2 (req, res, next) {
  var id = req.swagger.params['id'].value;
  var forced = req.swagger.params['forced'].value;
  var deleteDependencies = req.swagger.params['deleteDependencies'].value;
  V2stacks.deleteStackV2(id,forced,deleteDependencies)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getAllStackV2 = function getAllStackV2 (req, res, next) {
  V2stacks.getAllStackV2()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getCertificateStackV2 = function getCertificateStackV2 (req, res, next) {
  var id = req.swagger.params['id'].value;
  V2stacks.getCertificateStackV2(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getClusterRequestFromName = function getClusterRequestFromName (req, res, next) {
  var name = req.swagger.params['name'].value;
  V2stacks.getClusterRequestFromName(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateStackV2 = function getPrivateStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  var entry = req.swagger.params['entry'].value;
  V2stacks.getPrivateStackV2(name,entry)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesStackV2 = function getPrivatesStackV2 (req, res, next) {
  V2stacks.getPrivatesStackV2()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicStackV2 = function getPublicStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  var entry = req.swagger.params['entry'].value;
  V2stacks.getPublicStackV2(name,entry)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsStackV2 = function getPublicsStackV2 (req, res, next) {
  V2stacks.getPublicsStackV2()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getStackForAmbariV2 = function getStackForAmbariV2 (req, res, next) {
  var body = req.swagger.params['body'].value;
  V2stacks.getStackForAmbariV2(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getStackV2 = function getStackV2 (req, res, next) {
  var id = req.swagger.params['id'].value;
  var entry = req.swagger.params['entry'].value;
  V2stacks.getStackV2(id,entry)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPrivateStackV2 = function postPrivateStackV2 (req, res, next) {
  var body = req.swagger.params['body'].value;
  V2stacks.postPrivateStackV2(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicStackV2 = function postPublicStackV2 (req, res, next) {
  var body = req.swagger.params['body'].value;
  V2stacks.postPublicStackV2(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postPublicStackV2ForBlueprint = function postPublicStackV2ForBlueprint (req, res, next) {
  var body = req.swagger.params['body'].value;
  V2stacks.postPublicStackV2ForBlueprint(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putpasswordStackV2 = function putpasswordStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  var body = req.swagger.params['body'].value;
  V2stacks.putpasswordStackV2(name,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
        res.statusCode = 404;
      utils.writeJson(res, response);
    });
};

module.exports.putreinstallStackV2 = function putreinstallStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  var body = req.swagger.params['body'].value;
  V2stacks.putreinstallStackV2(name,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putrepairStackV2 = function putrepairStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  V2stacks.putrepairStackV2(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putscalingStackV2 = function putscalingStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  var body = req.swagger.params['body'].value;
  V2stacks.putscalingStackV2(name,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putstartStackV2 = function putstartStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  V2stacks.putstartStackV2(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putstopStackV2 = function putstopStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  V2stacks.putstopStackV2(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putsyncStackV2 = function putsyncStackV2 (req, res, next) {
  var name = req.swagger.params['name'].value;
  V2stacks.putsyncStackV2(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.statusStackV2 = function statusStackV2 (req, res, next) {
  var id = req.swagger.params['id'].value;
  V2stacks.statusStackV2(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.validateStackV2 = function validateStackV2 (req, res, next) {
  var body = req.swagger.params['body'].value;
  V2stacks.validateStackV2(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.variantsStackV2 = function variantsStackV2 (req, res, next) {
  V2stacks.variantsStackV2()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
