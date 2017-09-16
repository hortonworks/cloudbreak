package cli

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"testing"

	"os"
	"os/exec"
	"strings"

	"github.com/hortonworks/hdc-cli/client_cloudbreak/cluster"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/stacks"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func TestCreateClusterImplFull(t *testing.T) {
	inputs := make(map[string]string)
	inputs["key"] = "value"
	skeleton := &ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterName:  "test-cluster",
			InstanceRole: "role",
			Tags:         map[string]string{"tag-key": "tag-value"},
		},
		ClusterInputs: inputs,
		HiveMetastore: &HiveMetastore{},
	}
	skeleton.HiveMetastore.Name = "ms-name"
	skeleton.HiveMetastore.Username = "ms-user"
	skeleton.HiveMetastore.Password = "ms-passwd"
	skeleton.HiveMetastore.URL = "ms-url"
	skeleton.HiveMetastore.DatabaseType = POSTGRES

	_, actualStack, actualCluster := executeStackCreation(skeleton)

	validateRequests(actualStack, "TestCreateClusterImplFullStack", t)
	validateRequests(actualCluster, "TestCreateClusterImplFullCluster", t)
}

func TestCreateClusterImplNewRole(t *testing.T) {
	skeleton := &ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			InstanceRole: "CREATE",
		},
	}

	_, actualStack, _ := executeStackCreation(skeleton)

	validateRequests(actualStack, "TestCreateClusterImplNewRoleStack", t)
}

func TestCreateClusterImplExistingRds(t *testing.T) {
	skeleton := &ClusterSkeleton{HiveMetastore: &HiveMetastore{}}
	skeleton.HiveMetastore.Name = "ms-name"

	_, _, actualCluster := executeStackCreation(skeleton)

	validateRequests(actualCluster, "TestCreateClusterImplExistingRdsCluster", t)
}

func executeStackCreation(skeleton *ClusterSkeleton) (actualId int64, actualStack *models_cloudbreak.StackRequest, actualCluster *models_cloudbreak.ClusterRequest) {
	getBlueprint := func(name string) *models_cloudbreak.BlueprintResponse {
		return &models_cloudbreak.BlueprintResponse{
		// AmbariBlueprint: models_cloudbreak.AmbariBlueprint{},
		}
	}
	getNetwork := func(name string) models_cloudbreak.NetworkResponse {
		return models_cloudbreak.NetworkResponse{}
	}
	id := int64(1)
	getRdsConfig := func(string) models_cloudbreak.RDSConfigResponse {
		return models_cloudbreak.RDSConfigResponse{ID: id}
	}
	stackId := int64(20)
	postStack := func(params *stacks.PostPrivateStackParams) (*stacks.PostPrivateStackOK, error) {
		actualStack = params.Body
		return &stacks.PostPrivateStackOK{Payload: &models_cloudbreak.StackResponse{ID: stackId}}, nil
	}
	clusterId := int64(30)
	postCluster := func(params *cluster.PostClusterParams) (*cluster.PostClusterOK, error) {
		actualCluster = params.Body
		return &cluster.PostClusterOK{Payload: &models_cloudbreak.ClusterResponse{ID: clusterId}}, nil
	}

	createFuncs := []func(skeleton ClusterSkeleton) *models_cloudbreak.TemplateRequest{}
	for i := 0; i < 3; i++ {
		createFuncs = append(createFuncs, func(s ClusterSkeleton) *models_cloudbreak.TemplateRequest {
			return &models_cloudbreak.TemplateRequest{Name: &(&stringWrapper{"templ"}).s, CloudPlatform: &(&stringWrapper{"AWS"}).s, InstanceType: &(&stringWrapper{"m3.xlarge"}).s,
				VolumeCount: int32(1), VolumeSize: int32(100)}
		})
	}
	createSecurityGroupRequest := func(skeleton ClusterSkeleton, group string) *models_cloudbreak.SecurityGroupRequest {
		return &models_cloudbreak.SecurityGroupRequest{CloudPlatform: &(&stringWrapper{"AWS"}).s, Name: &(&stringWrapper{"secg"}).s, SecurityRules: make([]*models_cloudbreak.SecurityRuleRequest, 0)}
	}
	createNetworkRequest := func(skeleton ClusterSkeleton, getNetwork func(string) models_cloudbreak.NetworkResponse) *models_cloudbreak.NetworkRequest {
		return &models_cloudbreak.NetworkRequest{Name: &(&stringWrapper{"net"}).s, CloudPlatform: &(&stringWrapper{"AWS"}).s}
	}
	createRecipeRequests := func(recipes []Recipe) []*models_cloudbreak.RecipeRequest {
		return make([]*models_cloudbreak.RecipeRequest, 0)
	}
	createBlueprintRequest := func(skeleton ClusterSkeleton, blueprint *models_cloudbreak.BlueprintResponse) *models_cloudbreak.BlueprintRequest {
		return &models_cloudbreak.BlueprintRequest{Name: &(&stringWrapper{"blueprint"}).s}
	}
	addAutoscalingCluster := func(stackId int64, autoscalingEnabled bool) int64 {
		return stackId
	}
	setScalingConfigurations := func(int64, AutoscalingConfiguration) {
	}
	addPrometheusAlert := func(int64, AutoscalingPolicy) int64 {
		return stackId
	}
	addScalingPolicy := func(int64, AutoscalingPolicy, int64) int64 {
		return stackId
	}
	getLdapConfig := func(string) *models_cloudbreak.LdapConfigResponse {
		return &models_cloudbreak.LdapConfigResponse{ID: 1}
	}
	getCluster := func(string) *models_cloudbreak.StackResponse {
		return nil
	}
	getFlex := func(string) *models_cloudbreak.FlexSubscriptionResponse {
		return nil
	}

	actualId = createClusterImpl(*skeleton, createFuncs[0], createFuncs[1], createFuncs[2],
		createSecurityGroupRequest, createNetworkRequest, createRecipeRequests, createBlueprintRequest, createRDSRequest,
		getBlueprint, getNetwork, postStack, getRdsConfig, postCluster, addAutoscalingCluster, setScalingConfigurations, addPrometheusAlert,
		addScalingPolicy, getLdapConfig, getCluster, getFlex)

	return
}

