package cli

import (
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/cloud"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/credentials"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

func CreateAwsCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.AWS)
	createCredential(c)
}

func CreateAzureCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.AZURE)
	createCredential(c)
}

func CreateGcpCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.GCP)
	createCredential(c)
}

func CreateOpenstackCredential(c *cli.Context) {
	cloud.SetProviderType(cloud.OPENSTACK)
	createCredential(c)
}

func createCredential(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "create credential")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	createCredentialImpl(c.String, c.Bool, cbClient.Cloudbreak.Credentials)
}

type createCredentialClient interface {
	PostPublicCredential(*credentials.PostPublicCredentialParams) (*credentials.PostPublicCredentialOK, error)
	PostPrivateCredential(*credentials.PostPrivateCredentialParams) (*credentials.PostPrivateCredentialOK, error)
}

func createCredentialImpl(stringFinder func(string) string, boolFinder func(string) bool, client createCredentialClient) *models_cloudbreak.CredentialResponse {
	provider := cloud.GetProvider()
	credentialMap, err := provider.GetCredentialParameters(stringFinder, boolFinder)
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	name := stringFinder(FlName.Name)
	credReq := &models_cloudbreak.CredentialRequest{
		Name:          &name,
		Description:   &(&types.S{S: stringFinder(FlDescription.Name)}).S,
		CloudPlatform: provider.GetName(),
		Parameters:    credentialMap,
	}

	var credential *models_cloudbreak.CredentialResponse
	public := boolFinder(FlPublic.Name)
	if public {
		log.Infof("[createCredentialImpl] sending create public credential request")
		resp, err := client.PostPublicCredential(credentials.NewPostPublicCredentialParams().WithBody(credReq))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		credential = resp.Payload
	} else {
		log.Infof("[createCredentialImpl] sending create private credential request")
		resp, err := client.PostPrivateCredential(credentials.NewPostPrivateCredentialParams().WithBody(credReq))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		credential = resp.Payload
	}
	log.Infof("[createCredentialImpl] credential created: %s (id: %d)", *credential.Name, credential.ID)
	return credential
}

func DescribeCredential(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "describe credential")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	resp, err := cbClient.Cloudbreak.Credentials.GetPublicCredential(credentials.NewGetPublicCredentialParams().WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	cred := resp.Payload
	output.Write(cloudResourceHeader, &cloudResourceOut{*cred.Name, *cred.Description, *cred.CloudPlatform})
}

func DeleteCredential(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "delete credential")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	name := c.String(FlName.Name)
	log.Infof("[DeleteCredential] sending delete credential request with name: %s", name)
	if err := cbClient.Cloudbreak.Credentials.DeletePublicCredential(credentials.NewDeletePublicCredentialParams().WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteCredential] credential deleted, name: %s", name)
}

func ListCredentials(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "list credentials")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	listCredentialsImpl(cbClient.Cloudbreak.Credentials, output.WriteList)
}

type getPublicsCredentialClient interface {
	GetPublicsCredential(*credentials.GetPublicsCredentialParams) (*credentials.GetPublicsCredentialOK, error)
}

func listCredentialsImpl(client getPublicsCredentialClient, writer func([]string, []utils.Row)) {
	log.Infof("[listCredentialsImpl] sending credential list request")
	credResp, err := client.GetPublicsCredential(credentials.NewGetPublicsCredentialParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, cred := range credResp.Payload {
		tableRows = append(tableRows, &cloudResourceOut{*cred.Name, *cred.Description, *cred.CloudPlatform})
	}

	writer(cloudResourceHeader, tableRows)
}
