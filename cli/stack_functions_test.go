package cli

import (
	"testing"

	"github.com/hortonworks/cb-cli/client_cloudbreak/v1stacks"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type getStackAvailableClient struct {
}

func (c getStackAvailableClient) GetStack(*v1stacks.GetStackParams) (*v1stacks.GetStackOK, error) {
	return &v1stacks.GetStackOK{
		Payload: &models_cloudbreak.StackResponse{
			Status: "AVAILABLE",
			Cluster: &models_cloudbreak.ClusterResponse{
				Status: "AVAILABLE",
			},
		},
	}, nil
}

func TestWaitForOperationToFinishImplAvailable(t *testing.T) {
	waitForOperationToFinishImpl(int64(1), AVAILABLE, AVAILABLE, getStackAvailableClient{})
}

func TestWaitForOperationToFinishImplSkip(t *testing.T) {
	client := getStackAvailableClient{}
	waitForOperationToFinishImpl(int64(1), SKIP, AVAILABLE, client)
	waitForOperationToFinishImpl(int64(1), AVAILABLE, SKIP, client)
	waitForOperationToFinishImpl(int64(1), SKIP, SKIP, client)
}

type getStackFailedClient struct {
}

func (c getStackFailedClient) GetStack(*v1stacks.GetStackParams) (*v1stacks.GetStackOK, error) {
	return &v1stacks.GetStackOK{
		Payload: &models_cloudbreak.StackResponse{
			Status: "STOP_FAILED",
			Cluster: &models_cloudbreak.ClusterResponse{
				Status: "STOP_FAILED",
			},
		},
	}, nil
}

func TestWaitForOperationToFinishImplFailedStack(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			if r != "cluster operation failed" {
				t.Errorf("error not match cluster operation failed == %s", r)
			}
		}
	}()

	client := getStackFailedClient{}
	waitForOperationToFinishImpl(int64(1), SKIP, AVAILABLE, client)

	t.Error("Exit not happend")
}

func TestWaitForOperationToFinishImplFailedCluster(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			if r != "cluster operation failed" {
				t.Errorf("error not match cluster operation failed == %s", r)
			}
		}
	}()

	client := getStackFailedClient{}
	waitForOperationToFinishImpl(int64(1), AVAILABLE, SKIP, client)

	t.Error("Exit not happend")
}
