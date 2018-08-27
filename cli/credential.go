package cli

import (
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"time"

	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_organization_id_credentials"

	"errors"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/cloud"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

func CreateAwsCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.AWS)
	parameters := map[string]interface{}{"govCloud": false}
	createCredential(c, parameters)
}

func ModifyAwsCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.AWS)
	parameters := map[string]interface{}{"govCloud": false}
	modifyCredential(c, parameters)
}

func CreateAwsGovCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.AWS)
	parameters := map[string]interface{}{"govCloud": true}
	createCredential(c, parameters)
}

func ModifyAwsGovCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.AWS)
	parameters := map[string]interface{}{"govCloud": true}
	modifyCredential(c, parameters)
}

func CreateAzureCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.AZURE)
	parameters := map[string]interface{}{}
	createCredential(c, parameters)
}

func ModifyAzureCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.AZURE)
	parameters := map[string]interface{}{}
	modifyCredential(c, parameters)
}

func CreateGcpCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.GCP)
	parameters := map[string]interface{}{}
	createCredential(c, parameters)
}

func ModifyGcpCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.GCP)
	parameters := map[string]interface{}{}
	modifyCredential(c, parameters)
}

func CreateOpenstackCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.OPENSTACK)
	parameters := map[string]interface{}{}
	createCredential(c, parameters)
}

func ModifyOpenstackCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.OPENSTACK)
	parameters := map[string]interface{}{}
	modifyCredential(c, parameters)
}

func CreateCredentialFromFile(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "create credential")

	req := assembleCredentialRequest(c.String(FlInputJson.Name), c.String(FlNameOptional.Name))
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	orgID := c.Int64(FlOrganizationOptional.Name)
	postCredential(cbClient.Cloudbreak.V3OrganizationIDCredentials, orgID, req)
}

func ModifyCredentialFromFile(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "modify credential from file")

	credReq := assembleCredentialRequest(c.String(FlInputJson.Name), c.String(FlNameOptional.Name))
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	orgID := c.Int64(FlOrganizationOptional.Name)
	putCredential(cbClient.Cloudbreak.V3OrganizationIDCredentials, orgID, credReq)
}

func assembleCredentialRequest(path, credName string) *models_cloudbreak.CredentialRequest {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[assembleCredentialRequest] read credential json from file: %s", path)
	content := utils.ReadFile(path)

	var req models_cloudbreak.CredentialRequest
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		utils.LogErrorMessageAndExit(msg)
	}

	if len(credName) != 0 {
		req.Name = &credName
	}
	if req.Name == nil || len(*req.Name) == 0 {
		utils.LogErrorMessageAndExit("Name of the credential must be set either in the template or with the --name command line option.")
	}

	return &req
}

func createCredential(c *cli.Context, credentialParameters map[string]interface{}) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "create credential")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	createCredentialImpl(c.String, c.Int64, cbClient.Cloudbreak.V3OrganizationIDCredentials, credentialParameters)
}

func modifyCredential(c *cli.Context, credentialParameters map[string]interface{}) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "modify credential")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	modifyCredentialImpl(c.String, c.Int64, cbClient.Cloudbreak.V3OrganizationIDCredentials, credentialParameters)
}

type modifyCredentialClient interface {
	GetCredentialInOrganization(params *v3_organization_id_credentials.GetCredentialInOrganizationParams) (*v3_organization_id_credentials.GetCredentialInOrganizationOK, error)
	PutCredentialInOrganization(params *v3_organization_id_credentials.PutCredentialInOrganizationParams) (*v3_organization_id_credentials.PutCredentialInOrganizationOK, error)
}

func modifyCredentialImpl(stringFinder func(string) string, int64Finder func(string) int64, client modifyCredentialClient, credentialParameters map[string]interface{}) *models_cloudbreak.CredentialResponse {
	orgID := int64Finder(FlOrganizationOptional.Name)
	name := stringFinder(FlName.Name)
	description := stringFinder(FlDescriptionOptional.Name)
	provider := cloud.GetProvider()

	credential := getCredential(orgID, name, client)
	if *credential.CloudPlatform != *provider.GetName() {
		utils.LogErrorAndExit(errors.New("cloud provider cannot be modified"))
	}

	log.Infof("[modifyCredentialImpl] original credential found name: %s id: %d", name, credential.ID)
	credentialMap, err := provider.GetCredentialParameters(stringFinder)
	for k, v := range credentialParameters {
		credentialMap[k] = v
	}
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	if len(description) == 0 {
		origDesc := credential.Description
		if origDesc != nil && len(*origDesc) > 0 {
			description = *origDesc
		}
	}

	credReq := &models_cloudbreak.CredentialRequest{
		Name:          &name,
		Description:   &description,
		CloudPlatform: provider.GetName(),
		Parameters:    credentialMap,
	}

	return putCredential(client, orgID, credReq)
}

