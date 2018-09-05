package cli

import (
	"testing"

	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_organization_id_stacks"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type getStackAvailableClient struct {
}

func (c getStackAvailableClient) GetStackInOrganization(*v3_organization_id_stacks.GetStackInOrganizationParams) (*v3_organization_id_stacks.GetStackInOrganizationOK, error) {
	return &v3_organization_id_stacks.GetStackInOrganizationOK{
		Payload: &models_cloudbreak.StackResponse{
			Status: "AVAILABLE",
			Cluster: &models_cloudbreak.ClusterResponse{
				Status: "AVAILABLE",
			},
		},
	}, nil
}

func TestWaitForOperationToFinishImplAvailable(t *testing.T) {
	t.Parallel()

	waitForOperationToFinishImpl(int64(2), "name", AVAILABLE, AVAILABLE, getStackAvailableClient{})
}

func TestWaitForOperationToFinishImplSkip(t *testing.T) {
	t.Parallel()

	client := getStackAvailableClient{}
	waitForOperationToFinishImpl(int64(2), "name", SKIP, AVAILABLE, client)
	waitForOperationToFinishImpl(int64(2), "name", AVAILABLE, SKIP, client)
	waitForOperationToFinishImpl(int64(2), "name", SKIP, SKIP, client)
}

type getStackFailedClient struct {
}

func (c getStackFailedClient) GetStackInOrganization(*v3_organization_id_stacks.GetStackInOrganizationParams) (*v3_organization_id_stacks.GetStackInOrganizationOK, error) {
	return &v3_organization_id_stacks.GetStackInOrganizationOK{
		Payload: &models_cloudbreak.StackResponse{
			Status: "STOP_FAILED",
			Cluster: &models_cloudbreak.ClusterResponse{
				Status: "STOP_FAILED",
			},
		},
	}, nil
}

func TestWaitForOperationToFinishImplFailedStack(t *testing.T) {
	t.Parallel()

	defer func() {
		if r := recover(); r != nil {
			if r != "cluster operation failed" {
				t.Errorf("error not match cluster operation failed == %s", r)
			}
		}
	}()

	client := getStackFailedClient{}
	waitForOperationToFinishImpl(int64(2), "name", SKIP, AVAILABLE, client)

	t.Error("Exit not happened")
}

func TestWaitForOperationToFinishImplFailedCluster(t *testing.T) {
	t.Parallel()

	defer func() {
		if r := recover(); r != nil {
			if r != "cluster operation failed" {
				t.Errorf("error not match cluster operation failed == %s", r)
			}
		}
	}()

	client := getStackFailedClient{}
	waitForOperationToFinishImpl(int64(2), "name", AVAILABLE, SKIP, client)

	t.Error("Exit not happened")
}
