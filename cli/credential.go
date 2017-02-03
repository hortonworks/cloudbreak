package cli

import (
	"io/ioutil"
	"net/http"
	"strconv"
	"sync"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/credentials"
	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"
)

var CredentialHeader []string = []string{"ID", "Name"}

type Credential struct {
	Id   int64  `json:"Id" yaml:"Id"`
	Name string `json:"Name" yaml:"Name"`
}

func (c *Credential) DataAsStringArray() []string {
	return []string{strconv.FormatInt(c.Id, 10), c.Name}
}

func CreateCredential(c *cli.Context) error {
	checkRequiredFlags(c)

	go StartSpinner()

	client := &http.Client{Transport: TransportConfig}
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	createCredential(c.String, client.Get, oAuth2Client.CreateCredential)
	return nil
}

func createCredential(finder func(string) string, getResponse func(string) (*http.Response, error), createCredential func(string, models.CredentialResponse, string, bool) int64) int64 {
	sshKeyURL := finder(FlSSHKeyURL.Name)
	log.Infof("[CreateCredential] sending GET request for SSH key to %s", sshKeyURL)

	resp, err := getResponse(sshKeyURL)
	if err != nil {
		logErrorAndExit(err)
	}
	sshKey, _ := ioutil.ReadAll(resp.Body)
	log.Infof("[CreateCredential] SSH key recieved: %s", sshKey)

	var credentialMap = make(map[string]interface{})
	credentialMap["roleArn"] = finder(FlRoleARN.Name)
	defaultCredential := models.CredentialResponse{
		Parameters: credentialMap,
		PublicKey:  string(sshKey),
	}

	return createCredential(finder(FlCredentialName.Name), defaultCredential, finder(FlSSHKeyPair.Name), true)
}

func DeleteCredential(c *cli.Context) error {
	checkRequiredFlags(c)
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	if err := oAuth2Client.DeleteCredential(c.String(FlCredentialName.Name)); err != nil {
		logErrorAndExit(err)
	}
	return nil
}

func (c *Cloudbreak) CopyDefaultCredential(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	defer wg.Done()

	copyDefaultCredentialImpl(skeleton, channel, c.GetCredential, c.CreateCredential)
}

func copyDefaultCredentialImpl(skeleton ClusterSkeleton, channel chan int64, getCred func(string) models.CredentialResponse, createCred func(string, models.CredentialResponse, string, bool) int64) {
	defaultCred := getCred("aws-access")
	credentialName := "cred" + strconv.FormatInt(time.Now().UnixNano(), 10)
	channel <- createCred(credentialName, defaultCred, skeleton.SSHKeyName, true)
}

func (c *Cloudbreak) CreateCredential(name string, defaultCredential models.CredentialResponse, existingKey string, public bool) int64 {
	defer timeTrack(time.Now(), "create credential")

	return createCredentialImpl(name, defaultCredential, existingKey, public, c.Cloudbreak.Credentials.PostCredentialsAccount, c.Cloudbreak.Credentials.PostCredentialsUser)
}

func createCredentialImpl(name string, defaultCredential models.CredentialResponse, existingKey string, public bool,
	postAccountCredential func(*credentials.PostCredentialsAccountParams) (*credentials.PostCredentialsAccountOK, error),
	postUserCredential func(*credentials.PostCredentialsUserParams) (*credentials.PostCredentialsUserOK, error)) int64 {

	credReq := createCredentialRequest(name, defaultCredential, existingKey)

	log.Infof("[CreateCredential] sending credential create request with name: %s", name)
	var id int64
	if public {
		resp, err := postAccountCredential(&credentials.PostCredentialsAccountParams{Body: credReq})
		if err != nil {
			logErrorAndExit(err)
		}
		id = *resp.Payload.ID
	} else {
		resp, err := postUserCredential(&credentials.PostCredentialsUserParams{Body: credReq})
		if err != nil {
			logErrorAndExit(err)
		}
		id = *resp.Payload.ID
	}

	log.Infof("[CreateCredential] credential created, id: %d", id)
	return id
}

func createCredentialRequest(name string, defaultCredential models.CredentialResponse, existingKey string) *models.CredentialRequest {
	var credentialMap = make(map[string]interface{})
	credentialMap["selector"] = "role-based"
	credentialMap["roleArn"] = defaultCredential.Parameters["roleArn"]
	credentialMap["existingKeyPairName"] = existingKey

	credReq := models.CredentialRequest{
		Name:          name,
		CloudPlatform: "AWS",
		PublicKey:     defaultCredential.PublicKey,
		Parameters:    credentialMap,
	}

	return &credReq
}

func (c *Cloudbreak) GetCredentialById(credentialID int64) (*models.CredentialResponse, error) {
	defer timeTrack(time.Now(), "get credential by id")
	respCredential, err := c.Cloudbreak.Credentials.GetCredentialsID(&credentials.GetCredentialsIDParams{ID: credentialID})
	var credential *models.CredentialResponse
	if respCredential != nil {
		credential = respCredential.Payload
	}
	return credential, err
}

func (c *Cloudbreak) GetCredential(name string) models.CredentialResponse {
	defer timeTrack(time.Now(), "get credential by name")
	log.Infof("[GetCredential] sending get request to find credential with name: %s", name)
	resp, err := c.Cloudbreak.Credentials.GetCredentialsUserName(&credentials.GetCredentialsUserNameParams{Name: name})

	if err != nil {
		logErrorAndExit(err)
	}

	awsAccess := *resp.Payload
	log.Infof("[GetCredential] found credential, name: %s id: %d", awsAccess.Name, awsAccess.ID)
	return awsAccess
}

func (c *Cloudbreak) GetPublicCredentials() []*models.CredentialResponse {
	defer timeTrack(time.Now(), "get public credentials")
	resp, err := c.Cloudbreak.Credentials.GetCredentialsAccount(&credentials.GetCredentialsAccountParams{})
	if err != nil {
		logErrorAndExit(err)
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteCredential(name string) error {
	defer timeTrack(time.Now(), "delete credential")
	log.Infof("[DeleteCredential] delete credential: %s", name)
	return c.Cloudbreak.Credentials.DeleteCredentialsAccountName(&credentials.DeleteCredentialsAccountNameParams{Name: name})
}

func ListPrivateCredentials(c *cli.Context) error {
	checkRequiredFlags(c)
	defer timeTrack(time.Now(), "list the private credentials")

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	output := Output{Format: c.String(FlOutput.Name)}
	listPrivateCredentialsImpl(oAuth2Client.GetPrivateCredentials, output.WriteList)
	return nil
}

func listPrivateCredentialsImpl(getPrivateCredentials func() []*models.CredentialResponse, writer func([]string, []Row)) {
	credResp := getPrivateCredentials()

	var tableRows []Row
	for _, cred := range credResp {
		row := &Credential{Id: *cred.ID, Name: cred.Name}
		tableRows = append(tableRows, row)
	}

	writer(CredentialHeader, tableRows)
}

func (c *Cloudbreak) GetPrivateCredentials() []*models.CredentialResponse {
	defer timeTrack(time.Now(), "get private credentials")
	resp, err := c.Cloudbreak.Credentials.GetCredentialsUser(&credentials.GetCredentialsUserParams{})
	if err != nil {
		logErrorAndExit(err)
	}
	return resp.Payload
}
