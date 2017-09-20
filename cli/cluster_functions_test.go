package cli

import (
	"sort"
	"strings"
	"testing"

	"github.com/hortonworks/hdc-cli/cli/cloud"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func clusterSkeleton(stackParams map[string]string, credParams map[string]interface{}, netParams map[string]interface{}) (ClusterSkeletonResult, *models_cloudbreak.StackResponse, *models_cloudbreak.CredentialResponse, *models_cloudbreak.BlueprintResponse, *models_cloudbreak.NetworkResponse) {
	skeleton := ClusterSkeletonResult{}
	sr := models_cloudbreak.StackResponse{
		Name:         &(&stringWrapper{"stack-name"}).s,
		Status:       "status",
		StatusReason: "status-reason",
		HdpVersion:   "hdp-version",
		Parameters:   stackParams,
	}
	cr := models_cloudbreak.CredentialResponse{
		Parameters: credParams,
	}
	br := models_cloudbreak.BlueprintResponse{
		Name:            &(&stringWrapper{"blueprint-name"}).s,
		AmbariBlueprint: "{\"Blueprints\": {\"blueprint_name\": \"ambari-blueprint-name\"}}",
	}
	nj := models_cloudbreak.NetworkResponse{
		Parameters: netParams,
	}

	return skeleton, &sr, &cr, &br, &nj
}

func defaultNetworkParams() map[string]interface{} {
	np := make(map[string]interface{})
	np["internetGatewayId"] = ""
	return np
}

func TestFillMinimumSet(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	skeleton.fill(sr, cr, br, nil, nil, nj, nil, nil, nil, nil)

	if skeleton.ClusterName != *sr.Name {
		t.Errorf("name not match %s == %s", *sr.Name, skeleton.ClusterName)
	}
	if skeleton.InstanceRole != "" {
		t.Errorf("instance role not empty %s", skeleton.RemoteAccess)
	}
	if skeleton.Status != sr.Status {
		t.Errorf("status not match %s == %s", sr.Status, skeleton.Status)
	}
	if skeleton.StatusReason != sr.StatusReason {
		t.Errorf("status reason not match %s == %s", sr.StatusReason, skeleton.StatusReason)
	}
	if skeleton.HDPVersion != "hdp" {
		t.Errorf("HDP version not match hdp == %s", skeleton.HDPVersion)
	}
	if skeleton.ClusterType != *br.Name {
		t.Errorf("cluster type not match %s == %s", *br.Name, skeleton.ClusterType)
	}
	if skeleton.Network != nil {
		t.Errorf("network not empty %s", *skeleton.Network)
	}
	if skeleton.HiveMetastore != nil {
		t.Errorf("hive meta store not empty %s", *skeleton.HiveMetastore)
	}
	if skeleton.SSHKeyName != "" {
		t.Errorf("ssh key name not empty %s", skeleton.SSHKeyName)
	}
	if skeleton.RemoteAccess != "" {
		t.Errorf("remote access not empty %s", skeleton.RemoteAccess)
	}
	if skeleton.WebAccess != false {
		t.Error("web access must be false")
	}
}

func TestFillWithInstanceProfileStrategy(t *testing.T) {
	sp := make(map[string]string)
	sp["instanceProfileStrategy"] = "strategy"
	skeleton, sr, cr, br, nj := clusterSkeleton(sp, nil, defaultNetworkParams())

	skeleton.fill(sr, cr, br, nil, nil, nj, nil, nil, nil, nil)

	if skeleton.InstanceRole != sp["instanceProfileStrategy"] {
		t.Errorf("instance role not match %s == %s", sp["instanceProfileStrategy"], skeleton.InstanceRole)
	}
}

func TestFillWithUseExistingInstanceProfileStrategy(t *testing.T) {
	sp := make(map[string]string)
	sp["instanceProfileStrategy"] = "USE_EXISTING"
	sp["instanceProfile"] = "s3-role"
	skeleton, sr, cr, br, nj := clusterSkeleton(sp, nil, defaultNetworkParams())

	skeleton.fill(sr, cr, br, nil, nil, nj, nil, nil, nil, nil)

	if skeleton.InstanceRole != sp["instanceProfile"] {
		t.Errorf("instance role not match %s == %s", sp["instanceProfile"], skeleton.InstanceRole)
	}
}

func TestFillWithExistingNetwork(t *testing.T) {
	np := make(map[string]interface{})
	np["vpcId"] = "vpcId"
	np["subnetId"] = "subnetId"
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, np)

	skeleton.fill(sr, cr, br, nil, nil, nj, nil, nil, nil, nil)

	expected := cloud.Network{VpcId: "vpcId", SubnetId: "subnetId"}
	if *skeleton.Network != expected {
		t.Errorf("network not match %s == %s", expected, *skeleton.Network)
	}
}

