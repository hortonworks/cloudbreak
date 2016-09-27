package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/credentials"
	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"
	"io/ioutil"
	"net/http"
	"strconv"
	"sync"
	"time"
)

func CreateCredential(c *cli.Context) error {
	checkRequiredFlags(c, CreateCredential)

	go StartSpinner()

	sshKeyURL := c.String(FlSSHKeyURL.Name)
	log.Infof("[CreateCredential] sending GET request for SSH key to %s", sshKeyURL)
	client := &http.Client{Transport: TransportConfig}
	resp, err := client.Get(sshKeyURL)
	if err != nil {
		logErrorAndExit(CreateCredential, err.Error())
	}
	sshKey, _ := ioutil.ReadAll(resp.Body)
	log.Infof("[CreateCredential] SSH key recieved: %s", sshKey)

	var credentialMap = make(map[string]interface{})
	credentialMap["roleArn"] = c.String(FlRoleARN.Name)
	defaultCredential := models.CredentialResponse{
		Parameters: credentialMap,
		PublicKey:  string(sshKey),
	}

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	oAuth2Client.CreateCredential(c.String(FlCredentialName.Name), defaultCredential, c.String(FlSSHKeyPair.Name))
	return nil
}

func (c *Cloudbreak) CopyDefaultCredential(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	defaultCred := c.GetCredential("aws-access")
	credentialName := "cred" + strconv.FormatInt(time.Now().UnixNano(), 10)
	channel <- c.CreateCredential(credentialName, defaultCred, skeleton.SSHKeyName)
	wg.Done()
}

func (c *Cloudbreak) CreateCredential(name string, defaultCredential models.CredentialResponse, existingKey string) int64 {
	defer timeTrack(time.Now(), "create credential")
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

	log.Infof("[CreateCredential] sending credential create request with name: %s", name)
	resp, err := c.Cloudbreak.Credentials.PostCredentialsAccount(&credentials.PostCredentialsAccountParams{&credReq})

	if err != nil {
		logErrorAndExit(c.CreateCredential, err.Error())
	}

	log.Infof("[CreateCredential] credential created, id: %d", resp.Payload.ID)
	return resp.Payload.ID
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
		logErrorAndExit(c.GetCredential, err.Error())
	}

	awsAccess := *resp.Payload
	log.Infof("[GetCredential] found credential, name: %s id: %d", awsAccess.Name, awsAccess.ID)
	return awsAccess
}

func (c *Cloudbreak) GetPublicCredentials() []*models.CredentialResponse {
	defer timeTrack(time.Now(), "get public credentials")
	resp, err := c.Cloudbreak.Credentials.GetCredentialsAccount(&credentials.GetCredentialsAccountParams{})
	if err != nil {
		logErrorAndExit(c.GetPublicCredentials, err.Error())
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteCredential(name string) error {
	defer timeTrack(time.Now(), "delete credential")
	log.Infof("[DeleteCredential] delete credential: %s", name)
	return c.Cloudbreak.Credentials.DeleteCredentialsAccountName(&credentials.DeleteCredentialsAccountNameParams{Name: name})
}
