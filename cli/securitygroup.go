package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/sequenceiq/hdc-cli/client/securitygroups"
	"github.com/sequenceiq/hdc-cli/models"
	"strconv"
	"strings"
	"sync"
	"time"
)

func (c *Cloudbreak) CreateSecurityGroup(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {

	secGroupName := "secg" + strconv.FormatInt(time.Now().UnixNano(), 10)

	defaultPorts := []string{"22", "443"}
	if skeleton.WebAccess {
		defaultPorts = append(defaultPorts, "9443", "8080", "18080", "18081", "9995")
	}

	modifiable := false
	secRules := []*models.SecurityRule{
		{
			Subnet:     skeleton.RemoteAccess,
			Protocol:   "tcp",
			Ports:      strings.Join(defaultPorts, ","),
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
		newExitReturnError()
	}

	log.Infof("[CreateSecurityGroup] security group created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID

	wg.Done()
}
