package cli

import (
	"testing"

	"github.com/hortonworks/hdc-cli/client/blueprints"
	"github.com/hortonworks/hdc-cli/client/cluster"
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

	fetchClusterImpl(stack, true, getBlueprint, nil, nil, nil, nil, nil)

	if !blueprintCalled {
		t.Error("blueprint not called")
	}
}

func TestFetchClusterImplNotReduced(t *testing.T) {
	stack := &models.StackResponse{
		CredentialID: int64(1),
		InstanceGroups: []*models.InstanceGroup{
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
	getSecurityDetails := func(stack *models.StackResponse) (map[string][]*models.SecurityRule, error) {
		secDetailsCalled = true
		return nil, nil
	}

	var credentialCalled bool
	getCredential := func(id int64) (*models.CredentialResponse, error) {
		credentialCalled = true
		return nil, nil
	}

	var networkCalled bool
	getNetwork := func(id int64) *models.NetworkJSON {
		networkCalled = true
		return nil
	}

	var rdsCalled bool
	getRdsConfig := func(id int64) *models.RDSConfigResponse {
		rdsCalled = true
		return nil
	}

	fetchClusterImpl(stack, false, nil, getTemplate, getSecurityDetails, getCredential, getNetwork, getRdsConfig)

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

func TestDescribeClusterImpl(t *testing.T) {
	fetchCluster := func(*models.StackResponse, bool) (*ClusterSkeleton, error) {
		return &ClusterSkeleton{ClusterAndAmbariPassword: "password"}, nil
	}

	skeleton := describeClusterImpl("name", func(string) *models.StackResponse { return nil }, fetchCluster)

	if len(skeleton.ClusterAndAmbariPassword) != 0 {
		t.Errorf("password not cleaned up, %s", skeleton.ClusterAndAmbariPassword)
	}
}

func TestCreateClusterImpl(t *testing.T) {

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

func TestGenerateCreateSharedClusterSkeletonImpl(t *testing.T) {

}
