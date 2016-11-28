package cli

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"sync"
	"testing"

	"github.com/hortonworks/hdc-cli/client/blueprints"
	"github.com/hortonworks/hdc-cli/client/cluster"
	"github.com/hortonworks/hdc-cli/client/recipes"
	"github.com/hortonworks/hdc-cli/client/stacks"
	"github.com/hortonworks/hdc-cli/client/templates"
	"github.com/hortonworks/hdc-cli/models"
)

func TestFetchClusterImplReduced(t *testing.T) {
	stack := &models.StackResponse{
		Cluster: &models.ClusterResponse{
			BlueprintID: &(&int64Wrapper{int64(1)}).i,
		},
	}

	var blueprintCalled bool
	getBlueprint := func(params *blueprints.GetBlueprintsIDParams) (*blueprints.GetBlueprintsIDOK, error) {
		blueprintCalled = true
		return &blueprints.GetBlueprintsIDOK{Payload: &models.BlueprintResponse{
			Name: "blueprint",
			AmbariBlueprint: models.AmbariBlueprint{
				Blueprint: models.Blueprint{
					Name: &(&stringWrapper{"blueprint"}).s,
				},
			},
		}}, nil
	}

	fetchClusterImpl(stack, true, getBlueprint, nil, nil, nil, nil, nil, nil)

	if !blueprintCalled {
		t.Error("blueprint not called")
	}
}

func TestFetchClusterImplNotReduced(t *testing.T) {
	stack := &models.StackResponse{
		CredentialID: int64(1),
		InstanceGroups: []*models.InstanceGroupResponse{
			{
				Group:      MASTER,
				TemplateID: int64(1),
			},
		},
		Cluster: &models.ClusterResponse{RdsConfigID: &(&int64Wrapper{int64(1)}).i},
	}

	var templateCalled bool
	getTemplate := func(params *templates.GetTemplatesIDParams) (*templates.GetTemplatesIDOK, error) {
		templateCalled = true
		return &templates.GetTemplatesIDOK{
			Payload: &models.TemplateResponse{InstanceType: "type"},
		}, nil
	}

	var secDetailsCalled bool
	getSecurityDetails := func(stack *models.StackResponse) (map[string][]*models.SecurityRuleResponse, error) {
		secDetailsCalled = true
		return nil, nil
	}

	var credentialCalled bool
	getCredential := func(id int64) (*models.CredentialResponse, error) {
		credentialCalled = true
		return nil, nil
	}

	var networkCalled bool
	getNetwork := func(id int64) *models.NetworkResponse {
		networkCalled = true
		return nil
	}

	var rdsCalled bool
	getRdsConfig := func(id int64) *models.RDSConfigResponse {
		rdsCalled = true
		return nil
	}

	var recipeCalled bool
	getRecipe := func(*recipes.GetRecipesIDParams) (*recipes.GetRecipesIDOK, error) {
		recipeCalled = true
		return nil, nil
	}

	fetchClusterImpl(stack, false, nil, getTemplate, getSecurityDetails, getCredential, getNetwork, getRdsConfig, getRecipe)

	if !templateCalled {
		t.Error("template not called")
	}
	if !secDetailsCalled {
		t.Error("security details not called")
	}
	if !credentialCalled {
		t.Error("credential not called")
	}
	if !networkCalled {
		t.Error("network not called")
	}
	if !rdsCalled {
		t.Error("rds config not called")
	}
}

func TestCreateClusterImplMinimal(t *testing.T) {
	skeleton := &ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterName:              "cluster-name",
			HDPVersion:               "hdp-version",
			ClusterAndAmbariUser:     "user",
			ClusterAndAmbariPassword: "passwd",
			Worker: InstanceConfig{InstanceCount: 3},
		},
	}

	actualId, actualStack, actualCluster := executeStackCreation(skeleton)

	expectedId := int64(20)
	if actualId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actualId)
	}

	validateRequests(actualStack, "TestCreateClusterImplMinimalStack", t)
	validateRequests(actualCluster, "TestCreateClusterImplMinimalCluster", t)
}

