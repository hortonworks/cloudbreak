package cli

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"testing"

	"github.com/hortonworks/hdc-cli/client/cluster"
	"github.com/hortonworks/hdc-cli/client/stacks"
	"github.com/hortonworks/hdc-cli/models"
	"os"
	"os/exec"
	"strings"
)

func TestCreateClusterImplFull(t *testing.T) {
	inputs := make(map[string]string)
	inputs["key"] = "value"
	skeleton := &ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterName:   "test-cluster",
			ClusterInputs: inputs,
			InstanceRole:  "role",
			Tags:          map[string]string{"tag-key": "tag-value"},
		},
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

func executeStackCreation(skeleton *ClusterSkeleton) (actualId int64, actualStack *models.StackRequest, actualCluster *models.ClusterRequest) {
	getBlueprint := func(name string) *models.BlueprintResponse {
		return &models.BlueprintResponse{
			AmbariBlueprint: models.AmbariBlueprint{},
		}
	}
	getCredential := func(name string) models.CredentialResponse {
		return models.CredentialResponse{}
	}
	getNetwork := func(name string) models.NetworkResponse {
		return models.NetworkResponse{}
	}
	id := int64(1)
	getRdsConfig := func(string) models.RDSConfigResponse {
		return models.RDSConfigResponse{ID: &id}
	}
	stackId := int64(20)
	postStack := func(params *stacks.PostStacksUserParams) (*stacks.PostStacksUserOK, error) {
		actualStack = params.Body
		return &stacks.PostStacksUserOK{Payload: &models.StackResponse{ID: &stackId}}, nil
	}
	clusterId := int64(30)
	postCluster := func(params *cluster.PostStacksIDClusterParams) (*cluster.PostStacksIDClusterOK, error) {
		actualCluster = params.Body
		return &cluster.PostStacksIDClusterOK{Payload: &models.ClusterResponse{ID: &clusterId}}, nil
	}

	createFuncs := []func(skeleton ClusterSkeleton) *models.TemplateRequest{}
	for i := 0; i < 3; i++ {
		createFuncs = append(createFuncs, func(s ClusterSkeleton) *models.TemplateRequest {
			return &models.TemplateRequest{Name: "templ", CloudPlatform: "AWS", InstanceType: "m3.xlarge",
				VolumeCount: &(&int32Wrapper{int32(1)}).i, VolumeSize: &(&int32Wrapper{int32(100)}).i}
		})
	}
	createSecurityGroupRequest := func(skeleton ClusterSkeleton, group string) *models.SecurityGroupRequest {
		return &models.SecurityGroupRequest{CloudPlatform: "AWS", Name: "secg", SecurityRules: make([]*models.SecurityRuleRequest, 0)}
	}
	createCredentialRequest := func(name string, defaultCredential models.CredentialResponse, existingKey string) *models.CredentialRequest {
		return &models.CredentialRequest{Name: "cred", CloudPlatform: "AWS", PublicKey: "key"}
	}
	createNetworkRequest := func(skeleton ClusterSkeleton, getNetwork func(string) models.NetworkResponse) *models.NetworkRequest {
		return &models.NetworkRequest{Name: "net", CloudPlatform: "AWS"}
	}
	createRecipeRequests := func(recipes []Recipe) []*models.RecipeRequest {
		return make([]*models.RecipeRequest, 0)
	}
	createBlueprintRequest := func(skeleton ClusterSkeleton, blueprint *models.BlueprintResponse) *models.BlueprintRequest {
		return &models.BlueprintRequest{Name: "blueprint"}
	}

	actualId = createClusterImpl(*skeleton, createFuncs[0], createFuncs[1], createFuncs[2],
		createSecurityGroupRequest, createCredentialRequest, createNetworkRequest, createRecipeRequests, createBlueprintRequest,
		getBlueprint, getCredential, getNetwork, postStack, getRdsConfig, postCluster)

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
	getStack := func(string) *models.StackResponse {
		return &models.StackResponse{ID: &expectedId}
	}
	var actualId int64
	var actualUpdate models.UpdateStack
	putStack := func(params *stacks.PutStacksIDParams) error {
		actualId = params.ID
		actualUpdate = *params.Body
		return nil
	}

	expectedAdjustment := int32(2)
	resizeClusterImpl("name", WORKER, expectedAdjustment, getStack, putStack, nil)

	if actualId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actualId)
	}
	if actualUpdate.InstanceGroupAdjustment.InstanceGroup != WORKER {
		t.Errorf("type not match %s == %s", WORKER, actualUpdate.InstanceGroupAdjustment.InstanceGroup)
	}
	if actualUpdate.InstanceGroupAdjustment.ScalingAdjustment != expectedAdjustment {
		t.Errorf("type not match %d == %d", expectedAdjustment, actualUpdate.InstanceGroupAdjustment.ScalingAdjustment)
	}
	if !*actualUpdate.InstanceGroupAdjustment.WithClusterEvent {
		t.Error("with cluster event is false")
	}
}