func getCredential(orgID int64, name string, client modifyCredentialClient) *models_cloudbreak.CredentialResponse {
	defer utils.TimeTrack(time.Now(), "get credential")

	log.Infof("[getCredential] get credential by name: %s", name)
	response, err := client.GetCredentialInOrganization(v3_organization_id_credentials.NewGetCredentialInOrganizationParams().WithOrganizationID(orgID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return response.Payload
}

func putCredential(client modifyCredentialClient, orgID int64, credReq *models_cloudbreak.CredentialRequest) *models_cloudbreak.CredentialResponse {
	var credential *models_cloudbreak.CredentialResponse
	log.Infof("[putCredential] modify public credential: %s", *credReq.Name)

	resp, err := client.PutCredentialInOrganization(v3_organization_id_credentials.NewPutCredentialInOrganizationParams().WithOrganizationID(orgID).WithBody(credReq))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	credential = resp.Payload

	return credential
}

type createCredentialClient interface {
	CreateCredentialInOrganization(*v3_organization_id_credentials.CreateCredentialInOrganizationParams) (*v3_organization_id_credentials.CreateCredentialInOrganizationOK, error)
}

func createCredentialImpl(stringFinder func(string) string, int64Finder func(string) int64, client createCredentialClient, credentialParameters map[string]interface{}) *models_cloudbreak.CredentialResponse {
	provider := cloud.GetProvider()
	credentialMap, err := provider.GetCredentialParameters(stringFinder)
	for k, v := range credentialParameters {
		credentialMap[k] = v
	}
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	name := stringFinder(FlName.Name)
	orgID := int64Finder(FlOrganizationOptional.Name)
	credReq := &models_cloudbreak.CredentialRequest{
		Name:          &name,
		Description:   &(&types.S{S: stringFinder(FlDescriptionOptional.Name)}).S,
		CloudPlatform: provider.GetName(),
		Parameters:    credentialMap,
	}
	return postCredential(client, orgID, credReq)
}

func postCredential(client createCredentialClient, orgID int64, credReq *models_cloudbreak.CredentialRequest) *models_cloudbreak.CredentialResponse {
	var credential *models_cloudbreak.CredentialResponse

	log.Infof("[postCredential] sending create public credential request")
	resp, err := client.CreateCredentialInOrganization(v3_organization_id_credentials.NewCreateCredentialInOrganizationParams().WithOrganizationID(orgID).WithBody(credReq))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	credential = resp.Payload

	log.Infof("[postCredential] credential created: %s (id: %d)", *credential.Name, credential.ID)
	return credential
}

type credentialOutDescribe struct {
	*cloudResourceOut
	ID string `json:"ID" yaml:"ID"`
}

func (c *credentialOutDescribe) DataAsStringArray() []string {
	return append(c.cloudResourceOut.DataAsStringArray(), c.ID)
}

func DescribeCredential(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe credential")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	orgID := c.Int64(FlOrganizationOptional.Name)
	resp, err := cbClient.Cloudbreak.V3OrganizationIDCredentials.GetCredentialInOrganization(v3_organization_id_credentials.NewGetCredentialInOrganizationParams().WithOrganizationID(orgID).WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	cred := resp.Payload
	output.Write(append(cloudResourceHeader, "ID"), &credentialOutDescribe{&cloudResourceOut{*cred.Name, *cred.Description, *cred.CloudPlatform}, strconv.FormatInt(cred.ID, 10)})
}

func DeleteCredential(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete credential")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	orgID := c.Int64(FlOrganizationOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[DeleteCredential] sending delete credential request with name: %s", name)
	if _, err := cbClient.Cloudbreak.V3OrganizationIDCredentials.DeleteCredentialInOrganization(v3_organization_id_credentials.NewDeleteCredentialInOrganizationParams().WithOrganizationID(orgID).WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteCredential] credential deleted, name: %s", name)
}

func ListCredentials(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list credentials")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	orgID := c.Int64(FlOrganizationOptional.Name)
	listCredentialsImpl(cbClient.Cloudbreak.V3OrganizationIDCredentials, orgID, output.WriteList)
}

type listCredentialsByOrganizationClient interface {
	ListCredentialsByOrganization(*v3_organization_id_credentials.ListCredentialsByOrganizationParams) (*v3_organization_id_credentials.ListCredentialsByOrganizationOK, error)
}

func listCredentialsImpl(client listCredentialsByOrganizationClient, orgID int64, writer func([]string, []utils.Row)) {
	log.Infof("[listCredentialsImpl] sending credential list request")
	credResp, err := client.ListCredentialsByOrganization(v3_organization_id_credentials.NewListCredentialsByOrganizationParams().WithOrganizationID(orgID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, cred := range credResp.Payload {
		tableRows = append(tableRows, &cloudResourceOut{*cred.Name, *cred.Description, GetPlatformName(cred)})
	}

	writer(cloudResourceHeader, tableRows)
}

func GetPlatformName(credRes *models_cloudbreak.CredentialResponse) string {
	if credRes != nil && credRes.Parameters["govCloud"] == "true" {
		return *credRes.CloudPlatform + "_GOV"
	}
	return *credRes.CloudPlatform
}
