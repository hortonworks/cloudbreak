package cli

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/credentials"
	"github.com/hortonworks/hdc-cli/models"
	"strconv"
	"sync"
	"time"
)

func (c *Cloudbreak) CopyDefaultCredential(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	defaultCred := c.GetCredential("aws-access")
	channel <- c.CreateCredential(defaultCred, skeleton.SSHKeyName)
	wg.Done()
}

func (c *Cloudbreak) CreateCredential(defaultCredential models.CredentialResponse, existingKey string) int64 {
	defer timeTrack(time.Now(), "create credential")
	var credentialMap = make(map[string]interface{})
	credentialMap["selector"] = "role-based"
	credentialMap["roleArn"] = defaultCredential.Parameters["roleArn"]
	credentialMap["existingKeyPairName"] = existingKey

	credentialName := "cred" + strconv.FormatInt(time.Now().UnixNano(), 10)

	credReq := models.CredentialRequest{
		Name:          credentialName,
		CloudPlatform: "AWS",
		PublicKey:     defaultCredential.PublicKey,
		Parameters:    credentialMap,
	}

	log.Infof("[CreateCredential] sending credential create request with name: %s", credentialName)
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
	resp, err := c.Cloudbreak.Credentials.GetCredentialsUser(&credentials.GetCredentialsUserParams{})

	if err != nil {
		logErrorAndExit(c.GetCredential, err.Error())
	}

	var awsAccess models.CredentialResponse
	for _, credential := range resp.Payload {
		if credential.Name == name {
			awsAccess = *credential
			break
		}
	}

	if awsAccess.ID == nil {
		logErrorAndExit(c.GetCredential, fmt.Sprintf("failed to find the following credential: %s", name))
	}

	log.Infof("[GetCredential] found credential, name: %s id: %d", awsAccess.Name, awsAccess.ID)
	return awsAccess
}
