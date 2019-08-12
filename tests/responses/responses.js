var responses={};

var deploymentInfo = require('./accountpreferences/accountpreferences.json');

var defaultblueprint = require('./blueprints/default-blueprint.json');
var qablueprint = require('./blueprints/qa-blueprint.json');
var serviceversions = require('./blueprints/servicesversions.json');

var filesystems = require('./filesystems/filesystems.json');

var environments = require('./environments/environments.json');
var environmentAws = require('./environments/amazon.json');

var imagecatalog = require('./imagecatalogs/default-imagecatalog.json');
var qaimages = require('./imagecatalogs/qa-images.json');

var regionsAws = require('./connectors/regions-aws.json');
var sshkeys = require('./connectors/sshkeys.json');
var accessConfigs = require('./connectors/accessconfigs.json');
var connetworks = require('./connectors/networks.json');
var securitygroups = require('./connectors/securitygroups.json');
var encryptionkeys = require('./connectors/encryptionskeys.json');
var disktypes = require('./connectors/disktypes.json');
var tagspecifications = require('./connectors/tagspecifications.json');
var recommendations = require('./connectors/recommendations.json');

var credentials = require('./credentials/credentials.json');
var prerequisites = require('./credentials/prerequisites');

var rights = require('./utils/rights.json');
var stackmatrix = require('./utils/stackmatrix.json');

var recipes = require('./recipes/recipes.json');

const OK = 200;

var responseObject = function(payload, code) {
	return { responses : [ { response: payload , statusCode: code } ] };
}

responses.getCloudbreakInfo = responseObject({ 'app': { 'name':'cloudbreak', 'version':'MOCK' } }, OK);
responses.getCloudbreakHealth = responseObject({ 'status':'UP' }, OK);

responses.getEnvironmentInfo = responseObject({ 'app': {'name':'environment','version':'MOCK' } }, OK);
responses.getEnvironmentHealth = responseObject({ 'status': 'UP' }, OK);
responses.getDatalakeInfo = responseObject({ 'app': {'name':'datalake','version':'MOCK' } }, OK);
responses.getDatalakeHealth = responseObject({ 'status': 'UP' }, OK);
responses.getAutoscaleInfo = responseObject({ 'app': { 'name': 'autoscale', 'version': 'MOCK', 'capabilities': '' } }, OK);
responses.getAutoscaleHealth = responseObject({ 'status': 'UP' }, OK);

responses.listClusterTemplatesByWorkspace = responseObject({ 'responses': [] }, OK);

responses.evictCurrentUserDetails = responseObject({ 'username': 'mock@hortonworks.com' }, OK);
responses.getAuditEventsInWorkspace = responseObject([], OK);
responses.getDeploymentInfo = responseObject(deploymentInfo, OK);

responses.listImageCatalogsByWorkspace = responseObject(imagecatalog, OK);
responses.getImageCatalogInWorkspace = responseObject(imagecatalog, OK);
responses.createImageCatalogInWorkspace = responseObject(imagecatalog, OK);
responses.getImageCatalogRequestFromNameInWorkspace = responseObject(imagecatalog, OK);
responses.getImagesByNameInWorkspace = responseObject(qaimages, OK);

responses.listRecipesByWorkspace = responseObject(recipes, OK);

responses.getStackMatrixUtilV4 = responseObject(stackmatrix, OK);
responses.checkRight = responseObject(rights, OK);

responses.listBlueprintsByWorkspace = responseObject(qablueprint, OK);
responses.getBlueprintInWorkspace = responseObject(defaultblueprint, OK);
responses.createBlueprintInWorkspace= responseObject(defaultblueprint, OK);
responses.getServiceVersionsByBlueprintName = responseObject(serviceversions, OK);
responses.createRecommendationForWorkspace = responseObject(recommendations, OK);
responses.getServiceList = responseObject({ supportedVersions: [] }, OK);

responses.getFileSystemParameters = responseObject(filesystems, OK);

responses.listEnvironmentV1 = responseObject(environments, OK);
responses.getEnvironmentV1ByName = responseObject(environmentAws, OK);

responses.listSdx = responseObject([], OK);

responses.listCredentialsV1 = responseObject(credentials, OK);
responses.getPrerequisitesForCloudPlatform = responseObject(prerequisites, OK);

responses.listDistroXV1 = responseObject({ responses: [] }, OK);

responses.getRegionsByCredential = responseObject(regionsAws, OK);
responses.getPlatformSShKeys = responseObject(sshkeys, OK);
responses.getAccessConfigs = responseObject(accessConfigs, OK);
responses.getPlatformNetworks = responseObject(connetworks, OK);
responses.getPlatformSecurityGroups = responseObject(securitygroups, OK);
responses.getEncryptionKeys = responseObject(encryptionkeys, OK);
responses.getDisktypes = responseObject(disktypes, OK);
responses.getTagSpecifications = responseObject(tagspecifications, OK);

module.exports= {
  responses:responses
}
