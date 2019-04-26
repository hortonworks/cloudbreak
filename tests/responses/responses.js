var responses={};

var accountpreferences = require('./accountpreferences/accountpreferences.json');
var platforms = require('./accountpreferences/platforms.json');
var defaultblueprint = require('./blueprints/default-blueprint.json');
var qablueprint = require('./blueprints/qa-blueprint.json');
var disktypes = require('./connectors/disktypes.json');
var gateways = require('./connectors/gateways.json');
var ippools = require('./connectors/ippools.json');
var connetworks = require('./connectors/networks.json');
var recommendations = require('./connectors/recommendations.json');
var regions = require('./connectors/regions.json');
var sshkeys = require('./connectors/sshkeys.json');
var securitygroups = require('./connectors/securitygroups.json');
var credentialaws = require('./credentials/aws.json');
var credentialazure = require('./credentials/azure.json');
var credentialgcp = require('./credentials/gcp.json');
var credentialopenstack = require('./credentials/openstack.json');
var cbdefimagec = require('./imagecatalogs/cloudbreak-default.json');
var defimagec = require('./imagecatalogs/default-imagecatalog.json');
var qaimages = require('./imagecatalogs/qa-images.json');
var ldap = require('./ldapconfig/default-ldap.json');
var ldapSingle = require('./ldapconfig/default-single-ldap.json');
var networks = require('./networks/networks.json');
var proxy = require('./proxyconfig/default-proxy.json');
var proxySingle = require('./proxyconfig/default-single-proxy.json');
var rds = require('./rdsconfig/default-rds.json');
var rdsSingle = require('./rdsconfig/default-single-rds.json');
var recipes = require('./recipes/recipes.json');
var recipe = require('./recipes/recipe.json');
var responses = require('./responses.js');
var secgr = require('./securitygroups/securitygroups.json');
var secrules = require('./securityrules/securityrules.json');
var stacks = require('./stacks/stacks.json');
var aws = require('./stacks/aws.json');
var azure = require('./stacks/azure.json');
var gcp = require('./stacks/gcp.json');
var openstack = require('./stacks/openstack.json');
var datalake = require('./stacks/datalake.json');
var templates = require('./templates/qa-templates.json');
var profile = require('./users/default-profile.json');
var matrix = require('./utilsmatrix.json');
var mpacks = require('./mpacks/mpacks.json');
var mpacksSingle = require('./mpacks/mpacks-single.json');
var workspaces = require('./workspaces/workspaces.json');
var environments = require('./environment/envs.json');

const OK = 200;

var responseObject = function(payload, code) {
	return { responses : [ { response: payload , statusCode: code } ] };
}

var addResponseObject = function(responses, payload, code, conditionText) {
	var newElement = { response: payload, statusCode: code, condition: conditionText };
	responses.responses.push(newElement);
	return responses;
}

var responseObjectWithCondition = function(payload, code, conditionText) {
	return { responses : [ { response: payload , statusCode: code , condition: conditionText} ] };
}

var stackResponses = responseObjectWithCondition({"message":"Stack 'az404' not found"}, 404, "return params['name'].value === 'az404';");
stackResponses = addResponseObject(stackResponses, openstack, OK, "return (params['name'].value !== 'az404' && params['name'].value !== 'dl-ok')");
stackResponses = addResponseObject(stackResponses, datalake, OK, "return params['name'].value === 'dl-ok';");

var stackOperationResponses = responseObjectWithCondition({"message":"Stack 'azstatus' not found"}, 404, "return params['name'].value === 'azstatus';");
stackOperationResponses = addResponseObject(stackOperationResponses,null, OK , "return params['name'].value !== 'azstatus';");

responses.getCloudbreakInfo= responseObject({ "app": { "name":"cloudbreak", "version":"MOCK" } }, OK);
responses.getCloudbreakHealth= responseObject({ "status":"UP" }, OK);
responses.evictCurrentUserDetails = responseObject({ username: 'mock@hortonworks.com' }, OK);
responses.getDeploymentInfo = responseObject(accountpreferences, OK);
responses.getAuditEventsInWorkspace = responseObject([], OK);

