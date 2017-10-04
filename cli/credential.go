package cli

import (
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/cli/cloud"
	"github.com/hortonworks/hdc-cli/cli/types"
	"github.com/hortonworks/hdc-cli/cli/utils"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/credentials"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var credentialHeader []string = []string{"ID", "Name"}

type Credential struct {
	ID   int64  `json:"Id" yaml:"Id"`
	Name string `json:"Name" yaml:"Name"`
}

func (c *Credential) DataAsStringArray() []string {
	return []string{strconv.FormatInt(c.ID, 10), c.Name}
}

func CreateAwsCredential(c *cli.Context) error {
	cloud.CurrentCloud = cloud.AWS
	return createCredential(c)
}

func CreateAzureCredential(c *cli.Context) error {
	cloud.CurrentCloud = cloud.AZURE
	return createCredential(c)
}

func CreateGcpCredential(c *cli.Context) error {
	cloud.CurrentCloud = cloud.GCP
	return createCredential(c)
}

func CreateOpenstackCredential(c *cli.Context) error {
	cloud.CurrentCloud = cloud.OPENSTACK
	return createCredential(c)
}

func createCredential(c *cli.Context) error {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "create credential")

	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	createCredentialImpl(c.String, c.Bool, oAuth2Client.Cloudbreak.Credentials)
	return nil
}

type createCredentialClient interface {
	PostPublicCredential(*credentials.PostPublicCredentialParams) (*credentials.PostPublicCredentialOK, error)
	PostPrivateCredential(*credentials.PostPrivateCredentialParams) (*credentials.PostPrivateCredentialOK, error)
}

func createCredentialImpl(stringFinder func(string) string, boolFinder func(string) bool, client createCredentialClient) *models_cloudbreak.CredentialResponse {
	provider := cloud.GetProvider()
	credentialMap, err := provider.CreateCredentialParameters(stringFinder, boolFinder)
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

	log.Infof("[CreateCredential] sending credential create request with name: %s", name)
	var credential *models_cloudbreak.CredentialResponse
	public := boolFinder(FlPublic.Name)
	if public {
		resp, err := client.PostPublicCredential(credentials.NewPostPublicCredentialParams().WithBody(credReq))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		credential = resp.Payload
	} else {
		resp, err := client.PostPrivateCredential(credentials.NewPostPrivateCredentialParams().WithBody(credReq))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		credential = resp.Payload
	}

	log.Infof("[CreateCredential] credential created, id: %d", credential.ID)
	return credential
}

func ListPrivateCredentials(c *cli.Context) error {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "list the private credentials")

	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	listPrivateCredentialsImpl(oAuth2Client.Cloudbreak.Credentials, output.WriteList)
	return nil
}

type getPrivatesCredentialClient interface {
	GetPrivatesCredential(*credentials.GetPrivatesCredentialParams) (*credentials.GetPrivatesCredentialOK, error)
}

func listPrivateCredentialsImpl(client getPrivatesCredentialClient, writer func([]string, []utils.Row)) {
	credResp, err := client.GetPrivatesCredential(credentials.NewGetPrivatesCredentialParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, cred := range credResp.Payload {
		row := &Credential{ID: cred.ID, Name: *cred.Name}
		tableRows = append(tableRows, row)
	}

	writer(credentialHeader, tableRows)
}

func DeleteCredential(c *cli.Context) error {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "delete credential")

	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	name := c.String(FlName.Name)
	log.Infof("[DeleteCredential] delete credential: %s", name)
	if err := oAuth2Client.Cloudbreak.Credentials.DeletePublicCredential(credentials.NewDeletePublicCredentialParams().WithName(name)); err != nil {
		utils.LogErrorAndExit(err)
	}
	return nil
}
