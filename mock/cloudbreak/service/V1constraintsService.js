'use strict';


/**
 * delete constraint template by id
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * id Long 
 * no response value expected for this operation
 **/
exports.deleteConstraint = function(id) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete private constraint template by name
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePrivateConstraint = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * delete public (owned) or private constraint template by name
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * name String 
 * no response value expected for this operation
 **/
exports.deletePublicConstraint = function(name) {
  return new Promise(function(resolve, reject) {
    resolve();
  });
}


/**
 * retrieve constraint template by id
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * id Long 
 * returns ConstraintTemplateResponse
 **/
exports.getConstraint = function(id) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "orchestratorType" : "orchestratorType",
  "disk" : 1.4658129805029452,
  "memory" : 6.027456183070403,
  "publicInAccount" : false,
  "name" : "name",
  "description" : "description",
  "cpu" : 0.8008281904610115,
  "id" : 5
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a private constraint template by name
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * name String 
 * returns ConstraintTemplateResponse
 **/
exports.getPrivateConstraint = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "orchestratorType" : "orchestratorType",
  "disk" : 1.4658129805029452,
  "memory" : 6.027456183070403,
  "publicInAccount" : false,
  "name" : "name",
  "description" : "description",
  "cpu" : 0.8008281904610115,
  "id" : 5
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve private constraint templates
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * returns List
 **/
exports.getPrivatesConstraint = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "orchestratorType" : "orchestratorType",
  "disk" : 1.4658129805029452,
  "memory" : 6.027456183070403,
  "publicInAccount" : false,
  "name" : "name",
  "description" : "description",
  "cpu" : 0.8008281904610115,
  "id" : 5
}, {
  "orchestratorType" : "orchestratorType",
  "disk" : 1.4658129805029452,
  "memory" : 6.027456183070403,
  "publicInAccount" : false,
  "name" : "name",
  "description" : "description",
  "cpu" : 0.8008281904610115,
  "id" : 5
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve a public or private (owned) constraint template by name
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * name String 
 * returns ConstraintTemplateResponse
 **/
exports.getPublicConstraint = function(name) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "orchestratorType" : "orchestratorType",
  "disk" : 1.4658129805029452,
  "memory" : 6.027456183070403,
  "publicInAccount" : false,
  "name" : "name",
  "description" : "description",
  "cpu" : 0.8008281904610115,
  "id" : 5
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * retrieve public and private (owned) constraint templates
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * returns List
 **/
exports.getPublicsConstraint = function() {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = [ {
  "orchestratorType" : "orchestratorType",
  "disk" : 1.4658129805029452,
  "memory" : 6.027456183070403,
  "publicInAccount" : false,
  "name" : "name",
  "description" : "description",
  "cpu" : 0.8008281904610115,
  "id" : 5
}, {
  "orchestratorType" : "orchestratorType",
  "disk" : 1.4658129805029452,
  "memory" : 6.027456183070403,
  "publicInAccount" : false,
  "name" : "name",
  "description" : "description",
  "cpu" : 0.8008281904610115,
  "id" : 5
} ];
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create constraint template as private resource
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * body ConstraintTemplateRequest  (optional)
 * returns ConstraintTemplateResponse
 **/
exports.postPrivateConstraint = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "orchestratorType" : "orchestratorType",
  "disk" : 1.4658129805029452,
  "memory" : 6.027456183070403,
  "publicInAccount" : false,
  "name" : "name",
  "description" : "description",
  "cpu" : 0.8008281904610115,
  "id" : 5
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}


/**
 * create constraint template as public resource
 * A constraint template tells Cloudbreak the resource constraints (cpu, memory, disk) of the Ambari containers that will be deployed to the cluster. A constraint template must be created onenvironments where there is no one-to-one mapping between containers and nodes, like YARN.
 *
 * body ConstraintTemplateRequest  (optional)
 * returns ConstraintTemplateResponse
 **/
exports.postPublicConstraint = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "orchestratorType" : "orchestratorType",
  "disk" : 1.4658129805029452,
  "memory" : 6.027456183070403,
  "publicInAccount" : false,
  "name" : "name",
  "description" : "description",
  "cpu" : 0.8008281904610115,
  "id" : 5
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

