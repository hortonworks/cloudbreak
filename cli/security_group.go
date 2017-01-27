package cli

import (
	"strconv"
	"strings"
	"sync"
	"time"

	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/securitygroups"
	"github.com/hortonworks/hdc-cli/models"
)

var SECURITY_GROUP_DEFAULT_PORTS = []string{"22"}
var SECURITY_GROUP_GATEWAY_NGINX_PORT = "9443"
var SECURITY_GROUP_GATEWAY_KNOX_PORT = "8443"

func (c *Cloudbreak) CreateSecurityGroup(skeleton ClusterSkeleton, group string, channel chan int64, wg *sync.WaitGroup) {
	defer timeTrack(time.Now(), "create security group")
	defer wg.Done()

	createSecurityGroupImpl(skeleton, group, channel, c.Cloudbreak.Securitygroups.PostSecuritygroupsAccount)
}

func createSecurityGroupImpl(skeleton ClusterSkeleton, group string, channel chan int64,
	postSecGroup func(*securitygroups.PostSecuritygroupsAccountParams) (*securitygroups.PostSecuritygroupsAccountOK, error)) {

	secGroup := createSecurityGroupRequest(skeleton, group)

	log.Infof("[CreateSecurityGroup] sending security group create request with name: %s", secGroup.Name)
	resp, err := postSecGroup(&securitygroups.PostSecuritygroupsAccountParams{Body: secGroup})

	if err != nil {
		logErrorAndExit(createSecurityGroupImpl, err.Error())
	}

	log.Infof("[CreateSecurityGroup] security group created, id: %d", resp.Payload.ID)
	channel <- *resp.Payload.ID
}

func createSecurityGroupRequest(skeleton ClusterSkeleton, group string) *models.SecurityGroupRequest {
	secGroupName := fmt.Sprintf("hdc-sg-%s-%s", strings.ToLower(group), strconv.FormatInt(time.Now().UnixNano(), 10))

	openPorts := SECURITY_GROUP_DEFAULT_PORTS

	if group == MASTER {
		openPorts = append(openPorts, SECURITY_GROUP_GATEWAY_NGINX_PORT)
		if skeleton.WebAccess || skeleton.WebAccessHive || skeleton.WebAccessClusterManagement {
			secGroupName = fmt.Sprintf("hdc-sg-webaccess-%s-%s", strings.ToLower(group), strconv.FormatInt(time.Now().UnixNano(), 10))
			openPorts = append(openPorts, SECURITY_GROUP_GATEWAY_KNOX_PORT)
		}
	}

	modifiable := false
	secRules := []*models.SecurityRuleRequest{
		{
			Subnet:     skeleton.RemoteAccess,
			Protocol:   "tcp",
			Ports:      strings.Join(openPorts, ","),
			Modifiable: &modifiable,
		},
	}

	secGroup := models.SecurityGroupRequest{
		Name:          secGroupName,
		SecurityRules: secRules,
		CloudPlatform: "AWS",
	}

	return &secGroup
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