func TestFillWithRDSConfig(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	rdsName := "rds-name"
	rcr := []*models_cloudbreak.RDSConfigResponse{{
		Name: &rdsName,
		Type: HIVE_RDS,
	},
	}

	skeleton.fill(sr, cr, br, nil, nil, nj, rcr, nil, nil, nil)

	if skeleton.HiveMetastore == nil {
		t.Error("meta store is empty")
	} else if skeleton.HiveMetastore.Name != rdsName {
		t.Errorf("meta store name not match %s == %s", rdsName, skeleton.HiveMetastore.Name)
	}
}

func TestFillWithRDSConfigsdrgwsr(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	groups := make([]*models_cloudbreak.InstanceGroupResponse, 0)
	groups = append(groups, &models_cloudbreak.InstanceGroupResponse{Group: &MASTER, NodeCount: &(&int32Wrapper{1}).i})
	groups = append(groups, &models_cloudbreak.InstanceGroupResponse{Group: &WORKER, NodeCount: &(&int32Wrapper{2}).i})
	sr.InstanceGroups = groups

	n := int32(10)

	tm := make(map[string]*models_cloudbreak.TemplateResponse)
	tm[MASTER] = &models_cloudbreak.TemplateResponse{
		InstanceType: &(&stringWrapper{"master"}).s,
		VolumeType:   "type",
		VolumeSize:   &n,
		VolumeCount:  &n,
	}
	tm[WORKER] = &models_cloudbreak.TemplateResponse{
		InstanceType: &(&stringWrapper{"worker"}).s,
		VolumeType:   "type",
		VolumeSize:   &n,
		VolumeCount:  &n,
	}

	sm := make(map[string][]*models_cloudbreak.SecurityRuleResponse)
	rules := make([]*models_cloudbreak.SecurityRuleResponse, 0)
	rules = append(rules, &models_cloudbreak.SecurityRuleResponse{Ports: &(&stringWrapper{"ports"}).s})
	sm["master"] = rules

	skeleton.fill(sr, cr, br, tm, sm, nj, nil, nil, nil, nil)

	if skeleton.Master.InstanceCount != 1 {
		t.Errorf("master instance count not match 1 == %d", skeleton.Master.InstanceCount)
	}
	if skeleton.Master.InstanceType != *tm[MASTER].InstanceType {
		t.Errorf("master instance type not match %s == %s", *tm[MASTER].InstanceType, skeleton.Master.InstanceType)
	}
	if skeleton.Master.VolumeType != tm[MASTER].VolumeType {
		t.Errorf("master volume type not match %s == %s", tm[MASTER].VolumeType, skeleton.Master.VolumeType)
	}
	if *skeleton.Master.VolumeSize != *tm[MASTER].VolumeSize {
		t.Errorf("master volume size not match %d == %d", tm[MASTER].VolumeSize, skeleton.Master.VolumeSize)
	}
	if *skeleton.Master.VolumeCount != *tm[MASTER].VolumeCount {
		t.Errorf("master volume count not match %d == %d", tm[MASTER].VolumeCount, skeleton.Master.VolumeCount)
	}
	if skeleton.Worker.InstanceCount != 2 {
		t.Errorf("worker instance count not match 2 == %d", skeleton.Worker.InstanceCount)
	}
	if skeleton.Worker.InstanceType != *tm[WORKER].InstanceType {
		t.Errorf("worker instance type not match %s == %s", *tm[WORKER].InstanceType, skeleton.Worker.InstanceType)
	}
	if skeleton.Worker.VolumeType != tm[WORKER].VolumeType {
		t.Errorf("worker volume type not match %s == %s", tm[WORKER].VolumeType, skeleton.Worker.VolumeType)
	}
	if *skeleton.Worker.VolumeSize != *tm[WORKER].VolumeSize {
		t.Errorf("worker volume size not match %d == %d", tm[WORKER].VolumeSize, skeleton.Worker.VolumeSize)
	}
	if *skeleton.Worker.VolumeCount != *tm[WORKER].VolumeCount {
		t.Errorf("worker volume count not match %d == %d", tm[WORKER].VolumeCount, skeleton.Worker.VolumeCount)
	}
}

