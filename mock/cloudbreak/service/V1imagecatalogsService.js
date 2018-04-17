'use strict';


/**
 * delete public (owned) or private Image Catalog by id
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicImageCatalogByName = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve imagecatalog request by imagecatalog name
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * name String 
 * returns ImageCatalogRequest
 **/
exports.getImageCatalogRequestFromName = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/imagecatalogs/default-imagecatalog.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * determines available images for the Cloudbreak version by the given provider and default image catalog url
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * platform String 
 * returns ImagesResponse
 **/
exports.getImagesByProvider = function(platform) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "hdpImages" : [ {
    "date" : "date",
    "images" : {
      "key" : {
        "key" : "images"
      }
    },
    "stackDetails" : {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    },
    "os" : "os",
    "repo" : {
      "key" : "repo"
    },
    "osType" : "osType",
    "description" : "description",
    "uuid" : "uuid",
    "version" : "version"
  }, {
    "date" : "date",
    "images" : {
      "key" : {
        "key" : "images"
      }
    },
    "stackDetails" : {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    },
    "os" : "os",
    "repo" : {
      "key" : "repo"
    },
    "osType" : "osType",
    "description" : "description",
    "uuid" : "uuid",
    "version" : "version"
  } ],
  "baseImages" : [ {
    "hdpStacks" : [ {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    }, {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    } ],
    "date" : "date",
    "images" : {
      "key" : {
        "key" : "images"
      }
    },
    "stackDetails" : {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    },
    "os" : "os",
    "hdfStacks" : [ {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    }, {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    } ],
    "repo" : {
      "key" : "repo"
    },
    "osType" : "osType",
    "description" : "description",
    "uuid" : "uuid",
    "version" : "version"
  }, {
    "hdpStacks" : [ {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    }, {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    } ],
    "date" : "date",
    "images" : {
      "key" : {
        "key" : "images"
      }
    },
    "stackDetails" : {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    },
    "os" : "os",
    "hdfStacks" : [ {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    }, {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    } ],
    "repo" : {
      "key" : "repo"
    },
    "osType" : "osType",
    "description" : "description",
    "uuid" : "uuid",
    "version" : "version"
  } ],
  "hdfImages" : [ {
    "date" : "date",
    "images" : {
      "key" : {
        "key" : "images"
      }
    },
    "stackDetails" : {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    },
    "os" : "os",
    "repo" : {
      "key" : "repo"
    },
    "osType" : "osType",
    "description" : "description",
    "uuid" : "uuid",
    "version" : "version"
  }, {
    "date" : "date",
    "images" : {
      "key" : {
        "key" : "images"
      }
    },
    "stackDetails" : {
      "mpacks" : [ {
        "mpackUrl" : "mpackUrl"
      }, {
        "mpackUrl" : "mpackUrl"
      } ],
      "repo" : {
        "stack" : {
          "key" : "stack"
        },
        "util" : {
          "key" : "util"
        }
      },
      "version" : "version"
    },
    "os" : "os",
    "repo" : {
      "key" : "repo"
    },
    "osType" : "osType",
    "description" : "description",
    "uuid" : "uuid",
    "version" : "version"
  } ]
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * get custom image catalog by name
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * name String 
 * withImages Boolean  (optional)
 * returns ImageCatalogResponse
 **/
exports.getPublicImageCatalogsByName = function(name,withImages) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/imagecatalogs/cloudbreak-default.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * determines available images for the Cloudbreak version by the given provider and given image catalog url
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * name String 
 * platform String 
 * returns ImagesResponse
 **/
exports.getPublicImagesByProviderAndCustomImageCatalog = function(name,platform) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/imagecatalogs/qa-images.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * list available custom image catalogs as public resources
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * returns List
 **/
exports.getPublicsImageCatalogs = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/imagecatalogs/default-imagecatalog.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create Image Catalog as private resources
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * body ImageCatalogRequest  (optional)
 * returns ImageCatalogResponse
 **/
exports.postPrivateImageCatalog = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/imagecatalogs/default-imagecatalog.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create Image Catalog as public resources
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * body ImageCatalogRequest  (optional)
 * returns ImageCatalogResponse
 **/
exports.postPublicImageCatalog = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/imagecatalogs/default-imagecatalog.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * update public (owned) or private Image Catalog by id
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * body UpdateImageCatalogRequest  (optional)
 * returns ImageCatalogResponse
 **/
exports.putPublicImageCatalog = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/imagecatalogs/default-imagecatalog.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * update public (owned) or private Image Catalog by id
 * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
 *
 * name String 
 * returns ImageCatalogResponse
 **/
exports.putSetDefaultImageCatalogByName = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = require('../responses/imagecatalogs/default-imagecatalog.json');
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

