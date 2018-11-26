package credential

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/hortonworks/cb-cli/cloudbreak/common"
	"github.com/hortonworks/cb-cli/cloudbreak/oauth"
	"os"
	"strconv"
	"time"

	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v3_workspace_id_credentials"

	"errors"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	"github.com/hortonworks/cb-cli/cloudbreak/cloud"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/types"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var AwsPrerequisiteOutputHeader = []string{"Account Id", "CloudPlatform", "External Id", "Policy JSON"}

type awsPrerequisiteOutput struct {
	AccountId     string `json:"AccountId" yaml:"AccountId"`
	CloudPlatform string `json:"CloudPlatform" yaml:"CloudPlatform"`
	ExternalId    string `json:"ExternalId" yaml:"ExternalId"`
	PolicyJSON    string `json:"PolicyJSON" yaml:"PolicyJSON"`
}

func (p *awsPrerequisiteOutput) DataAsStringArray() []string {
	var raw map[string]interface{}
	err := json.Unmarshal([]byte(p.PolicyJSON), &raw)
	if err != nil {
		return []string{p.CloudPlatform, p.AccountId, p.ExternalId, string(err.Error())}
	}

	policyJSON, err := json.MarshalIndent(raw, "", "  ")
	if err != nil {
		return []string{p.CloudPlatform, p.AccountId, p.ExternalId, string(err.Error())}
	}
	return []string{p.AccountId, p.CloudPlatform, p.ExternalId, string(policyJSON)}
}

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
	defer utils.TimeTrack(time.Now(), "create credential")

	req := assembleCredentialRequest(c.String(fl.FlInputJson.Name), c.String(fl.FlNameOptional.Name))
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	postCredential(cbClient.Cloudbreak.V3WorkspaceIDCredentials, workspaceID, req)
}

func ModifyCredentialFromFile(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "modify credential from file")

	credReq := assembleCredentialRequest(c.String(fl.FlInputJson.Name), c.String(fl.FlNameOptional.Name))
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	putCredential(cbClient.Cloudbreak.V3WorkspaceIDCredentials, workspaceID, credReq)
}

func assembleCredentialRequest(path, credName string) *model.CredentialRequest {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[assembleCredentialRequest] read credential json from file: %s", path)
	content := utils.ReadFile(path)

	var req model.CredentialRequest
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
	defer utils.TimeTrack(time.Now(), "create credential")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	createCredentialImpl(c.String, c.Int64, cbClient.Cloudbreak.V3WorkspaceIDCredentials, credentialParameters)
}

func modifyCredential(c *cli.Context, credentialParameters map[string]interface{}) {
	defer utils.TimeTrack(time.Now(), "modify credential")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	modifyCredentialImpl(c.String, c.Int64, cbClient.Cloudbreak.V3WorkspaceIDCredentials, credentialParameters)
}

type modifyCredentialClient interface {
	GetCredentialInWorkspace(params *v3_workspace_id_credentials.GetCredentialInWorkspaceParams) (*v3_workspace_id_credentials.GetCredentialInWorkspaceOK, error)
	PutCredentialInWorkspace(params *v3_workspace_id_credentials.PutCredentialInWorkspaceParams) (*v3_workspace_id_credentials.PutCredentialInWorkspaceOK, error)
}

func modifyCredentialImpl(stringFinder func(string) string, int64Finder func(string) int64, client modifyCredentialClient, credentialParameters map[string]interface{}) *model.CredentialResponse {
	workspaceID := int64Finder(fl.FlWorkspaceOptional.Name)
	name := stringFinder(fl.FlName.Name)
	description := stringFinder(fl.FlDescriptionOptional.Name)
	provider := cloud.GetProvider()

	credential := getCredential(workspaceID, name, client)
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

	credReq := &model.CredentialRequest{
		Name:          &name,
		Description:   &description,
		CloudPlatform: provider.GetName(),
		Parameters:    credentialMap,
	}

	return putCredential(client, workspaceID, credReq)
}

