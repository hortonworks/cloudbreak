package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/sequenceiq/hdc-cli/client/securitygroups"
	"github.com/sequenceiq/hdc-cli/models"
	"strconv"
	"sync"
	"time"
)

func (c *Cloudbreak) CreateSecurityGroup(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {

	secGroupName := "secg" + strconv.FormatInt(time.Now().UnixNano(), 10)

	modifiable := false
	secRules := []*models.SecurityRule{
		{
			Subnet:     skeleton.RemoteAccess,
			Protocol:   "tcp",
			Ports:      "9443,22,443,8080,18080,18081,9995",
			Modifiable: &modifiable,
		},
	}

	secGroup := models.SecurityGroupJSON{
		Name:          secGroupName,
		SecurityRules: secRules,
	}

	log.Infof("[CreateSecurityGroup] sending security group create request with name: %s", secGroupName)
	resp, err := c.Cloudbreak.Securitygroups.PostSecuritygroupsAccount(&securitygroups.PostSecuritygroupsAccountParams{&secGroup})

	if err != nil {
		log.Errorf("[CreateSecurityGroup] %s", err.Error())
		newExitError()
	}

	log.Infof("[CreateSecurityGroup] security group created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID

	wg.Done()
}
