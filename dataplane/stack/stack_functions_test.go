package stack

import (
	"testing"

	v4stack "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_stacks"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
)

type getStackAvailableClient struct {
}

func (c getStackAvailableClient) GetStackInWorkspaceV4(params *v4stack.GetStackInWorkspaceV4Params) (*v4stack.GetStackInWorkspaceV4OK, error) {
	return &v4stack.GetStackInWorkspaceV4OK{
		Payload: &model.StackV4Response{
			Status: "AVAILABLE",
			Cluster: &model.ClusterV4Response{
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

func (c getStackFailedClient) GetStackInWorkspaceV4(*v4stack.GetStackInWorkspaceV4Params) (*v4stack.GetStackInWorkspaceV4OK, error) {
	return &v4stack.GetStackInWorkspaceV4OK{
		Payload: &model.StackV4Response{
			Status: "STOP_FAILED",
			Cluster: &model.ClusterV4Response{
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
