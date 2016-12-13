package cli

import (
	"strconv"
	"strings"
	"sync"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/securitygroups"
	"github.com/hortonworks/hdc-cli/models"
)

var SECURITY_GROUP_DEFAULT_PORTS = []string{"22", "443", "9443"}

func (c *Cloudbreak) CreateSecurityGroup(skeleton ClusterSkeleton, channel chan int64, wg *sync.WaitGroup) {
	defer timeTrack(time.Now(), "create security group")
	defer wg.Done()

	createSecurityGroupImpl(skeleton, channel, c.Cloudbreak.Securitygroups.PostSecuritygroupsAccount)
}

func createSecurityGroupImpl(skeleton ClusterSkeleton, channel chan int64,
	postSecGroup func(*securitygroups.PostSecuritygroupsAccountParams) (*securitygroups.PostSecuritygroupsAccountOK, error)) {

	secGroupName := "secg" + strconv.FormatInt(time.Now().UnixNano(), 10)

	defaultPorts := SECURITY_GROUP_DEFAULT_PORTS
	if skeleton.WebAccess {
		defaultPorts = append(defaultPorts, "8080", "9995")
	}

	modifiable := false
	secRules := []*models.SecurityRuleRequest{
		{
			Subnet:     skeleton.RemoteAccess,
			Protocol:   "tcp",
			Ports:      strings.Join(defaultPorts, ","),
			Modifiable: &modifiable,
		},
	}

	secGroup := models.SecurityGroupRequest{
		Name:          secGroupName,
		SecurityRules: secRules,
	}

	log.Infof("[CreateSecurityGroup] sending security group create request with name: %s", secGroupName)
	resp, err := postSecGroup(&securitygroups.PostSecuritygroupsAccountParams{Body: &secGroup})

	if err != nil {
		logErrorAndExit(createSecurityGroupImpl, err.Error())
	}

	log.Infof("[CreateSecurityGroup] security group created, id: %d", resp.Payload.ID)
	channel <- *resp.Payload.ID
}

func (c *Cloudbreak) GetSecurityDetails(stack *models.StackResponse) (map[string][]*models.SecurityRuleResponse, error) {
	defer timeTrack(time.Now(), "get security group by id")

	return getSecurityDetailsImpl(stack, c.Cloudbreak.Securitygroups.GetSecuritygroupsID)
}

func getSecurityDetailsImpl(stack *models.StackResponse,
	getIds func(*securitygroups.GetSecuritygroupsIDParams) (*securitygroups.GetSecuritygroupsIDOK, error)) (securityMap map[string][]*models.SecurityRuleResponse, err error) {
	securityMap = make(map[string][]*models.SecurityRuleResponse)
	for _, v := range stack.InstanceGroups {
		if respSecurityGroup, err := getIds(&securitygroups.GetSecuritygroupsIDParams{ID: *v.SecurityGroupID}); err == nil {
			securityGroup := respSecurityGroup.Payload
			for _, sr := range securityGroup.SecurityRules {
				securityMap[sr.Subnet] = append(securityMap[sr.Subnet], sr)
			}
		}
	}
	return securityMap, err
}

func (c *Cloudbreak) GetPublicSecurityGroups() []*models.SecurityGroupResponse {
	defer timeTrack(time.Now(), "get public security groups")
	resp, err := c.Cloudbreak.Securitygroups.GetSecuritygroupsAccount(&securitygroups.GetSecuritygroupsAccountParams{})
	if err != nil {
		logErrorAndExit(c.GetPublicSecurityGroups, err.Error())
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteSecurityGroup(name string) error {
	defer timeTrack(time.Now(), "delete security group")
	log.Infof("[DeleteSecurityGroup] delete security group: %s", name)
	return c.Cloudbreak.Securitygroups.DeleteSecuritygroupsAccountName(&securitygroups.DeleteSecuritygroupsAccountNameParams{Name: name})
}
