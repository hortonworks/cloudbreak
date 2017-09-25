package cli

import (
	"strconv"
	"strings"
	"sync"
	"time"

	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/securitygroups"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

var SECURITY_GROUP_DEFAULT_PORTS = []string{"22"}
var SECURITY_GROUP_GATEWAY_KNOX_PORT = "8443"

func (c *Cloudbreak) CreateSecurityGroup(skeleton ClusterSkeleton, group string, channel chan int64, wg *sync.WaitGroup) {
	defer timeTrack(time.Now(), "create security group")
	defer wg.Done()

	createSecurityGroupImpl(skeleton, group, channel, c.Cloudbreak.Securitygroups.PostPublicSecurityGroup)
}

func createSecurityGroupImpl(skeleton ClusterSkeleton, group string, channel chan int64,
	postSecGroup func(*securitygroups.PostPublicSecurityGroupParams) (*securitygroups.PostPublicSecurityGroupOK, error)) {

	secGroup := createSecurityGroupRequest(skeleton, group)

	log.Infof("[CreateSecurityGroup] sending security group create request with name: %s", secGroup.Name)
	resp, err := postSecGroup(securitygroups.NewPostPublicSecurityGroupParams().WithBody(secGroup))

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[CreateSecurityGroup] security group created, id: %d", resp.Payload.ID)
	channel <- resp.Payload.ID
}

func createSecurityGroupRequest(skeleton ClusterSkeleton, group string) *models_cloudbreak.SecurityGroupRequest {
	secGroupName := fmt.Sprintf("hdc-sg-%s-%s", strings.ToLower(group), strconv.FormatInt(time.Now().UnixNano(), 10))

	openPorts := SECURITY_GROUP_DEFAULT_PORTS

	if group == MASTER {
		if skeleton.WebAccess || skeleton.HiveJDBCAccess || skeleton.ClusterComponentAccess {
			secGroupName = fmt.Sprintf("hdc-sg-webaccess-%s-%s", strings.ToLower(group), strconv.FormatInt(time.Now().UnixNano(), 10))
			openPorts = append(openPorts, SECURITY_GROUP_GATEWAY_KNOX_PORT)
		}
	}

	modifiable := false
	ports := strings.Join(openPorts, ",")
	secRules := []*models_cloudbreak.SecurityRuleRequest{
		{
			Subnet:     &skeleton.RemoteAccess,
			Protocol:   &(&stringWrapper{"tcp"}).s,
			Ports:      &ports,
			Modifiable: &modifiable,
		},
	}

	secGroup := models_cloudbreak.SecurityGroupRequest{
		Name:          &secGroupName,
		SecurityRules: secRules,
		CloudPlatform: &(&stringWrapper{"AWS"}).s,
	}

	return &secGroup
}

func (c *Cloudbreak) GetSecurityDetails(stack *models_cloudbreak.StackResponse) (map[string][]*models_cloudbreak.SecurityRuleResponse, error) {
	defer timeTrack(time.Now(), "get security group by id")

	return getSecurityDetailsImpl(stack, c.Cloudbreak.Securitygroups.GetSecurityGroup)
}

func getSecurityDetailsImpl(stack *models_cloudbreak.StackResponse,
	getIds func(*securitygroups.GetSecurityGroupParams) (*securitygroups.GetSecurityGroupOK, error)) (securityMap map[string][]*models_cloudbreak.SecurityRuleResponse, err error) {
	securityMap = make(map[string][]*models_cloudbreak.SecurityRuleResponse)
	for _, v := range stack.InstanceGroups {
		if respSecurityGroup, err := getIds(securitygroups.NewGetSecurityGroupParams().WithID(v.SecurityGroupID)); err == nil {
			securityGroup := respSecurityGroup.Payload
			for _, sr := range securityGroup.SecurityRules {
				securityMap[*sr.Subnet] = append(securityMap[*sr.Subnet], sr)
			}
		}
	}
	return securityMap, err
}

func (c *Cloudbreak) GetPublicSecurityGroups() []*models_cloudbreak.SecurityGroupResponse {
	defer timeTrack(time.Now(), "get public security groups")
	resp, err := c.Cloudbreak.Securitygroups.GetPublicsSecurityGroup(securitygroups.NewGetPublicsSecurityGroupParams())
	if err != nil {
		logErrorAndExit(err)
	}
	return resp.Payload
}

func (c *Cloudbreak) DeleteSecurityGroup(name string) error {
	defer timeTrack(time.Now(), "delete security group")
	log.Infof("[DeleteSecurityGroup] delete security group: %s", name)
	return c.Cloudbreak.Securitygroups.DeletePublicSecurityGroup(securitygroups.NewDeletePublicSecurityGroupParams().WithName(name))
}