func TestCreateClusterImplFull(t *testing.T) {
	inputs := make(map[string]string)
	inputs["key"] = "value"
	skeleton := &ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterInputs: inputs,
			InstanceRole:  "role",
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
		return nil
	}
	createTemplate := func(s ClusterSkeleton, c chan int64, wg *sync.WaitGroup) {
		defer wg.Done()
		c <- int64(1)
		c <- int64(2)
	}
	createBlueprint := func(s ClusterSkeleton, bp *models.BlueprintResponse, c chan int64, wg *sync.WaitGroup) {
		defer wg.Done()
		c <- int64(3)
	}
	createFuncs := []func(skeleton ClusterSkeleton, c chan int64, wg *sync.WaitGroup){}
	for i := 0; i < 3; i++ {
		x := i
		createFuncs = append(createFuncs, func(s ClusterSkeleton, c chan int64, wg *sync.WaitGroup) {
			defer wg.Done()
			c <- int64(x + 10)
		})
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

	createRecipes := func(skeleton ClusterSkeleton, master chan int64, worker chan int64, wg *sync.WaitGroup) {
		defer wg.Done()
		close(master)
		close(worker)
	}

	actualId = createClusterImpl(*skeleton, getBlueprint,
		createFuncs[0], createTemplate, createFuncs[1], createFuncs[2], createBlueprint,
		postStack, getRdsConfig, postCluster, createRecipes)

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
	var actialId int64
	var actualUpdate models.UpdateStack
	putStack := func(params *stacks.PutStacksIDParams) error {
		actialId = params.ID
		actualUpdate = *params.Body
		return nil
	}

	expectedAdjustment := int32(2)
	resizeClusterImpl("name", expectedAdjustment, getStack, putStack, nil)

	if actialId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actialId)
	}
	if actualUpdate.InstanceGroupAdjustment.InstanceGroup != WORKER {
		t.Errorf("type not math %s == %s", WORKER, actualUpdate.InstanceGroupAdjustment.InstanceGroup)
	}
	if actualUpdate.InstanceGroupAdjustment.ScalingAdjustment != expectedAdjustment {
		t.Errorf("type not math %d == %d", expectedAdjustment, actualUpdate.InstanceGroupAdjustment.ScalingAdjustment)
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
	var actialId int64
	var actualUpdate models.UpdateCluster
	putCluster := func(params *cluster.PutStacksIDClusterParams) error {
		actialId = params.ID
		actualUpdate = *params.Body
		return nil
	}

	expectedAdjustment := int32(0)
	resizeClusterImpl("name", expectedAdjustment, getStack, nil, putCluster)

	if actialId != expectedId {
		t.Errorf("id not match %d == %d", expectedId, actialId)
	}
	if actualUpdate.HostGroupAdjustment.HostGroup != WORKER {
		t.Errorf("type not math %s == %s", WORKER, actualUpdate.HostGroupAdjustment.HostGroup)
	}
	if actualUpdate.HostGroupAdjustment.ScalingAdjustment != expectedAdjustment {
		t.Errorf("type not math %d == %d", expectedAdjustment, actualUpdate.HostGroupAdjustment.ScalingAdjustment)
	}
	if !*actualUpdate.HostGroupAdjustment.WithStackUpdate {
		t.Error("with cluster event is false")
	}
}

func TestGenerateCreateSharedClusterSkeletonImplNoClusterName(t *testing.T) {
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

	generateCreateSharedClusterSkeletonImpl(skeleton, "", "", getBlueprint, nil, nil, nil, nil)

	expected, _ := ioutil.ReadFile("testdata/TestGenerateCreateSharedClusterSkeletonImplNoClusterName.json")
	if skeleton.Json() != string(expected) {
		t.Errorf("json not match %s == %s", string(expected), skeleton.Json())
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
			Status:  &(&stringWrapper{"CREATED"}).s,
			Cluster: &models.ClusterResponse{Status: &(&stringWrapper{"CREATED"}).s}}
	}

	generateCreateSharedClusterSkeletonImpl(skeleton, "name", "type", getBlueprint, getCluster, nil, nil, nil)

	expected, _ := ioutil.ReadFile("testdata/TestGenerateCreateSharedClusterSkeletonImplNotAvailable.json")
	if skeleton.Json() != string(expected) {
		t.Errorf("json not match %s == %s", string(expected), skeleton.Json())
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
			ID:     &(&int64Wrapper{int64(1)}).i,
			Status: &(&stringWrapper{"AVAILABLE"}).s,
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
		t.Errorf("json not match %s == %s", string(expected), skeleton.Json())
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
			ID:      &(&int64Wrapper{int64(1)}).i,
			Status:  &(&stringWrapper{"AVAILABLE"}).s,
			Cluster: &models.ClusterResponse{RdsConfigID: &(&int64Wrapper{int64(2)}).i},
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
		t.Errorf("json not match %s == %s", string(expected), skeleton.Json())
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