func TestResizeClusterImplCluster(t *testing.T) {
	expectedId := int64(1)
	getStack := func(string) *models.StackResponse {
		return &models.StackResponse{ID: &expectedId}
	}
	var actualId int64
	var actualUpdate models.UpdateCluster
	putCluster := func(params *cluster.PutStacksIDClusterParams) error {
		actualId = params.ID
		actualUpdate = *params.Body
		return nil
	}

	expectedAdjustment := int32(0)
	resizeClusterImpl("name", WORKER, expectedAdjustment, getStack, nil, putCluster)

	if actualId != expectedId {
		t.Errorf("id does not match %d == %d", expectedId, actualId)
	}
	if actualUpdate.HostGroupAdjustment.HostGroup != WORKER {
		t.Errorf("type does not match %s == %s", WORKER, actualUpdate.HostGroupAdjustment.HostGroup)
	}
	if actualUpdate.HostGroupAdjustment.ScalingAdjustment != expectedAdjustment {
		t.Errorf("type does not match %d == %d", expectedAdjustment, actualUpdate.HostGroupAdjustment.ScalingAdjustment)
	}
	if !*actualUpdate.HostGroupAdjustment.WithStackUpdate {
		t.Error("with cluster event is false")
	}
}

func TestResizeClusterInvalidWorkerCount(t *testing.T) {
	expectedId := int64(1)
	getStack := func(string) *models.StackResponse {
		var instances = make([]*models.InstanceMetaData, 0)
		for i := 0; i < 3; i++ {
			instances = append(instances, &models.InstanceMetaData{})
		}
		ig := []*models.InstanceGroupResponse{{Group: WORKER, Metadata: instances}}
		return &models.StackResponse{ID: &expectedId, InstanceGroups: ig}
	}
	putCluster := func(params *cluster.PutStacksIDClusterParams) error {
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
	getStack := func(string) *models.StackResponse {
		var instances = make([]*models.InstanceMetaData, 0)
		for i := 0; i < 1; i++ {
			instances = append(instances, &models.InstanceMetaData{})
		}
		ig := []*models.InstanceGroupResponse{{Group: COMPUTE, Metadata: instances}}
		return &models.StackResponse{ID: &expectedId, InstanceGroups: ig}
	}
	putCluster := func(params *cluster.PutStacksIDClusterParams) error {
		return nil
	}
	expectedAdjustment := int32(-1)

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
	expectedMessage := "The compute host group must contain at least 1 host after the downscale"
	if !strings.Contains(got, expectedMessage) {
		t.Errorf("It should exit with validation error message: %s BUT got: %s", expectedMessage, got)
	}
}

func TestGenerateCreateSharedClusterSkeletonImplNotAvailable(t *testing.T) {
	skeleton := &ClusterSkeleton{}

	getBlueprint := func(name string) *models.BlueprintResponse {
		return &models.BlueprintResponse{
			Name: "blueprint",
			AmbariBlueprint: models.AmbariBlueprint{
				Blueprint: models.Blueprint{
					Name: &(&stringWrapper{"blueprint"}).s,
				},
			},
		}
	}
	getCluster := func(string) *models.StackResponse {
		return &models.StackResponse{
			Status:    &(&stringWrapper{"CREATED"}).s,
			NetworkID: &(&int64Wrapper{int64(1)}).i,
			Cluster:   &models.ClusterResponse{Status: &(&stringWrapper{"CREATED"}).s}}
	}

	generateCreateSharedClusterSkeletonImpl(skeleton, "name", "type", getBlueprint, getCluster, nil, nil, nil)

	expected, _ := ioutil.ReadFile("testdata/TestGenerateCreateSharedClusterSkeletonImplNotAvailable.json")
	if skeleton.Json() != string(expected) {
		t.Errorf("json does not match \n%s\n==\n%s", string(expected), skeleton.Json())
	}
}

func TestGenerateCreateSharedClusterSkeletonImplMinimalConfig(t *testing.T) {
	skeleton := &ClusterSkeleton{}

	getBlueprint := func(name string) *models.BlueprintResponse {
		return &models.BlueprintResponse{
			Name: "blueprint",
			AmbariBlueprint: models.AmbariBlueprint{
				Blueprint: models.Blueprint{
					Name: &(&stringWrapper{"blueprint"}).s,
				},
			},
		}
	}
	getCluster := func(string) *models.StackResponse {
		return &models.StackResponse{
			ID:        &(&int64Wrapper{int64(1)}).i,
			NetworkID: &(&int64Wrapper{int64(1)}).i,
			Status:    &(&stringWrapper{"AVAILABLE"}).s,
		}
	}
	getClusterConfig := func(id int64, params []*models.BlueprintParameter) []*models.BlueprintInput {
		return nil
	}
	getNetwork := func(id int64) *models.NetworkResponse {
		np := make(map[string]interface{})
		np["internetGatewayId"] = "igw"
		return &models.NetworkResponse{Parameters: np}
	}

	generateCreateSharedClusterSkeletonImpl(skeleton, "name", "type", getBlueprint, getCluster, getClusterConfig, getNetwork, nil)

	expected, _ := ioutil.ReadFile("testdata/TestGenerateCreateSharedClusterSkeletonImplMinimalConfig.json")
	if skeleton.Json() != string(expected) {
		t.Errorf("json not match \n%s\n==\n%s", string(expected), skeleton.Json())
	}
}

func TestGenerateCreateSharedClusterSkeletonImplFullConfig(t *testing.T) {
	skeleton := &ClusterSkeleton{HiveMetastore: &HiveMetastore{}}

	getBlueprint := func(name string) *models.BlueprintResponse {
		return &models.BlueprintResponse{
			Name: "blueprint",
			AmbariBlueprint: models.AmbariBlueprint{
				Blueprint: models.Blueprint{
					Name: &(&stringWrapper{"blueprint"}).s,
				},
			},
			Inputs: []*models.BlueprintParameter{{Name: &(&stringWrapper{"bp-param-name"}).s}},
		}
	}
	getCluster := func(string) *models.StackResponse {
		return &models.StackResponse{
			ID:        &(&int64Wrapper{int64(1)}).i,
			Status:    &(&stringWrapper{"AVAILABLE"}).s,
			NetworkID: &(&int64Wrapper{int64(1)}).i,
			Cluster:   &models.ClusterResponse{RdsConfigID: &(&int64Wrapper{int64(2)}).i},
		}
	}
	getClusterConfig := func(id int64, params []*models.BlueprintParameter) []*models.BlueprintInput {
		return []*models.BlueprintInput{{Name: &(&stringWrapper{"key"}).s, PropertyValue: &(&stringWrapper{"value"}).s}}
	}
	getNetwork := func(id int64) *models.NetworkResponse {
		np := make(map[string]interface{})
		np["vpcId"] = "vpcId"
		np["subnetId"] = "subnetId"
		return &models.NetworkResponse{Parameters: np}
	}
	getRdsConfig := func(id int64) *models.RDSConfigResponse {
		return &models.RDSConfigResponse{Name: "rds-name"}
	}

	generateCreateSharedClusterSkeletonImpl(skeleton, "name", "type", getBlueprint, getCluster, getClusterConfig, getNetwork, getRdsConfig)

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