func validateRequests(actual interface{}, expectedName string, t *testing.T) {
	writer := &bytes.Buffer{}
	encoder := json.NewEncoder(writer)
	encoder.Encode(actual)
	expected, _ := ioutil.ReadFile("testdata/" + expectedName + ".json")
	if writer.String() != string(expected) {
		t.Errorf("json not match %s == %s", string(expected), writer.String())
	}
}

func TestResizeClusterImplStack(t *testing.T) {
	expectedId := int64(1)
	getStack := func(string) *models_cloudbreak.StackResponse {
		return &models_cloudbreak.StackResponse{ID: expectedId}
	}
	var actualId int64
	var actualUpdate models_cloudbreak.UpdateStack
	putStack := func(params *stacks.PutStackParams) error {
		actualId = params.ID
		actualUpdate = *params.Body
		return nil
	}

	expectedAdjustment := int32(2)
	resizeClusterImpl("name", WORKER, expectedAdjustment, getStack, putStack, nil)

	if actualId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actualId)
	}
	if *actualUpdate.InstanceGroupAdjustment.InstanceGroup != WORKER {
		t.Errorf("type not match %s == %s", WORKER, *actualUpdate.InstanceGroupAdjustment.InstanceGroup)
	}
	if *actualUpdate.InstanceGroupAdjustment.ScalingAdjustment != expectedAdjustment {
		t.Errorf("type not match %d == %d", expectedAdjustment, actualUpdate.InstanceGroupAdjustment.ScalingAdjustment)
	}
	if !*actualUpdate.InstanceGroupAdjustment.WithClusterEvent {
		t.Error("with cluster event is false")
	}
}