func TestFillWithSshKey(t *testing.T) {
	cp := make(map[string]interface{})
	cp["existingKeyPairName"] = "ssh-key-name"
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, cp, defaultNetworkParams())

	skeleton.fill(sr, cr, br, nil, nil, nj, nil, nil, nil, nil)

	if skeleton.SSHKeyName != cp["existingKeyPairName"] {
		t.Errorf("ssh key name not match %s == %s", cp["existingKeyPairName"], skeleton.SSHKeyName)
	}
}

func TestFillWithSecurityMap(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	cl := models_cloudbreak.ClusterResponse{
		Gateway: &models_cloudbreak.GatewayJSON{
			ExposedServices: []string{"AMBARI"},
		},
	}
	sr.Cluster = &cl

	sm := make(map[string][]*models_cloudbreak.SecurityRuleResponse)
	rules := make([]*models_cloudbreak.SecurityRuleResponse, 0)
	rules = append(rules, &models_cloudbreak.SecurityRuleResponse{Ports: &(&stringWrapper{"22,9443,8443"}).s})
	sm["master"] = rules
	sm["worker"] = rules

	skeleton.fill(sr, cr, br, nil, sm, nj, nil, nil, nil, nil)

	expected := []string{"master", "worker"}
	sort.Strings(expected)
	actual := strings.Split(skeleton.RemoteAccess, ",")
	sort.Strings(actual)

	if strings.Join(actual, "") != strings.Join(expected, "") {
		t.Errorf("remote access not match %s == %s", expected, actual)
	}
	if skeleton.WebAccess != true {
		t.Error("web access must be true")
	}
}

func TestFillWithSecurityMapDefaultPorts(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	sm := make(map[string][]*models_cloudbreak.SecurityRuleResponse)
	rules := make([]*models_cloudbreak.SecurityRuleResponse, 0)
	rules = append(rules, &models_cloudbreak.SecurityRuleResponse{Ports: &(&stringWrapper{"22,9443"}).s})
	sm["master"] = rules

	skeleton.fill(sr, cr, br, nil, sm, nj, nil, nil, nil, nil)

	if skeleton.WebAccess != false {
		t.Error("web access must be false")
	}
}

func TestFillWithCluster(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	inputs := make([]*models_cloudbreak.BlueprintInput, 0)
	inputs = append(inputs, &models_cloudbreak.BlueprintInput{Name: "property", PropertyValue: "value"})
	sr.Cluster = &models_cloudbreak.ClusterResponse{
		Status:          "cluster-status",
		StatusReason:    "cluster-reason",
		BlueprintInputs: inputs,
	}

	skeleton.fill(sr, cr, br, nil, nil, nj, nil, nil, nil, nil)

	if skeleton.Status != sr.Cluster.Status {
		t.Errorf("status not match %s == %s", sr.Cluster.Status, skeleton.Status)
	}
	if skeleton.StatusReason != sr.Cluster.StatusReason {
		t.Errorf("status reason not match %s == %s", sr.Cluster.StatusReason, skeleton.StatusReason)
	}

}

func TestFillWithNoInstances(t *testing.T) {
	skeleton, sr, _, _, _ := clusterSkeleton(nil, nil, nil)

	skeleton.fill(sr, nil, nil, nil, nil, nil, nil, nil, nil, nil)

	if skeleton.Nodes != UNKNOWN {
		t.Errorf("nodes status not match %s == %s", skeleton.Nodes, UNKNOWN)
	}
}

