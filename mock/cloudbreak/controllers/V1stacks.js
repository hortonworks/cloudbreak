'use strict';

var utils = require('../utils/writer.js');
var V1stacks = require('../service/V1stacksService');

module.exports.deleteCluster = function deleteCluster (req, res, next) {
  var id = req.swagger.params['id'].value;
  var withStackDelete = req.swagger.params['withStackDelete'].value;
  var deleteDependencies = req.swagger.params['deleteDependencies'].value;
  V1stacks.deleteCluster(id,withStackDelete,deleteDependencies)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deleteInstanceStack = function deleteInstanceStack (req, res, next) {
  var stackId = req.swagger.params['stackId'].value;
  var instanceId = req.swagger.params['instanceId'].value;
  V1stacks.deleteInstanceStack(stackId,instanceId)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePrivateStack = function deletePrivateStack (req, res, next) {
  var name = req.swagger.params['name'].value;
  var forced = req.swagger.params['forced'].value;
  var deleteDependencies = req.swagger.params['deleteDependencies'].value;
  V1stacks.deletePrivateStack(name,forced,deleteDependencies)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deletePublicStack = function deletePublicStack (req, res, next) {
  var name = req.swagger.params['name'].value;
  var forced = req.swagger.params['forced'].value;
  var deleteDependencies = req.swagger.params['deleteDependencies'].value;
  V1stacks.deletePublicStack(name,forced,deleteDependencies)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.deleteStack = function deleteStack (req, res, next) {
  var id = req.swagger.params['id'].value;
  var forced = req.swagger.params['forced'].value;
  var deleteDependencies = req.swagger.params['deleteDependencies'].value;
  V1stacks.deleteStack(id,forced,deleteDependencies)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.failureReportCluster = function failureReportCluster (req, res, next) {
  var id = req.swagger.params['id'].value;
  var body = req.swagger.params['body'].value;
  V1stacks.failureReportCluster(id,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getAllStack = function getAllStack (req, res, next) {
  V1stacks.getAllStack()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getCertificateStack = function getCertificateStack (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1stacks.getCertificateStack(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getCluster = function getCluster (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1stacks.getCluster(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getConfigsCluster = function getConfigsCluster (req, res, next) {
  var id = req.swagger.params['id'].value;
  var body = req.swagger.params['body'].value;
  V1stacks.getConfigsCluster(id,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getFullCluster = function getFullCluster (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1stacks.getFullCluster(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateCluster = function getPrivateCluster (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1stacks.getPrivateCluster(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivateStack = function getPrivateStack (req, res, next) {
  var name = req.swagger.params['name'].value;
  var entry = req.swagger.params['entry'].value;
  V1stacks.getPrivateStack(name,entry)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPrivatesStack = function getPrivatesStack (req, res, next) {
  V1stacks.getPrivatesStack()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicCluster = function getPublicCluster (req, res, next) {
  var name = req.swagger.params['name'].value;
  V1stacks.getPublicCluster(name)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicStack = function getPublicStack (req, res, next) {
  var name = req.swagger.params['name'].value;
  var entry = req.swagger.params['entry'].value;
  V1stacks.getPublicStack(name,entry)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getPublicsStack = function getPublicsStack (req, res, next) {
  V1stacks.getPublicsStack()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getStack = function getStack (req, res, next) {
  var id = req.swagger.params['id'].value;
  var entry = req.swagger.params['entry'].value;
  V1stacks.getStack(id,entry)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getStackForAmbari = function getStackForAmbari (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1stacks.getStackForAmbari(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.postCluster = function postCluster (req, res, next) {
  var id = req.swagger.params['id'].value;
  var body = req.swagger.params['body'].value;
  V1stacks.postCluster(id,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putCluster = function putCluster (req, res, next) {
  var id = req.swagger.params['id'].value;
  var body = req.swagger.params['body'].value;
  V1stacks.putCluster(id,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.putStack = function putStack (req, res, next) {
  var id = req.swagger.params['id'].value;
  var body = req.swagger.params['body'].value;
  V1stacks.putStack(id,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.repairCluster = function repairCluster (req, res, next) {
  var id = req.swagger.params['id'].value;
  var body = req.swagger.params['body'].value;
  V1stacks.repairCluster(id,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.statusStack = function statusStack (req, res, next) {
  var id = req.swagger.params['id'].value;
  V1stacks.statusStack(id)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.upgradeCluster = function upgradeCluster (req, res, next) {
  var id = req.swagger.params['id'].value;
  var body = req.swagger.params['body'].value;
  V1stacks.upgradeCluster(id,body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.validateStack = function validateStack (req, res, next) {
  var body = req.swagger.params['body'].value;
  V1stacks.validateStack(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.variantsStack = function variantsStack (req, res, next) {
  V1stacks.variantsStack()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