func TestResizeClusterImplCluster(t *testing.T) {
	expectedId := int64(1)
	getStack := func(string) *models_cloudbreak.StackResponse {
		return &models_cloudbreak.StackResponse{ID: expectedId}
	}
	var actualId int64
	var actualUpdate models_cloudbreak.UpdateCluster
	putCluster := func(params *cluster.PutClusterParams) error {
		actualId = params.ID
		actualUpdate = *params.Body
		return nil
	}

	expectedAdjustment := int32(0)
	resizeClusterImpl("name", WORKER, expectedAdjustment, getStack, nil, putCluster)

	if actualId != expectedId {
		t.Errorf("id does not match %d == %d", expectedId, actualId)
	}
	if *actualUpdate.HostGroupAdjustment.HostGroup != WORKER {
		t.Errorf("type does not match %s == %s", WORKER, *actualUpdate.HostGroupAdjustment.HostGroup)
	}
	if *actualUpdate.HostGroupAdjustment.ScalingAdjustment != expectedAdjustment {
		t.Errorf("type does not match %d == %d", expectedAdjustment, actualUpdate.HostGroupAdjustment.ScalingAdjustment)
	}
	if !*actualUpdate.HostGroupAdjustment.WithStackUpdate {
		t.Error("with cluster event is false")
	}
}

func TestResizeClusterInvalidWorkerCount(t *testing.T) {
	expectedId := int64(1)
	getStack := func(string) *models_cloudbreak.StackResponse {
		var instances = make([]*models_cloudbreak.InstanceMetaData, 0)
		for i := 0; i < 3; i++ {
			instances = append(instances, &models_cloudbreak.InstanceMetaData{})
		}
		ig := []*models_cloudbreak.InstanceGroupResponse{{Group: &WORKER, Metadata: instances}}
		return &models_cloudbreak.StackResponse{ID: expectedId, InstanceGroups: ig}
	}
	putCluster := func(params *cluster.PutClusterParams) error {
		return nil
	}
	expectedAdjustment := int32(-1)

	// Only run the failing part when a specific env variable is set
	if os.Getenv("OS_EXIT") == "1" {
		resizeClusterImpl("name", WORKER, expectedAdjustment, getStack, nil, putCluster)
		return
	}

	// Start the actual test in a different subprocess
	cmd := exec.Command(os.Args[0], "-test.v", "-test.run=TestResizeClusterInvalidWorkerCount")
	cmd.Env = append(cmd.Env, "OS_EXIT=1")
	cmd.Env = append(cmd.Env, "GOPATH="+os.Getenv("GOPATH"))
	stdout, err := cmd.CombinedOutput()
	if err != nil {
		t.Error(err)
	}

	// Check that the log fatal message is what we expected
	got := string(stdout)
	expectedMessage := "You cannot scale down the worker host group below 3, because it can cause data loss"
	if !strings.Contains(got, expectedMessage) {
		t.Errorf("It should exit with validation error message: %s BUT got: %s", expectedMessage, got)
	}
}

func TestResizeClusterInvalidComputeCount(t *testing.T) {
	expectedId := int64(1)
	getStack := func(string) *models_cloudbreak.StackResponse {
		var instances = make([]*models_cloudbreak.InstanceMetaData, 0)
		for i := 0; i < 1; i++ {
			instances = append(instances, &models_cloudbreak.InstanceMetaData{})
		}
		ig := []*models_cloudbreak.InstanceGroupResponse{{Group: &COMPUTE, Metadata: instances}}
		return &models_cloudbreak.StackResponse{ID: expectedId, InstanceGroups: ig}
	}
	putCluster := func(params *cluster.PutClusterParams) error {
		return nil
	}
	expectedAdjustment := int32(-2)

	// Only run the failing part when a specific env variable is set
	if os.Getenv("OS_EXIT") == "1" {
		resizeClusterImpl("name", COMPUTE, expectedAdjustment, getStack, nil, putCluster)
		return
	}

	// Start the actual test in a different subprocess
	cmd := exec.Command(os.Args[0], "-test.v", "-test.run=TestResizeClusterInvalidComputeCount")
	cmd.Env = append(cmd.Env, "OS_EXIT=1")
	cmd.Env = append(cmd.Env, "GOPATH="+os.Getenv("GOPATH"))
	stdout, err := cmd.CombinedOutput()
	if err != nil {
		t.Error(err)
	}

	// Check that the log fatal message is what we expected
	got := string(stdout)
	expectedMessage := "You cannot scale the compute nodes below 0"
	if !strings.Contains(got, expectedMessage) {
		t.Errorf("It should exit with validation error message: %s BUT got: %s", expectedMessage, got)
	}
}

