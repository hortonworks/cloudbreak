package cli

import (
	"testing"

	"github.com/hortonworks/hdc-cli/models"
)

type sw struct {
	s string
}

func clusterSkeleton(stackParams map[string]string, credParams map[string]interface{}, netParams map[string]interface{}) (ClusterSkeleton, *models.StackResponse, *models.CredentialResponse, *models.BlueprintResponse, *models.NetworkJSON) {
	skeleton := ClusterSkeleton{}
	sr := models.StackResponse{
		Name:         "stack-name",
		Status:       &(&sw{"status"}).s,
		StatusReason: &(&sw{"status-reason"}).s,
		HdpVersion:   &(&sw{"hdp-version"}).s,
		Parameters:   stackParams,
	}
	cr := models.CredentialResponse{
		Parameters: credParams,
	}
	br := models.BlueprintResponse{
		Name: "blueprint-name",
		AmbariBlueprint: models.AmbariBlueprint{
			Blueprint: models.Blueprint{
				Name: &(&sw{"ambari-blueprint-name"}).s,
			},
		},
	}
	nj := models.NetworkJSON{
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

	skeleton.fill(sr, cr, br, nil, nil, nj, nil)

	if skeleton.ClusterName != sr.Name {
		t.Errorf("name not match %s == %s", sr.Name, skeleton.ClusterName)
	}
	if skeleton.InstanceRole != "" {
		t.Errorf("instance role not empty %s", skeleton.RemoteAccess)
	}
	if skeleton.Status != *sr.Status {
		t.Errorf("status not match %s == %s", *sr.Status, skeleton.Status)
	}
	if skeleton.StatusReason != *sr.StatusReason {
		t.Errorf("status reason not match %s == %s", *sr.StatusReason, skeleton.StatusReason)
	}
	if skeleton.ClusterInputs != nil {
		t.Errorf("cluster inputs not empty %s", skeleton.ClusterInputs)
	}
	if skeleton.HDPVersion != "hdp" {
		t.Errorf("HDP version not match hdp == %s", skeleton.HDPVersion)
	}
	if skeleton.ClusterType != br.Name {
		t.Errorf("cluster type not match %s == %s", br.Name, skeleton.ClusterType)
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

	skeleton.fill(sr, cr, br, nil, nil, nj, nil)

	if skeleton.InstanceRole != sp["instanceProfileStrategy"] {
		t.Errorf("instance role not match %s == %s", sp["instanceProfileStrategy"], skeleton.InstanceRole)
	}
}

func TestFillWithUseExistingInstanceProfileStrategy(t *testing.T) {
	sp := make(map[string]string)
	sp["instanceProfileStrategy"] = "USE_EXISTING"
	sp["s3Role"] = "s3-role"
	skeleton, sr, cr, br, nj := clusterSkeleton(sp, nil, defaultNetworkParams())

	skeleton.fill(sr, cr, br, nil, nil, nj, nil)

	if skeleton.InstanceRole != sp["s3Role"] {
		t.Errorf("instance role not match %s == %s", sp["s3Role"], skeleton.InstanceRole)
	}
}

func TestFillWithExistingNetwork(t *testing.T) {
	np := make(map[string]interface{})
	np["vpcId"] = "vpcId"
	np["subnetId"] = "subnetId"
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, np)

	skeleton.fill(sr, cr, br, nil, nil, nj, nil)

	expected := Network{VpcId: "vpcId", SubnetId: "subnetId"}
	if *skeleton.Network != expected {
		t.Errorf("network not match %s == %s", expected, *skeleton.Network)
	}
}

func TestFillWithRDSConfig(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	rcr := &models.RDSConfigResponse{
		Name: "rds-name",
	}

	skeleton.fill(sr, cr, br, nil, nil, nj, rcr)

	if skeleton.HiveMetastore == nil {
		t.Error("meta store is empty")
	} else if skeleton.HiveMetastore.Name != rcr.Name {
		t.Errorf("meta store name not match %s == %s", rcr.Name, skeleton.HiveMetastore.Name)
	}
}

func TestFillWithRDSConfigsdrgwsr(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	groups := make([]*models.InstanceGroup, 0)
	groups = append(groups, &models.InstanceGroup{Group: MASTER, NodeCount: 1})
	groups = append(groups, &models.InstanceGroup{Group: WORKER, NodeCount: 2})
	sr.InstanceGroups = groups

	n := int32(10)

	tm := make(map[string]*models.TemplateResponse)
	tm[MASTER] = &models.TemplateResponse{
		InstanceType: "master",
		VolumeType:   &(&sw{"type"}).s,
		VolumeSize:   &n,
		VolumeCount:  n,
	}
	tm[WORKER] = &models.TemplateResponse{
		InstanceType: "worker",
		VolumeType:   &(&sw{"type"}).s,
		VolumeSize:   &n,
		VolumeCount:  n,
	}

	sm := make(map[string][]*models.SecurityRule)
	rules := make([]*models.SecurityRule, 0)
	rules = append(rules, &models.SecurityRule{Ports: "ports"})
	sm["master"] = rules

	skeleton.fill(sr, cr, br, tm, sm, nj, nil)

	if skeleton.Master.InstanceCount != 1 {
		t.Errorf("master instance count not match 1 == %d", skeleton.Master.InstanceCount)
	}
	if skeleton.Master.InstanceType != tm[MASTER].InstanceType {
		t.Errorf("master instance type not match %s == %s", tm[MASTER].InstanceType, skeleton.Master.InstanceType)
	}
	if skeleton.Master.VolumeType != *tm[MASTER].VolumeType {
		t.Errorf("master volume type not match %s == %s", *tm[MASTER].VolumeType, skeleton.Master.VolumeType)
	}
	if skeleton.Master.VolumeSize != *tm[MASTER].VolumeSize {
		t.Errorf("master volume size not match %d == %d", *tm[MASTER].VolumeSize, skeleton.Master.VolumeSize)
	}
	if skeleton.Master.VolumeCount != tm[MASTER].VolumeCount {
		t.Errorf("master volume count not match %d == %d", tm[MASTER].VolumeCount, skeleton.Master.VolumeCount)
	}
	if skeleton.Worker.InstanceCount != 2 {
		t.Errorf("worker instance count not match 2 == %d", skeleton.Worker.InstanceCount)
	}
	if skeleton.Worker.InstanceType != tm[WORKER].InstanceType {
		t.Errorf("worker instance type not match %s == %s", tm[WORKER].InstanceType, skeleton.Worker.InstanceType)
	}
	if skeleton.Worker.VolumeType != *tm[WORKER].VolumeType {
		t.Errorf("worker volume type not match %s == %s", *tm[WORKER].VolumeType, skeleton.Worker.VolumeType)
	}
	if skeleton.Worker.VolumeSize != *tm[WORKER].VolumeSize {
		t.Errorf("worker volume size not match %d == %d", *tm[WORKER].VolumeSize, skeleton.Worker.VolumeSize)
	}
	if skeleton.Worker.VolumeCount != tm[WORKER].VolumeCount {
		t.Errorf("worker volume count not match %d == %d", tm[WORKER].VolumeCount, skeleton.Worker.VolumeCount)
	}
}

func TestFillWithSshKey(t *testing.T) {
	cp := make(map[string]interface{})
	cp["existingKeyPairName"] = "ssh-key-name"
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, cp, defaultNetworkParams())

	skeleton.fill(sr, cr, br, nil, nil, nj, nil)

	if skeleton.SSHKeyName != cp["existingKeyPairName"] {
		t.Errorf("ssh key name not match %s == %s", cp["existingKeyPairName"], skeleton.SSHKeyName)
	}
}

func TestFillWithSecurityMap(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	sm := make(map[string][]*models.SecurityRule)
	rules := make([]*models.SecurityRule, 0)
	rules = append(rules, &models.SecurityRule{Ports: "ports"})
	sm["master"] = rules
	sm["worker"] = rules

	skeleton.fill(sr, cr, br, nil, sm, nj, nil)

	expected := "master,worker"
	if skeleton.RemoteAccess != expected {
		t.Errorf("remote access not match %s == %s", expected, skeleton.RemoteAccess)
	}
	if skeleton.WebAccess != true {
		t.Error("web access must be true")
	}
}

func TestFillWithSecurityMapDefaultPorts(t *testing.T) {
	skeleton, sr, cr, br, nj := clusterSkeleton(nil, nil, defaultNetworkParams())

	sm := make(map[string][]*models.SecurityRule)
	rules := make([]*models.SecurityRule, 0)
	rules = append(rules, &models.SecurityRule{Ports: "22,443,9443"})
	sm["master"] = rules

	skeleton.fill(sr, cr, br, nil, sm, nj, nil)

	if skeleton.WebAccess != false {
		t.Error("web access must be false")
	}
}