func getCredential(workspaceID int64, name string, client modifyCredentialClient) *model.CredentialResponse {
	defer utils.TimeTrack(time.Now(), "get credential")

	log.Infof("[getCredential] get credential by name: %s", name)
	response, err := client.GetCredentialInWorkspace(v3_workspace_id_credentials.NewGetCredentialInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return response.Payload
}

func putCredential(client modifyCredentialClient, workspaceID int64, credReq *model.CredentialRequest) *model.CredentialResponse {
	var credential *model.CredentialResponse
	log.Infof("[putCredential] modify public credential: %s", *credReq.Name)

	resp, err := client.PutCredentialInWorkspace(v3_workspace_id_credentials.NewPutCredentialInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(credReq))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	credential = resp.Payload

	return credential
}

type createCredentialClient interface {
	CreateCredentialInWorkspace(*v3_workspace_id_credentials.CreateCredentialInWorkspaceParams) (*v3_workspace_id_credentials.CreateCredentialInWorkspaceOK, error)
}

func createCredentialImpl(stringFinder func(string) string, int64Finder func(string) int64, client createCredentialClient, credentialParameters map[string]interface{}) *model.CredentialResponse {
	provider := cloud.GetProvider()
	credentialMap, err := provider.GetCredentialParameters(stringFinder)
	for k, v := range credentialParameters {
		credentialMap[k] = v
	}
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	name := stringFinder(fl.FlName.Name)
	workspaceID := int64Finder(fl.FlWorkspaceOptional.Name)
	credReq := &model.CredentialRequest{
		Name:          &name,
		Description:   &(&types.S{S: stringFinder(fl.FlDescriptionOptional.Name)}).S,
		CloudPlatform: provider.GetName(),
		Parameters:    credentialMap,
	}
	return postCredential(client, workspaceID, credReq)
}

func postCredential(client createCredentialClient, workspaceID int64, credReq *model.CredentialRequest) *model.CredentialResponse {
	var credential *model.CredentialResponse

	log.Infof("[postCredential] sending create public credential request")
	resp, err := client.CreateCredentialInWorkspace(v3_workspace_id_credentials.NewCreateCredentialInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(credReq))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	credential = resp.Payload

	log.Infof("[postCredential] credential created: %s (id: %d)", *credential.Name, credential.ID)
	return credential
}

type credentialOutDescribe struct {
	*common.CloudResourceOut
	ID string `json:"ID" yaml:"ID"`
}

func (c *credentialOutDescribe) DataAsStringArray() []string {
	return append(c.CloudResourceOut.DataAsStringArray(), c.ID)
}

func DescribeCredential(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe credential")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	resp, err := cbClient.Cloudbreak.V3WorkspaceIDCredentials.GetCredentialInWorkspace(v3_workspace_id_credentials.NewGetCredentialInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(c.String(fl.FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	cred := resp.Payload
	output.Write(append(common.CloudResourceHeader, "ID"), &credentialOutDescribe{&common.CloudResourceOut{*cred.Name, *cred.Description, *cred.CloudPlatform}, strconv.FormatInt(cred.ID, 10)})
}

func DeleteCredential(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete credential")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	name := c.String(fl.FlName.Name)
	log.Infof("[DeleteCredential] sending delete credential request with name: %s", name)
	if _, err := cbClient.Cloudbreak.V3WorkspaceIDCredentials.DeleteCredentialInWorkspace(v3_workspace_id_credentials.NewDeleteCredentialInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteCredential] credential deleted, name: %s", name)
}

func ListCredentials(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list credentials")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	listCredentialsImpl(cbClient.Cloudbreak.V3WorkspaceIDCredentials, workspaceID, output.WriteList)
}

type listCredentialsByWorkspaceClient interface {
	ListCredentialsByWorkspace(*v3_workspace_id_credentials.ListCredentialsByWorkspaceParams) (*v3_workspace_id_credentials.ListCredentialsByWorkspaceOK, error)
}

func listCredentialsImpl(client listCredentialsByWorkspaceClient, workspaceID int64, writer func([]string, []utils.Row)) {
	log.Infof("[listCredentialsImpl] sending credential list request")
	credResp, err := client.ListCredentialsByWorkspace(v3_workspace_id_credentials.NewListCredentialsByWorkspaceParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, cred := range credResp.Payload {
		tableRows = append(tableRows, &common.CloudResourceOut{*cred.Name, *cred.Description, GetPlatformName(cred)})
	}

	writer(common.CloudResourceHeader, tableRows)
}

func GetPlatformName(credRes *model.CredentialResponse) string {
	if credRes != nil && credRes.Parameters["govCloud"] == "true" {
		return *credRes.CloudPlatform + "_GOV"
	}
	return *credRes.CloudPlatform
}

func GetAwsCredentialPrerequisites(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "get credentials prerequisites for Aws")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)

	log.Infof("[GetAwsCredentialPrerequisites] sending Aws credential prerequisites request")
	prerequisitesForCloudPlatformParams := v3_workspace_id_credentials.NewGetPrerequisitesForCloudPlatformParams().WithWorkspaceID(workspaceID).WithCloudPlatform("aws")
	credPrereqResp, err := cbClient.Cloudbreak.V3WorkspaceIDCredentials.GetPrerequisitesForCloudPlatform(prerequisitesForCloudPlatformParams)
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	prerequisites := credPrereqResp.Payload
	output.Write(AwsPrerequisiteOutputHeader, convertAwsPrerequistiesToJSON(prerequisites))
}

func convertAwsPrerequistiesToJSON(prerequisites *model.CredentialPrerequisites) *awsPrerequisiteOutput {
	policyJsonEncoded := utils.SafeStringConvert(prerequisites.Aws.PolicyJSON)
	policyJson, err := base64.StdEncoding.DecodeString(policyJsonEncoded)
	if err != nil {
		utils.LogErrorMessageAndExit("Could not parse AWS cb-policy.json from the response.")
	}
	return &awsPrerequisiteOutput{
		AccountId:     utils.SafeStringConvert(prerequisites.AccountID),
		CloudPlatform: utils.SafeStringConvert(prerequisites.CloudPlatform),
		ExternalId:    utils.SafeStringConvert(prerequisites.Aws.ExternalID),
		PolicyJSON:    string(policyJson),
	}
}
