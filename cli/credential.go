package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/sequenceiq/hdc-cli/client/credentials"
	"github.com/sequenceiq/hdc-cli/models"
	"strconv"
	"sync"
	"time"
)

func (c *Cloudbreak) CreateCredential(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	var credentialMap = make(map[string]interface{})
	credentialMap["selector"] = "role-based"
	credentialMap["roleArn"] = ""
	credentialMap["existingKeyPairName"] = skeleton.SSHKeyName

	credentialName := "cred" + strconv.FormatInt(time.Now().UnixNano(), 10)

	credReq := models.CredentialRequest{
		Name:          credentialName,
		CloudPlatform: "AWS",
		PublicKey:     "",
		Parameters:    credentialMap,
	}

	log.Infof("[CreateCredential] sending credential create request with name: %s", credentialName)
	resp, err := c.Cloudbreak.Credentials.PostCredentialsAccount(&credentials.PostCredentialsAccountParams{&credReq})

	if err != nil {
		log.Errorf("[CreateCredential] %s", err.Error())
		newExitReturnError()
	}

	log.Infof("[CreateCredential] credential created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID
	wg.Done()
}
