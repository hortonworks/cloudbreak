package cli

import (
	"regexp"
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/client_cloudbreak/securitygroups"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func TestCreateSecurityGroupImplNoWebAccess(t *testing.T) {
	skeleton := ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			RemoteAccess: "remote-access",
		},
	}
	c := make(chan int64, 1)
	expectedId := int64(1)
	var actualGroup *models_cloudbreak.SecurityGroupRequest
	postSecGroup := func(params *securitygroups.PostPublicSecurityGroupParams) (*securitygroups.PostPublicSecurityGroupOK, error) {
		actualGroup = params.Body
		resp := securitygroups.PostPublicSecurityGroupOK{
			Payload: &models_cloudbreak.SecurityGroupResponse{ID: expectedId},
		}
		return &resp, nil
	}

	createSecurityGroupImpl(skeleton, MASTER, c, postSecGroup)

	actualId := <-c
	if actualId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actualId)
	}
	if m, _ := regexp.MatchString("hdc-sg-master-([0-9]{10,20})", *actualGroup.Name); m == false {
		t.Errorf("name not match hdc-sg-master-([0-9]{10,20}) == %s", *actualGroup.Name)
	}
	if len(actualGroup.SecurityRules) != 1 {
		t.Fatal("missing security group rule")
	}
	rule := actualGroup.SecurityRules[0]
	if *rule.Subnet != skeleton.RemoteAccess {
		t.Errorf("rule subnet not match %s == %s", skeleton.RemoteAccess, *rule.Subnet)
	}
	if *rule.Protocol != "tcp" {
		t.Errorf("rule protocol not match tcp == %s", *rule.Protocol)
	}
	expectedPorts := append(SECURITY_GROUP_DEFAULT_PORTS)
	if *rule.Ports != strings.Join(expectedPorts, ",") {
		t.Errorf("rule ports not match %s == %s", strings.Join(expectedPorts, ","), *rule.Ports)
	}
	if *rule.Modifiable != false {
		t.Error("rule is modifiable")
	}
}

func TestCreateSecurityGroupImplWebAccess(t *testing.T) {
	skeleton := ClusterSkeleton{ClusterSkeletonBase: ClusterSkeletonBase{WebAccess: true}}
	c := make(chan int64, 1)
	var actualGroup *models_cloudbreak.SecurityGroupRequest
	postSecGroup := func(params *securitygroups.PostPublicSecurityGroupParams) (*securitygroups.PostPublicSecurityGroupOK, error) {
		actualGroup = params.Body
		id := int64(1)
		resp := securitygroups.PostPublicSecurityGroupOK{
			Payload: &models_cloudbreak.SecurityGroupResponse{ID: id},
		}
		return &resp, nil
	}

	createSecurityGroupImpl(skeleton, MASTER, c, postSecGroup)

	if len(actualGroup.SecurityRules) != 1 {
		t.Fatal("missing security group rule")
	}
	rule := actualGroup.SecurityRules[0]
	expectedPorts := append(SECURITY_GROUP_DEFAULT_PORTS, "8443")
	if *rule.Ports != strings.Join(expectedPorts, ",") {
		t.Errorf("rule ports not match %s == %s", strings.Join(expectedPorts, ","), *rule.Ports)
	}
}

func TestGetSecurityDetailsImpl(t *testing.T) {
	id1 := int64(1)
	id2 := int64(2)
	groups := []*models_cloudbreak.InstanceGroupResponse{
		{SecurityGroupID: id1},
		{SecurityGroupID: id2},
	}
	stack := &models_cloudbreak.StackResponse{
		InstanceGroups: groups,
	}
	rules := make(map[string][]*models_cloudbreak.SecurityRuleResponse)
	rules["subnet1"] = []*models_cloudbreak.SecurityRuleResponse{{Subnet: &(&stringWrapper{"first"}).s}, {Subnet: &(&stringWrapper{"sec"}).s}}
	rules["subnet2"] = []*models_cloudbreak.SecurityRuleResponse{{Subnet: &(&stringWrapper{"subnet"}).s}}
	getIds := func(params *securitygroups.GetSecurityGroupParams) (*securitygroups.GetSecurityGroupOK, error) {
		return &securitygroups.GetSecurityGroupOK{Payload: &models_cloudbreak.SecurityGroupResponse{
			SecurityRules: rules["subnet"+strconv.FormatInt(params.ID, 10)],
		}}, nil
	}

	actual, _ := getSecurityDetailsImpl(stack, getIds)

	var expectedLen int
	for _, r := range rules {
		expectedLen += len(r)
	}
	if len(actual) != expectedLen {
		t.Errorf("size not match %d == %d", expectedLen, len(actual))
	}
}
