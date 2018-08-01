package cli

import (
	"encoding/json"
	"fmt"
	"os"
	"time"

	"errors"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/cloud"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1credentials"
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
	postCredential(cbClient.Cloudbreak.V1credentials, c.Bool(FlPublicOptional.Name), req)
}

func ModifyCredentialFromFile(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "modify credential from file")

	credReq := assembleCredentialRequest(c.String(FlInputJson.Name), c.String(FlNameOptional.Name))
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	credential := getCredential(*credReq.Name, cbClient.Cloudbreak.V1credentials)
	putCredential(cbClient.Cloudbreak.V1credentials, *credential.Public, credReq)
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
	createCredentialImpl(c.String, c.Bool, cbClient.Cloudbreak.V1credentials, credentialParameters)
}

func modifyCredential(c *cli.Context, credentialParameters map[string]interface{}) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "modify credential")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	modifyCredentialImpl(c.String, c.Bool, cbClient.Cloudbreak.V1credentials, credentialParameters)
}

type modifyCredentialClient interface {
	PutPrivateCredential(params *v1credentials.PutPrivateCredentialParams) (*v1credentials.PutPrivateCredentialOK, error)
	PutPublicCredential(params *v1credentials.PutPublicCredentialParams) (*v1credentials.PutPublicCredentialOK, error)
	GetPublicCredential(params *v1credentials.GetPublicCredentialParams) (*v1credentials.GetPublicCredentialOK, error)
}

func modifyCredentialImpl(stringFinder func(string) string, boolFinder func(string) bool, client modifyCredentialClient, credentialParameters map[string]interface{}) *models_cloudbreak.CredentialResponse {
	name := stringFinder(FlName.Name)
	description := stringFinder(FlDescriptionOptional.Name)
	provider := cloud.GetProvider()

	credential := getCredential(name, client)
	if *credential.CloudPlatform != *provider.GetName() {
		utils.LogErrorAndExit(errors.New("cloud provider cannot be modified"))
	}

	log.Infof("[modifyCredentialImpl] original credential found name: %s id: %d", name, credential.ID)
	credentialMap, err := provider.GetCredentialParameters(stringFinder, boolFinder)
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

	return putCredential(client, *credential.Public, credReq)
}

func getCredential(name string, client modifyCredentialClient) *models_cloudbreak.CredentialResponse {
	defer utils.TimeTrack(time.Now(), "get credential")

	log.Infof("[getCredential] get credential by name: %s", name)
	response, err := client.GetPublicCredential(v1credentials.NewGetPublicCredentialParams().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return response.Payload
}

func putCredential(client modifyCredentialClient, public bool, credReq *models_cloudbreak.CredentialRequest) *models_cloudbreak.CredentialResponse {
	var credential *models_cloudbreak.CredentialResponse
	if public {
		log.Infof("[putCredential] modify public credential: %s", *credReq.Name)
		resp, err := client.PutPublicCredential(v1credentials.NewPutPublicCredentialParams().WithBody(credReq))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		credential = resp.Payload
	} else {
		log.Infof("[putCredential] modify private credential: %s", *credReq.Name)
		resp, err := client.PutPrivateCredential(v1credentials.NewPutPrivateCredentialParams().WithBody(credReq))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		credential = resp.Payload
	}
	return credential
}

type createCredentialClient interface {
	PostPublicCredential(*v1credentials.PostPublicCredentialParams) (*v1credentials.PostPublicCredentialOK, error)
	PostPrivateCredential(*v1credentials.PostPrivateCredentialParams) (*v1credentials.PostPrivateCredentialOK, error)
}

func createCredentialImpl(stringFinder func(string) string, boolFinder func(string) bool, client createCredentialClient, credentialParameters map[string]interface{}) *models_cloudbreak.CredentialResponse {
	provider := cloud.GetProvider()
	credentialMap, err := provider.GetCredentialParameters(stringFinder, boolFinder)
	for k, v := range credentialParameters {
		credentialMap[k] = v
	}
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	name := stringFinder(FlName.Name)
	credReq := &models_cloudbreak.CredentialRequest{
		Name:          &name,
		Description:   &(&types.S{S: stringFinder(FlDescriptionOptional.Name)}).S,
		CloudPlatform: provider.GetName(),
		Parameters:    credentialMap,
	}
	public := boolFinder(FlPublicOptional.Name)
	return postCredential(client, public, credReq)
}

func postCredential(client createCredentialClient, public bool, credReq *models_cloudbreak.CredentialRequest) *models_cloudbreak.CredentialResponse {
	var credential *models_cloudbreak.CredentialResponse
	if public {
		log.Infof("[postCredential] sending create public credential request")
		resp, err := client.PostPublicCredential(v1credentials.NewPostPublicCredentialParams().WithBody(credReq))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		credential = resp.Payload
	} else {
		log.Infof("[postCredential] sending create private credential request")
		resp, err := client.PostPrivateCredential(v1credentials.NewPostPrivateCredentialParams().WithBody(credReq))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		credential = resp.Payload
	}
	log.Infof("[postCredential] credential created: %s (id: %d)", *credential.Name, credential.ID)
	return credential
}

func DescribeCredential(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe credential")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	resp, err := cbClient.Cloudbreak.V1credentials.GetPublicCredential(v1credentials.NewGetPublicCredentialParams().WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	cred := resp.Payload
	output.Write(cloudResourceHeader, &cloudResourceOut{*cred.Name, *cred.Description, *cred.CloudPlatform})
}

func DeleteCredential(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete credential")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	name := c.String(FlName.Name)
	log.Infof("[DeleteCredential] sending delete credential request with name: %s", name)
	if err := cbClient.Cloudbreak.V1credentials.DeletePublicCredential(v1credentials.NewDeletePublicCredentialParams().WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteCredential] credential deleted, name: %s", name)
}

func ListCredentials(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list credentials")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	listCredentialsImpl(cbClient.Cloudbreak.V1credentials, output.WriteList)
}

type getPublicsCredentialClient interface {
	GetPublicsCredential(*v1credentials.GetPublicsCredentialParams) (*v1credentials.GetPublicsCredentialOK, error)
}

func listCredentialsImpl(client getPublicsCredentialClient, writer func([]string, []utils.Row)) {
	log.Infof("[listCredentialsImpl] sending credential list request")
	credResp, err := client.GetPublicsCredential(v1credentials.NewGetPublicsCredentialParams())
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
