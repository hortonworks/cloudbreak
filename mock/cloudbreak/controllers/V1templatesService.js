'use strict';

exports.deletePrivateTemplate = function(args, res, next) {
  /**
   * delete private template by name
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deletePublicTemplate = function(args, res, next) {
  /**
   * delete public (owned) or private template by name
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * name String 
   * no response value expected for this operation
   **/
  res.end();
}

exports.deleteTemplate = function(args, res, next) {
  /**
   * delete template by id
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * id Long 
   * no response value expected for this operation
   **/
  res.end();
}

exports.getPrivateTemplate = function(args, res, next) {
  /**
   * retrieve a private template by name
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * name String 
   * returns TemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPrivatesTemplate = function(args, res, next) {
  /**
   * retrieve private templates
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = [ {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
} ];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicTemplate = function(args, res, next) {
  /**
   * retrieve a public or private (owned) template by name
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * name String 
   * returns TemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getPublicsTemplate = function(args, res, next) {
  /**
   * retrieve public and private (owned) templates
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = 
  [ 
    {
      "cloudPlatform":"OPENSTACK",
      "parameters":
      {
        "encrypted":false
      },
      "description":"",
      "volumeType":"HDD",
      "instanceType":"cloudbreak",
      "customInstanceType":null,
      "topologyId":null,
      "name":"taef3eebf-c93c-49c9-af5d-37d1ec97d160",
      "id":1,
      "volumeCount":0,
      "volumeSize":100,
      "public":false
    },{
      "cloudPlatform":"AWS",
      "parameters":
      {
        "encrypted":false
      },
      "description":"",
      "volumeType":"standard",
      "instanceType":"m4.xlarge",
      "customInstanceType":null,
      "topologyId":null,
      "name":"tb9d8d756-81e4-471f-a7a9-a188fa1ce85b",
      "id":2,
      "volumeCount":1,
      "volumeSize":100,
      "public":false
    },{
      "cloudPlatform":"AWS",
      "parameters":
      {
        "encrypted":false
      },
      "description":"",
      "volumeType":"standard",
      "instanceType":"m4.4xlarge",
      "customInstanceType":null,
      "topologyId":null,
      "name":"t7b01713b-522e-41b6-bbfc-5c60da63d050",
      "id":3,
      "volumeCount":1,
      "volumeSize":100,
      "public":false
    },{
      "cloudPlatform":"AZURE",
      "parameters":
      {
        "encrypted":false,
        "managedDisk":true
      },
      "description":"",
      "volumeType":"Standard_LRS",
      "instanceType":"Standard_D3_v2",
      "customInstanceType":null,
      "topologyId":null,
      "name":"t38826b32-a741-473a-9ab9-0510b4fd7829",
      "id":4,
      "volumeCount":1,
      "volumeSize":100,
      "public":false
    },{
      "cloudPlatform": "GCP",
      "parameters":
      {
         "encrypted": false
      },
      "description": "",
      "volumeType": "pd-standard",
      "instanceType": "n1-standard-4",
      "customInstanceType": null,
      "topologyId": null,
      "name": "t2c7d1a00-45f0-4dca-a6bd-ac992e761d90",
      "id": 5,
      "volumeCount": 1,
      "volumeSize": 100,
      "public": false
    }
];
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.getTemplate = function(args, res, next) {
  /**
   * retrieve template by id
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * id Long 
   * returns TemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPrivateTemplate = function(args, res, next) {
  /**
   * create template as private resource
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * body TemplateRequest  (optional)
   * returns TemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

exports.postPublicTemplate = function(args, res, next) {
  /**
   * create template as public resource
   * A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.
   *
   * body TemplateRequest  (optional)
   * returns TemplateResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "volumeType" : "aeiou",
  "cloudPlatform" : "aeiou",
  "public" : false,
  "instanceType" : "aeiou",
  "customInstanceType" : {
    "memory" : 0,
    "cpus" : 6
  },
  "topologyId" : 1,
  "name" : "aeiou",
  "description" : "aeiou",
  "volumeCount" : 5,
  "id" : 5,
  "parameters" : {
    "key" : "{}"
  },
  "volumeSize" : 2
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}

