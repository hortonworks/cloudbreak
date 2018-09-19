package stack

import (
	"testing"

	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v3_workspace_id_stacks"
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
)

type getStackAvailableClient struct {
}

func (c getStackAvailableClient) GetStackInWorkspace(*v3_workspace_id_stacks.GetStackInWorkspaceParams) (*v3_workspace_id_stacks.GetStackInWorkspaceOK, error) {
	return &v3_workspace_id_stacks.GetStackInWorkspaceOK{
		Payload: &model.StackResponse{
			Status: "AVAILABLE",
			Cluster: &model.ClusterResponse{
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

func (c getStackFailedClient) GetStackInWorkspace(*v3_workspace_id_stacks.GetStackInWorkspaceParams) (*v3_workspace_id_stacks.GetStackInWorkspaceOK, error) {
	return &v3_workspace_id_stacks.GetStackInWorkspaceOK{
		Payload: &model.StackResponse{
			Status: "STOP_FAILED",
			Cluster: &model.ClusterResponse{
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