func TestGenerateCreateSharedClusterSkeletonImplNotAvailable(t *testing.T) {
	skeleton := &ClusterSkeleton{}

	getBlueprint := func(name string) *models_cloudbreak.BlueprintResponse {
		return &models_cloudbreak.BlueprintResponse{
			Name:            &(&stringWrapper{"blueprint"}).s,
			AmbariBlueprint: "{\"Blueprints\": {\"blueprint_name\"\": \"blueprint\"}}",
		}
	}
	getCluster := func(string) *models_cloudbreak.StackResponse {
		return &models_cloudbreak.StackResponse{
			Status:    "CREATED",
			NetworkID: 1,
			Cluster:   &models_cloudbreak.ClusterResponse{Status: "CREATED"}}
	}

	fillSharedParameters(skeleton, getBlueprint, getCluster)

	expected, _ := ioutil.ReadFile("testdata/TestGenerateCreateSharedClusterSkeletonImplNotAvailable.json")
	if skeleton.Json() != string(expected) {
		t.Errorf("json does not match \n%s\n==\n%s", string(expected), skeleton.Json())
	}
}

func TestGenerateCreateSharedClusterSkeletonImplMinimalConfig(t *testing.T) {
	skeleton := &ClusterSkeleton{}

	getBlueprint := func(name string) *models_cloudbreak.BlueprintResponse {
		return &models_cloudbreak.BlueprintResponse{
			Name:            &(&stringWrapper{"blueprint"}).s,
			AmbariBlueprint: "{\"Blueprints\": {\"blueprint_name\"\": \"blueprint\"}}",
		}
	}
	getCluster := func(string) *models_cloudbreak.StackResponse {
		np := make(map[string]interface{})
		np["internetGatewayId"] = "igw"
		networResp := &models_cloudbreak.NetworkResponse{Parameters: np}
		return &models_cloudbreak.StackResponse{
			ID:        1,
			NetworkID: 1,
			Status:    "AVAILABLE",
			Network:   networResp,
		}
	}

	fillSharedParameters(skeleton, getBlueprint, getCluster)

	expected, _ := ioutil.ReadFile("testdata/TestGenerateCreateSharedClusterSkeletonImplMinimalConfig.json")
	if skeleton.Json() != string(expected) {
		t.Errorf("json not match \n%s\n==\n%s", string(expected), skeleton.Json())
	}
}

func TestGenerateCreateSharedClusterSkeletonImplFullConfig(t *testing.T) {
	skeleton := &ClusterSkeleton{HiveMetastore: &HiveMetastore{}}

	getBlueprint := func(name string) *models_cloudbreak.BlueprintResponse {
		return &models_cloudbreak.BlueprintResponse{
			Name:            &(&stringWrapper{"blueprint"}).s,
			AmbariBlueprint: "{\"Blueprints\": {\"blueprint_name\"\": \"blueprint\"}}",
			Inputs:          []*models_cloudbreak.BlueprintParameter{{Name: "bp-param-name"}},
		}
	}
	getCluster := func(string) *models_cloudbreak.StackResponse {
		np := make(map[string]interface{})
		np["vpcId"] = "vpcId"
		np["subnetId"] = "subnetId"
		network := &models_cloudbreak.NetworkResponse{Parameters: np}
		return &models_cloudbreak.StackResponse{
			ID:      1,
			Status:  "AVAILABLE",
			Network: network,
			Cluster: &models_cloudbreak.ClusterResponse{RdsConfigs: []*models_cloudbreak.RDSConfigResponse{{Name: &(&stringWrapper{"rds-name"}).s, Type: HIVE_RDS}}},
		}
	}

	fillSharedParameters(skeleton, getBlueprint, getCluster)

	expected, _ := ioutil.ReadFile("testdata/TestGenerateCreateSharedClusterSkeletonImplFullConfig.json")
	if skeleton.Json() != string(expected) {
		t.Errorf("json does not match \n%s\n==\n%s", string(expected), skeleton.Json())
	}
}

func TestGenerateBaseSkeletonWithValidClusterType(t *testing.T) {
	skeleton := getBaseSkeleton()
	valid := false
	for _, v := range BlueprintMap {
		if v == skeleton.ClusterType {
			valid = true
			break
		}
	}
	if !valid {
		t.Errorf("the generated cluster skeleton contains an invalid cluster type: %s", skeleton.ClusterType)
	}
}