func TestFillWitMixedHostStatuses(t *testing.T) {
	skeleton, _, _, _, _ := clusterSkeleton(nil, nil, nil)
	host1 := "host1.example.com"
	host2 := "host2.example.com"
	metadata := []*models_cloudbreak.InstanceMetaData{
		{DiscoveryFQDN: host1,
			InstanceGroup: MASTER},
		{DiscoveryFQDN: host2,
			InstanceGroup: WORKER},
	}
	ig := []*models_cloudbreak.InstanceGroupResponse{{Metadata: metadata}}
	hm := []*models_cloudbreak.HostMetadata{
		{Name: &host1,
			State: UNHEALTHY},
		{Name: &host2,
			State: HEALTHY},
	}
	hg := []*models_cloudbreak.HostGroupResponse{
		{Name: &MASTER,
			Metadata: hm},
	}
	cr := models_cloudbreak.ClusterResponse{
		HostGroups: hg,
	}
	sr := models_cloudbreak.StackResponse{
		Name:           &(&stringWrapper{"stack-name"}).s,
		InstanceGroups: ig,
		Cluster:        &cr,
	}

	skeleton.fill(&sr, nil, nil, nil, nil, nil, nil, nil, nil, nil)

	if skeleton.Nodes != UNHEALTHY {
		t.Errorf("nodes status not match %s == %s", skeleton.Nodes, UNHEALTHY)
	}
}

func TestFillWithHealthyHostStatuses(t *testing.T) {
	skeleton, _, _, _, _ := clusterSkeleton(nil, nil, nil)
	host1 := "host1.example.com"
	host2 := "host2.example.com"
	metadata := []*models_cloudbreak.InstanceMetaData{
		{DiscoveryFQDN: host1,
			InstanceGroup: MASTER},
		{DiscoveryFQDN: host2,
			InstanceGroup: WORKER},
	}
	ig := []*models_cloudbreak.InstanceGroupResponse{{Metadata: metadata}}
	hm := []*models_cloudbreak.HostMetadata{
		{Name: &host1,
			State: HEALTHY},
		{Name: &host2,
			State: HEALTHY},
	}
	hg := []*models_cloudbreak.HostGroupResponse{
		{Name: &MASTER,
			Metadata: hm},
	}
	cr := models_cloudbreak.ClusterResponse{
		HostGroups: hg,
	}
	sr := models_cloudbreak.StackResponse{
		Name:           &(&stringWrapper{"stack-name"}).s,
		InstanceGroups: ig,
		Cluster:        &cr,
	}

	skeleton.fill(&sr, nil, nil, nil, nil, nil, nil, nil, nil, nil)

	if skeleton.Nodes != HEALTHY {
		t.Errorf("nodes status not match %s == %s", skeleton.Nodes, HEALTHY)
	}
}

func TestFillWithUnknownHostStatuses(t *testing.T) {
	skeleton, _, _, _, _ := clusterSkeleton(nil, nil, nil)
	host1 := "host1.example.com"
	host2 := "host2.example.com"
	metadata := []*models_cloudbreak.InstanceMetaData{
		{DiscoveryFQDN: host1,
			InstanceGroup: MASTER},
		{DiscoveryFQDN: host2,
			InstanceGroup: WORKER},
	}
	ig := []*models_cloudbreak.InstanceGroupResponse{{Metadata: metadata}}
	cr := models_cloudbreak.ClusterResponse{}
	sr := models_cloudbreak.StackResponse{
		Name:           &(&stringWrapper{"stack-name"}).s,
		InstanceGroups: ig,
		Cluster:        &cr,
	}

	skeleton.fill(&sr, nil, nil, nil, nil, nil, nil, nil, nil, nil)

	if skeleton.Nodes != UNKNOWN {
		t.Errorf("nodes status not match %s == %s", skeleton.Nodes, UNKNOWN)
	}
}

func TestFillWithClusterAvailable(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	sr.Cluster = &models_cloudbreak.ClusterResponse{
		Status: "AVAILABLE",
	}

	skeleton.fill(sr, cr, br, nil, nil, nj, nil, nil, nil, nil)

	if skeleton.Status != sr.Status {
		t.Errorf("status not match %s == %s", sr.Status, skeleton.Status)
	}
}