responses.isPlatformSelectionDisabled= responseObject(platforms, OK);
responses.platformEnablement= responseObject(platforms, OK);
responses.listBlueprintsByWorkspace = responseObject(qablueprint, OK);
responses.getBlueprintInWorkspace = responseObject(defaultblueprint, OK);
responses.createBlueprintInWorkspace= responseObject(defaultblueprint, OK);
responses.getDisktypes= responseObject(disktypes, OK);
responses.getGatewaysCredentialId= responseObject(gateways, OK);
responses.getIpPoolsCredentialId= responseObject(ippools, OK);
responses.getPlatformNetworks= responseObject(connetworks, OK);
responses.getPublicsNetwork= responseObject(networks, OK);
responses.createRecommendation= responseObject(recommendations, OK);
responses.getRegions= responseObject(regions, OK);
responses.getPlatformSShKeys= responseObject(sshkeys, OK);
responses.getCredentialInWorkspace = responseObject(credentialopenstack, OK);
responses.createCredentialInWorkspace = responseObject(credentialopenstack, OK);
responses.listCredentialsByWorkspace = responseObject({ "responses": [credentialopenstack, credentialgcp, credentialazure, credentialaws] }, OK);
responses.getPublicCredential= responseObject(credentialopenstack, OK);
responses.getPrivatesCredential= responseObject([credentialopenstack,credentialgcp,credentialazure,credentialaws], OK);
responses.getPrivateCredential= responseObject(credentialopenstack, OK);
responses.listImageCatalogsByWorkspace = responseObject(defimagec, OK);
responses.getImageCatalogInWorkspace = responseObject(defimagec, OK);
responses.createImageCatalogInWorkspace = responseObject(defimagec, OK);
responses.getPublicImageCatalogsByName= responseObject(defimagec, OK);
responses.getImagesByProviderAndCustomImageCatalogInWorkspace= responseObject(qaimages, OK);
responses.putSetDefaultImageCatalogByName= responseObject(defimagec, OK);
responses.postPrivateImageCatalog= responseObject(defimagec, OK);
responses.getImageCatalogRequestFromName= responseObject(defimagec, OK);
responses.listLdapsByWorkspace = responseObject(ldap, OK);
responses.getLdapConfigInWorkspace = responseObject(ldap, OK);
responses.createLdapConfigsInWorkspace = responseObject(ldapSingle, OK);
responses.postLdapConnectionTestInWorkspace = responseObject({ "connectionResult":"Failed to connect to LDAP server: hwxad-1a2bcd3e45678f90.elb.eu-west-1.amazonaws.com:123" }, OK);
responses.getPrivatesLdap= responseObject(ldap, OK);
responses.postPrivateLdap= responseObject(ldap, OK);
responses.getPrivateLdap= responseObject(ldap, OK);
responses.getLdap= responseObject(ldap, OK);
responses.listProxyconfigsByWorkspace = responseObject(proxy, OK);
responses.getProxyconfigInWorkspace = responseObject(proxy, OK);
responses.createProxyconfigInWorkspace = responseObject(proxySingle, OK);
responses.getPrivatesProxyConfig= responseObject(proxy, OK);
responses.postPrivateProxyConfig= responseObject(proxy, OK);
responses.getPrivateProxyConfig= responseObject(proxy, OK);
responses.getProxyConfig= responseObject(proxy, OK);
responses.listDatabasesByWorkspace = responseObject(rds, OK);
responses.createDatabaseInWorkspace = responseObject(rdsSingle, OK);
responses.getDatabaseInWorkspace = responseObject(rds, OK);
responses.testDatabaseConnectionInWorkspace = responseObject({ "result":"Failed to connect to RDS: The connection attempt failed." }, OK);
responses.getPrivatesRds= responseObject(rds, OK);
responses.postPrivateRds= responseObject(rds, OK);
responses.getPrivateRds= responseObject(rds, OK);
responses.listRecipesByWorkspace= responseObject(recipes, OK);
responses.getPublicRecipe= responseObject(recipe, OK);
responses.postPublicRecipe= responseObject(recipe, OK);
responses.postPrivateRecipe= responseObject(recipe, OK);
responses.getRecipeRequestFromName = responseObject(recipes, OK);
responses.getPublicsSecurityGroup = responseObject(secgr, OK);
responses.getRecipeInWorkspace = responseObject(recipe, OK);
responses.getDefaultSecurityRules= responseObject(secrules, OK);
responses.listStackInWorkspaceV4= responseObject({ "responses" :[openstack,aws,azure,gcp]}, OK);
responses.getPublicStack= stackResponses;
responses.getPublicsTemplate= responseObject(templates, OK);
responses.getUserProfile= responseObject(profile, OK);
responses.getRegionsByCredentialId= responseObject(regions, OK);
responses.listStackInWorkspaceV4 = responseObject(stacks, OK);
responses.postStackInWorkspaceV4 = responseObject(openstack, OK);
responses.getStackInWorkspaceV4 = stackResponses;
responses.postStackInWorkspaceV4= responseObject(openstack, OK);
responses.getPlatformSecurityGroups= responseObject(securitygroups, OK);
responses.createManagementPackInWorkspace = responseObject(mpacksSingle, OK);
responses.postPrivateManagementPack = responseObject(mpacks, OK);
responses.getWorkspaces = responseObject(workspaces, OK);
responses.listEnvironment = responseObject(environments, OK);

responses.postRepositoryConfigsValidation= responseObject({
  "utilsBaseURL" : true,
  "ambariGpgKeyUrl" : true,
  "mpackUrl" : true,
  "stackBaseURL" : true,
  "versionDefinitionFileUrl" : true,
  "ambariBaseUrl" : true
}, OK);
responses.getStackMatrixUtil= responseObject(matrix , OK);
responses.getTagSpecifications = responseObject({ "specifications" : { "key" : { "key" : "{}" } } }, OK);
responses.listManagementPacksByWorkspace = responseObject({ "responses": [ {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
}, {
  "public" : false,
  "name" : "name",
  "description" : "description",
  "purge" : false,
  "force" : false,
  "id" : 0,
  "mpackUrl" : "mpackUrl",
  "purgeList" : [ "purgeList", "purgeList" ]
} ] },OK);

responses.putstartStackV2 = stackOperationResponses;
responses.putscalingStackV2 = stackOperationResponses;
responses.putstopStackV2 = stackOperationResponses;
responses.putrepairStackV2 = stackOperationResponses;
responses.putsyncStackV2 = stackOperationResponses;

responses.checkClientVersionV4 = responseObject({ "versionCheckOk" : true, "message" : "message" }, OK);
 
module.exports= {
  responses:responses
}
