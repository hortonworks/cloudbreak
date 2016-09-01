package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/securitygroups"
	"github.com/hortonworks/hdc-cli/models"
	"strconv"
	"strings"
	"sync"
	"time"
)

var SECURITY_GROUP_DEFAULT_PORTS = []string{"22", "443", "9443"}

func (c *Cloudbreak) CreateSecurityGroup(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {

	secGroupName := "secg" + strconv.FormatInt(time.Now().UnixNano(), 10)

	defaultPorts := SECURITY_GROUP_DEFAULT_PORTS
	if skeleton.WebAccess {
		defaultPorts = append(defaultPorts, "8080", "18080", "18081", "9995")
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

func (c *Cloudbreak) GetSecurityDetails(client *Cloudbreak, stack *models.StackResponse) (securityMap map[string][]*models.SecurityRule, err error) {
	securityMap = make(map[string][]*models.SecurityRule)
	for _, v := range stack.InstanceGroups {
		if respSecurityGroup, err := client.Cloudbreak.Securitygroups.GetSecuritygroupsID(&securitygroups.GetSecuritygroupsIDParams{ID: v.SecurityGroupID}); err == nil {
			securityGroup := respSecurityGroup.Payload
			for _, sr := range securityGroup.SecurityRules {
				securityMap[sr.Subnet] = append(securityMap[sr.Subnet], sr)
			}
		}
	}
	return securityMap, err
}
