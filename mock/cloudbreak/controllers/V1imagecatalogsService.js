'use strict';

exports.deletePublicImageCatalogByName = function(args, res, next) {
  /**
   * delete public (owned) or private Image Catalog by id
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getImageCatalogRequestFromName = function(args, res, next) {
  /**
   * retrieve imagecatalog request by imagecatalog name
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * name String 
   * returns ImageCatalogRequest
   **/
  var examples = {};
  examples['application/json'] = require('../responses/imagecatalogs/default-imagecatalog.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getImagesByProvider = function(args, res, next) {
  /**
   * determines available images for the Cloudbreak version by the given provider and default image catalog url
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * platform String 
   * returns ImagesResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "hdpImages" : [ {
    "date" : "aeiou",
    "images" : {
      "key" : {
        "key" : "aeiou"
      }
    },
    "stackDetails" : "",
    "os" : "aeiou",
    "repo" : {
      "key" : "aeiou"
    },
    "osType" : "aeiou",
    "description" : "aeiou",
    "uuid" : "aeiou",
    "version" : "aeiou"
  } ],
  "baseImages" : [ {
    "hdpStacks" : [ {
      "repo" : {
        "stack" : {
          "key" : "aeiou"
        },
        "util" : {
          "key" : "aeiou"
        }
      },
      "version" : "aeiou"
    } ],
    "date" : "aeiou",
    "images" : {
      "key" : {
        "key" : "aeiou"
      }
    },
    "stackDetails" : "",
    "os" : "aeiou",
    "hdfStacks" : [ "" ],
    "repo" : {
      "key" : "aeiou"
    },
    "osType" : "aeiou",
    "description" : "aeiou",
    "uuid" : "aeiou",
    "version" : "aeiou"
  } ],
  "hdfImages" : [ "" ]
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicImageCatalogsByName = function(args, res, next) {
  /**
   * get custom image catalog by name
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * name String 
   * withImages Boolean  (optional)
   * returns ImageCatalogResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "publicInAccount" : false,
  "imagesResponse" : {
    "hdpImages" : [ {
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "baseImages" : [ {
      "hdpStacks" : [ {
        "repo" : {
          "stack" : {
            "key" : "aeiou"
          },
          "util" : {
            "key" : "aeiou"
          }
        },
        "version" : "aeiou"
      } ],
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "hdfStacks" : [ "" ],
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "hdfImages" : [ "" ]
  },
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou",
  "usedAsDefault" : false
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicImagesByProviderAndCustomImageCatalog = function(args, res, next) {
  /**
   * determines available images for the Cloudbreak version by the given provider and given image catalog url
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * name String 
   * platform String 
   * returns ImagesResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/imagecatalogs/qa-images.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsImageCatalogs = function(args, res, next) {
  /**
   * list available custom image catalogs as public resources
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = require('../responses/imagecatalogs/default-imagecatalog.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateImageCatalog = function(args, res, next) {
  /**
   * create Image Catalog as private resources
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * body ImageCatalogRequest  (optional)
   * returns ImageCatalogResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "publicInAccount" : false,
  "imagesResponse" : {
    "hdpImages" : [ {
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "baseImages" : [ {
      "hdpStacks" : [ {
        "repo" : {
          "stack" : {
            "key" : "aeiou"
          },
          "util" : {
            "key" : "aeiou"
          }
        },
        "version" : "aeiou"
      } ],
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "hdfStacks" : [ "" ],
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "hdfImages" : [ "" ]
  },
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou",
  "usedAsDefault" : false
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicImageCatalog = function(args, res, next) {
  /**
   * create Image Catalog as public resources
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * body ImageCatalogRequest  (optional)
   * returns ImageCatalogResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "publicInAccount" : false,
  "imagesResponse" : {
    "hdpImages" : [ {
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "baseImages" : [ {
      "hdpStacks" : [ {
        "repo" : {
          "stack" : {
            "key" : "aeiou"
          },
          "util" : {
            "key" : "aeiou"
          }
        },
        "version" : "aeiou"
      } ],
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "hdfStacks" : [ "" ],
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "hdfImages" : [ "" ]
  },
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou",
  "usedAsDefault" : false
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.putPublicImageCatalog = function(args, res, next) {
  /**
   * update public (owned) or private Image Catalog by id
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * body UpdateImageCatalogRequest  (optional)
   * returns ImageCatalogResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "publicInAccount" : false,
  "imagesResponse" : {
    "hdpImages" : [ {
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "baseImages" : [ {
      "hdpStacks" : [ {
        "repo" : {
          "stack" : {
            "key" : "aeiou"
          },
          "util" : {
            "key" : "aeiou"
          }
        },
        "version" : "aeiou"
      } ],
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "hdfStacks" : [ "" ],
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "hdfImages" : [ "" ]
  },
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou",
  "usedAsDefault" : false
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.putSetDefaultImageCatalogByName = function(args, res, next) {
  /**
   * update public (owned) or private Image Catalog by id
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * name String 
   * returns ImageCatalogResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "publicInAccount" : false,
  "imagesResponse" : {
    "hdpImages" : [ {
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "baseImages" : [ {
      "hdpStacks" : [ {
        "repo" : {
          "stack" : {
            "key" : "aeiou"
          },
          "util" : {
            "key" : "aeiou"
          }
        },
        "version" : "aeiou"
      } ],
      "date" : "aeiou",
      "images" : {
        "key" : {
          "key" : "aeiou"
        }
      },
      "stackDetails" : "",
      "os" : "aeiou",
      "hdfStacks" : [ "" ],
      "repo" : {
        "key" : "aeiou"
      },
      "osType" : "aeiou",
      "description" : "aeiou",
      "uuid" : "aeiou",
      "version" : "aeiou"
    } ],
    "hdfImages" : [ "" ]
  },
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou",
  "usedAsDefault" : false
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

