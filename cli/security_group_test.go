package cli

import (
	"regexp"
	"strconv"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/client/securitygroups"
	"github.com/hortonworks/hdc-cli/models"
)

func TestCreateSecurityGroupImplNoWebAccess(t *testing.T) {
	skeleton := ClusterSkeleton{
		RemoteAccess: "remote-access",
	}
	c := make(chan int64, 1)
	expectedId := int64(1)
	var actualGroup *models.SecurityGroupJSON
	postSecGroup := func(params *securitygroups.PostSecuritygroupsAccountParams) (*securitygroups.PostSecuritygroupsAccountOK, error) {
		actualGroup = params.Body
		resp := securitygroups.PostSecuritygroupsAccountOK{
			Payload: &models.ID{ID: expectedId},
		}
		return &resp, nil
	}

	createSecurityGroupImpl(skeleton, c, postSecGroup)

	actualId := <-c
	if actualId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actualId)
	}
	if m, _ := regexp.MatchString("secg([0-9]{10,20})", actualGroup.Name); m == false {
		t.Errorf("name not match secg([0-9]{10,20}) == %s", actualGroup.Name)
	}
	if len(actualGroup.SecurityRules) != 1 {
		t.Fatal("missing security group rule")
	}
	rule := actualGroup.SecurityRules[0]
	if rule.Subnet != skeleton.RemoteAccess {
		t.Errorf("rule subnet not match %s == %s", skeleton.RemoteAccess, rule.Subnet)
	}
	if rule.Protocol != "tcp" {
		t.Errorf("rule protocol not match tcp == %s", rule.Protocol)
	}
	expectedPorts := append(SECURITY_GROUP_DEFAULT_PORTS)
	if rule.Ports != strings.Join(expectedPorts, ",") {
		t.Errorf("rule ports not match %s == %s", strings.Join(expectedPorts, ","), rule.Ports)
	}
	if *rule.Modifiable != false {
		t.Error("rule is modifiable")
	}
}

func TestCreateSecurityGroupImplWebAccess(t *testing.T) {
	skeleton := ClusterSkeleton{WebAccess: true}
	c := make(chan int64, 1)
	var actualGroup *models.SecurityGroupJSON
	postSecGroup := func(params *securitygroups.PostSecuritygroupsAccountParams) (*securitygroups.PostSecuritygroupsAccountOK, error) {
		actualGroup = params.Body
		resp := securitygroups.PostSecuritygroupsAccountOK{
			Payload: &models.ID{ID: int64(1)},
		}
		return &resp, nil
	}

	createSecurityGroupImpl(skeleton, c, postSecGroup)

	if len(actualGroup.SecurityRules) != 1 {
		t.Fatal("missing security group rule")
	}
	rule := actualGroup.SecurityRules[0]
	expectedPorts := append(SECURITY_GROUP_DEFAULT_PORTS, "8080", "18080", "18081", "9995", "6080", "21000", "10000")
	if rule.Ports != strings.Join(expectedPorts, ",") {
		t.Errorf("rule ports not match %s == %s", strings.Join(expectedPorts, ","), rule.Ports)
	}
}

func TestGetSecurityDetailsImpl(t *testing.T) {
	groups := []*models.InstanceGroup{
		{SecurityGroupID: int64(1)},
		{SecurityGroupID: int64(2)},
	}
	stack := &models.StackResponse{
		InstanceGroups: groups,
	}
	rules := make(map[string][]*models.SecurityRule)
	rules["subnet1"] = []*models.SecurityRule{{Subnet: "first"}, {Subnet: "sec"}}
	rules["subnet2"] = []*models.SecurityRule{{Subnet: "subnet"}}
	getIds := func(params *securitygroups.GetSecuritygroupsIDParams) (*securitygroups.GetSecuritygroupsIDOK, error) {
		return &securitygroups.GetSecuritygroupsIDOK{Payload: &models.SecurityGroupJSON{
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
