'use strict';


/**
 * checks the client version
 * 
 *
 * version String 
 * returns VersionCheckResult
 **/
exports.checkClientVersion = function(version) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "versionCheckOk" : true,
  "message" : "message"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create a database for the service in the RDS if the connection could be created
 * 
 *
 * body RDSBuildRequest  (optional)
 * target List  (optional)
 * returns RdsBuildResult
 **/
exports.createRDSDatabaseUtil = function(body,target) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "results" : {
    "key" : "results"
  }
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * returns default ambari details for distinct HDP and HDF
 * 
 *
 * returns StackMatrix
 **/
exports.getStackMatrixUtil = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
        "hdp": {
            "3.0": {
                "version": "3.0.0.0-1073",
                "minAmbari": "2.7",
                "repo": {
                    "stack": {
                        "repoid": "HDP-3.0",
                        "repository-version": "3.0.0.0-1073",
                        "debian9": "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian9/3.x/BUILDS/3.0.0.0-1073",
                        "ubuntu16": "http://s3.amazonaws.com/dev.hortonworks.com/HDP/ubuntu16/3.x/BUILDS/3.0.0.0-1073",
                        "vdf-ubuntu16": "http://s3.amazonaws.com/dev.hortonworks.com/HDP/ubuntu16/3.x/BUILDS/3.0.0.0-1073/HDP-3.0.0.0-1073.xml",
                        "redhat7": "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos7/3.x/BUILDS/3.0.0.0-1073",
                        "vdf-redhat7": "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos7/3.x/BUILDS/3.0.0.0-1073/HDP-3.0.0.0-1073.xml",
                        "vdf-debian9": "http://s3.amazonaws.com/dev.hortonworks.com/HDP/debian9/3.x/BUILDS/3.0.0.0-1073/HDP-3.0.0.0-1073.xml"
                    },
                    "util": {
                        "repoid": "HDP-UTILS-1.1.0.22",
                        "debian9": "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.22/repos/debian9",
                        "ubuntu16": "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.22/repos/ubuntu16",
                        "redhat7": "http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.22/repos/centos7"
                    }
                },
                "ambari": {
                    "version": "2.7.0.0-190",
                    "repo": {
                        "debian9": {
                            "baseUrl": "http://s3.amazonaws.com/dev.hortonworks.com/ambari/debian9/2.x/BUILDS/2.7.0.0-190",
                            "gpgKeyUrl": "http://s3.amazonaws.com/dev.hortonworks.com/ambari/debian9/2.x/BUILDS/2.7.0.0-190/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "ubuntu16": {
                            "baseUrl": "http://s3.amazonaws.com/dev.hortonworks.com/ambari/ubuntu16/2.x/BUILDS/2.7.0.0-190",
                            "gpgKeyUrl": "http://s3.amazonaws.com/dev.hortonworks.com/ambari/ubuntu16/2.x/BUILDS/2.7.0.0-190/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "redhat7": {
                            "baseUrl": "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos7/2.x/BUILDS/2.7.0.0-190",
                            "gpgKeyUrl": "http://s3.amazonaws.com/dev.hortonworks.com/ambari/centos7/2.x/BUILDS/2.7.0.0-190/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        }
                    }
                }
            },
            "2.5": {
                "version": "2.5.5.0",
                "minAmbari": "2.6",
                "repo": {
                    "stack": {
                        "repoid": "HDP-2.5",
                        "vdf-redhat6": "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.3.0/HDP-2.5.3.0-37.xml",
                        "repository-version": "2.5.3.0-37",
                        "debian9": "http://public-repo-1.hortonworks.com/HDP/debian9/2.x/updates/2.5.5.0",
                        "ubuntu16": "http://public-repo-1.hortonworks.com/HDP/ubuntu16/2.x/updates/2.5.5.0",
                        "vdf-ubuntu16": "http://public-repo-1.hortonworks.com/HDP/ubuntu16/2.x/updates/2.5.3.0/HDP-2.5.3.0-37.xml",
                        "redhat7": "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.5.5.0",
                        "redhat6": "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.5.0",
                        "vdf-redhat7": "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.5.3.0/HDP-2.5.3.0-37.xml",
                        "vdf-debian9": "http://public-repo-1.hortonworks.com/HDP/debian9/2.x/updates/2.5.3.0/HDP-2.5.3.0-37.xml"
                    },
                    "util": {
                        "repoid": "HDP-UTILS-1.1.0.21",
                        "debian9": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/debian9",
                        "ubuntu16": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/ubuntu16",
                        "redhat7": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7",
                        "redhat6": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6"
                    }
                },
                "ambari": {
                    "version": "2.6.1.3",
                    "repo": {
                        "sles12": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/sles12/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "debian9": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/debian9/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "ubuntu16": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/ubuntu16/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "redhat7": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "redhat6": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        }
                    }
                }
            },
            "2.6": {
                "version": "2.6.4.5-2",
                "minAmbari": "2.6",
                "repo": {
                    "stack": {
                        "repoid": "HDP-2.6",
                        "vdf-redhat6": "http://private-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.6.4.5-2/HDP-2.6.4.5-2.xml",
                        "sles12": "http://private-repo-1.hortonworks.com/HDP/sles12/2.x/updates/2.6.4.5-2",
                        "repository-version": "2.6.4.5-2",
                        "vdf-sles12": "http://private-repo-1.hortonworks.com/HDP/sles12/2.x/updates/2.6.4.5-2/HDP-2.6.4.5-2.xml",
                        "debian9": "http://private-repo-1.hortonworks.com/HDP/debian9/2.x/updates/2.6.4.5-2",
                        "ubuntu16": "http://private-repo-1.hortonworks.com/HDP/ubuntu16/2.x/updates/2.6.4.5-2",
                        "vdf-ubuntu16": "http://private-repo-1.hortonworks.com/HDP/ubuntu16/2.x/updates/2.6.4.5-2/HDP-2.6.4.5-2.xml",
                        "redhat7": "http://private-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.4.5-2",
                        "redhat6": "http://private-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.6.4.5-2",
                        "vdf-redhat7": "http://private-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.4.5-2/HDP-2.6.4.5-2.xml",
                        "vdf-debian9": "http://private-repo-1.hortonworks.com/HDP/debian9/2.x/updates/2.6.4.5-2/HDP-2.6.4.5-2.xml"
                    },
                    "util": {
                        "repoid": "HDP-UTILS-1.1.0.22",
                        "sles12": "http://private-repo-1.hortonworks.com/HDP-UTILS-1.1.0.22/repos/sles12",
                        "debian9": "http://private-repo-1.hortonworks.com/HDP-UTILS-1.1.0.22/repos/debian9",
                        "ubuntu16": "http://private-repo-1.hortonworks.com/HDP-UTILS-1.1.0.22/repos/ubuntu16",
                        "redhat7": "http://private-repo-1.hortonworks.com/HDP-UTILS-1.1.0.22/repos/centos7",
                        "redhat6": "http://private-repo-1.hortonworks.com/HDP-UTILS-1.1.0.22/repos/centos6"
                    }
                },
                "ambari": {
                    "version": "2.6.1.3",
                    "repo": {
                        "sles12": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/sles12/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "debian9": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/debian9/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "ubuntu16": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/ubuntu16/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "redhat7": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "redhat6": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        }
                    }
                }
            }
        },
        "hdf": {
            "3.1": {
                "version": "3.1.1.0-35",
                "minAmbari": "2.6",
                "repo": {
                    "stack": {
                        "repoid": "HDF-3.1",
                        "mpack": "http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.1.1.0/tars/hdf_ambari_mp/hdf-ambari-mpack-3.1.1.0-35.tar.gz",
                        "vdf-redhat6": "http://public-repo-1.hortonworks.com/HDF/centos6/3.x/updates/3.1.1.0/HDF-3.1.1.0-35.xml",
                        "repository-version": "3.1.1.0-35",
                        "debian9": "http://public-repo-1.hortonworks.com/HDF/debian9/3.x/updates/3.1.1.0",
                        "ubuntu16": "http://public-repo-1.hortonworks.com/HDF/ubuntu16/3.x/updates/3.1.1.0",
                        "vdf-ubuntu16": "http://public-repo-1.hortonworks.com/HDF/ubuntu16/3.x/updates/3.1.1.0/HDF-3.1.1.0-35.xml",
                        "redhat7": "http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.1.1.0",
                        "redhat6": "http://public-repo-1.hortonworks.com/HDF/centos6/3.x/updates/3.1.1.0",
                        "vdf-redhat7": "http://public-repo-1.hortonworks.com/HDF/centos7/3.x/updates/3.1.1.0/HDF-3.1.1.0-35.xml",
                        "vdf-debian9": "http://public-repo-1.hortonworks.com/HDF/debian9/3.x/updates/3.1.1.0/HDF-3.1.1.0-35.xml"
                    },
                    "util": {
                        "repoid": "HDP-UTILS-1.1.0.21",
                        "debian9": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/debian9",
                        "ubuntu16": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/ubuntu16",
                        "redhat7": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7",
                        "redhat6": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos6"
                    }
                },
                "ambari": {
                    "version": "2.6.1.3",
                    "repo": {
                        "sles12": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/sles12/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "debian9": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/debian9/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "ubuntu16": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/ubuntu16/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "redhat7": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        },
                        "redhat6": {
                            "baseUrl": "http://public-repo-1.hortonworks.com/ambari/centos6/2.x/updates/2.6.1.3",
                            "gpgKeyUrl": "http://public-repo-1.hortonworks.com/ambari/centos6/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                        }
                    }
                }
            }
        }
    };
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * tests a database connection parameters
 * 
 *
 * body AmbariDatabaseDetails  (optional)
 * returns AmbariDatabaseTestResult
 **/
exports.testAmbariDatabaseUtil = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "error" : "error"
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

