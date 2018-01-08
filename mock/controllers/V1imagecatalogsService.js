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
        },
        "knox" : {
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

exports.getPublicImageCatalogsById = function(args, res, next) {
  /**
   * get custom image catalog by name
   * Provides an interface to determine available Virtual Machine images for the given version of Cloudbreak.
   *
   * name String 
   * returns ImageCatalogResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "default" : false,
  "publicInAccount" : false,
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou"
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
  examples['application/json'] = 
    {
    "baseImages":
    [
      {
        "hdpStacks":
        [
          {
            "version":"2.5.5.0",
            "repo":
            {
              "stack":
              {
                "repoid":"HDP-2.5",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.5.0",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.5.5.0",
                "repository-version":"2.5.3.0-37",
                "vdf-redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.3.0/HDP-2.5.3.0-37.xml",
                "vdf-redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.5.3.0/HDP-2.5.3.0-37.xml"
              },
              "util":
              {
                "repoid":"HDP-UTILS-1.1.0.21",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7"
              },
              "knox":
              {}
            }
          },{
            "version":"2.6.3.0",
            "repo":
            {
              "stack":
              {
                "repoid":"HDP-2.6",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.6.3.0",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.3.0",
                "repository-version":"2.6.3.0-235",
                "vdf-redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.6.3.0/HDP-2.6.3.0-235.xml",
                "vdf-redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.3.0/HDP-2.6.3.0-235.xml"
              },
              "util":
              {
                "repoid":"HDP-UTILS-1.1.0.21",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7"
              },
              "knox":
              {}
            }
          }
        ],
        "hdfStacks":
        [
          {
            "version":"3.0.0.0-453",
            "repo":
            {
              "stack":
              {
                "repoid":"HDF-3.0",
                "redhat6":"http://public-repo-1.hortonworks.com/HDF/centos6/3.x/updates/3.0.0.0",
                "redhat7":"http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.0.0.0",
                "mpack":"http://public-repo-1.hortonworks.com/HDF/centos6/3.x/updates/3.0.0.0/tars/hdf_ambari_mp/hdf-ambari-mpack-3.0.0.0-453.tar.gz",
                "repository-version":"3.0.0.0-453",
                "vdf-redhat6":"http://public-repo-1.hortonworks.com/HDF/centos6/3.x/updates/3.0.0.0/HDF-3.0.0.0-453.xml",
                "vdf-redhat7":"http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.0.0.0/HDF-3.0.0.0-453.xml"
              },
              "util":
              {
                "repoid":"HDP-UTILS-1.1.0.21",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7"
              },
              "knox":
              {
                "repoid":"KNOX-HDP-2.6",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.6.1.0",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.1.0"
              }
            }
          }
        ],
        "date":"2017-10-13",
        "description":"Cloudbreak official base image",
        "os":"centos7",
        "osType":"redhat7",
        "uuid":"f6e778fc-7f17-4535-9021-515351df3691",
        "version":"2.6.0.0",
        "repo":
        {
          "baseurl":"http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.0.0",
          "gpgkey":"http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
        },
        "images":
        {
          "openstack":{"default":"hdc-hdp--1710161226"},
          "gcp":
          {
            "default":"sequenceiqimage/hdc-hdp--1710161226.tar.gz"
          },
          "azure":
          {
            "Australia East":"https://hwxaustraliaeast.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Australia South East":"https://hwxaustralisoutheast.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Brazil South":"https://sequenceiqbrazilsouth2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Canada Central":"https://sequenceiqcanadacentral.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Canada East":"https://sequenceiqcanadaeast.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Central India":"https://hwxcentralindia.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Central US":"https://sequenceiqcentralus2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "East Asia":"https://sequenceiqeastasia2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "East US":"https://sequenceiqeastus12.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "East US 2":"https://sequenceiqeastus22.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Japan East":"https://sequenceiqjapaneast2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Japan West":"https://sequenceiqjapanwest2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Korea Central":"https://hwxkoreacentral.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Korea South":"https://hwxkoreasouth.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "North Central US":"https://sequenceiqorthcentralus2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "North Europe":"https://sequenceiqnortheurope2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "South Central US":"https://sequenceiqouthcentralus2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "South India":"https://hwxsouthindia.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "Southeast Asia":"https://sequenceiqsoutheastasia2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "UK South":"https://hwxsouthuk.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "UK West":"https://hwxwestuk.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "West Central US":"https://hwxwestcentralus.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "West Europe":"https://sequenceiqwesteurope2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "West India":"https://hwxwestindia.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "West US":"https://sequenceiqwestus2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd",
            "West US 2":"https://hwxwestus2.blob.core.windows.net/images/hdc-hdp--1710161226.vhd"
          }
        }
      },{
        "hdpStacks":
        [
          {
            "version":"2.5.5.0",
            "repo":
            {
              "stack":
              {
                "repoid":"HDP-2.5",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.5.0",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.5.5.0",
                "repository-version":"2.5.3.0-37",
                "vdf-redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.3.0/HDP-2.5.3.0-37.xml",
                "vdf-redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.5.3.0/HDP-2.5.3.0-37.xml"
              },
              "util":
              {
                "repoid":"HDP-UTILS-1.1.0.21",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7"
              },
              "knox":
              {}
            }
          },{
            "version":"2.6.3.0",
            "repo":
            {
              "stack":
              {
                "repoid":"HDP-2.6",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.6.3.0",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.3.0",
                "repository-version":"2.6.3.0-235",
                "vdf-redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.6.3.0/HDP-2.6.3.0-235.xml",
                "vdf-redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.3.0/HDP-2.6.3.0-235.xml"
              },
              "util":
              {
                "repoid":"HDP-UTILS-1.1.0.21",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7"
              },
              "knox":
              {}
            }
          }
        ],
        "hdfStacks":
        [
          {
            "version":"3.0.0.0-453",
            "repo":
            {
              "stack":
              {
                "repoid":"HDF-3.0",
                "redhat6":"http://public-repo-1.hortonworks.com/HDF/centos6/3.x/updates/3.0.0.0",
                "redhat7":"http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.0.0.0",
                "mpack":"http://public-repo-1.hortonworks.com/HDF/centos6/3.x/updates/3.0.0.0/tars/hdf_ambari_mp/hdf-ambari-mpack-3.0.0.0-453.tar.gz",
                "repository-version":"3.0.0.0-453",
                "vdf-redhat6":"http://public-repo-1.hortonworks.com/HDF/centos6/3.x/updates/3.0.0.0/HDF-3.0.0.0-453.xml",
                "vdf-redhat7":"http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.0.0.0/HDF-3.0.0.0-453.xml"
              },
              "util":
              {
                "repoid":"HDP-UTILS-1.1.0.21",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7"
              },
              "knox":
              {
                "repoid":"KNOX-HDP-2.6",
                "redhat6":"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.6.1.0",
                "redhat7":"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.1.0"
              }
            }
          }
        ],
        "date":"2017-11-10",
        "description":"Official Cloudbreak image",
        "os":"centos7",
        "osType":"redhat7",
        "uuid":"3f66bdad-5f59-4460-6182-0359c2cf7f1b",
        "version":"2.6.0.0",
        "repo":
        {
          "baseurl":"http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.0.0",
          "gpgkey":"http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
        },
        "images":
        {
          "openstack":{
            "default":"hdc-hdp--1711101841"
          }
        }
      }
    ],
    "hdpImages":[],
    "hdfImages":[]
  };
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
  examples['application/json'] = 
  [
  	{
  		"name":"cloudbreak-default",
  		"url":"https://s3-eu-west-1.amazonaws.com/cloudbreak-info/v2-dev-cb-image-catalog.json",
  		"publicInAccount":true,
  		"default":true
  	}
  ];
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
  "default" : false,
  "publicInAccount" : false,
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou"
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
  "default" : false,
  "publicInAccount" : false,
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou"
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
  "default" : false,
  "publicInAccount" : false,
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou"
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
  "default" : false,
  "publicInAccount" : false,
  "name" : "aeiou",
  "id" : 0,
  "url" : "aeiou"
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

