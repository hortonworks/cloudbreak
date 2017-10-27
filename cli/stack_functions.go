package cli

import (
	"strings"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1stacks"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v2stacks"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
)

type status string

func (c status) String() string {
	return string(c)
}

const (
	AVAILABLE        = status("AVAILABLE")
	STOPPED          = status("STOPPED")
	DELETE_COMPLETED = status("DELETE_COMPLETED")
	SKIP             = status("")
)

func (c *Cloudbreak) waitForOperationToFinish(name string, stackStatus, clusterStatus status) {
	defer utils.TimeTrack(time.Now(), "wait for operation to finish")

	resp := c.getStackByName(name)

	log.Infof("[waitForOperationToFinish] start waiting")
	waitForOperationToFinishImpl(resp.ID, stackStatus, clusterStatus, c.Cloudbreak.V1stacks)
}

type getStackClient interface {
	GetStack(*v1stacks.GetStackParams) (*v1stacks.GetStackOK, error)
}

func waitForOperationToFinishImpl(stackID int64, desiredStackStatus, desiredClusterStatus status, client getStackClient) {
	for {
		resp, err := client.GetStack(v1stacks.NewGetStackParams().WithID(stackID))
		if err != nil {
			utils.LogErrorAndExit(err)
		}

		stackStatus := resp.Payload.Status
		clusterStatus := resp.Payload.Cluster.Status
		log.Infof("[waitForClusterToFinishImpl] stack status: %s, cluster status: %s", stackStatus, clusterStatus)

		if (desiredStackStatus == SKIP || stackStatus == desiredStackStatus.String()) && (desiredClusterStatus == SKIP || clusterStatus == desiredClusterStatus.String()) {
			log.Infof("[waitForClusterToFinishImpl] cluster operation successfully finished")
			break
		}
		if strings.Contains(stackStatus, "FAILED") || strings.Contains(clusterStatus, "FAILED") {
			utils.LogErrorMessageAndExit("cluster operation failed")
		}

		log.Infof("[waitForClusterToFinishImpl] cluster operation is in progress, wait for 20 seconds")
		time.Sleep(20 * time.Second)
	}
}

func (c *Cloudbreak) getStackByName(name string) *models_cloudbreak.StackResponse {
	defer utils.TimeTrack(time.Now(), "get stack by name")

	log.Infof("[getStackByName] fetch stack, name: %s", name)
	stack, err := c.Cloudbreak.V1stacks.GetPublicStack(v1stacks.NewGetPublicStackParams().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return stack.Payload
}

func (c *Cloudbreak) createStack(req *models_cloudbreak.StackV2Request, public bool) *models_cloudbreak.StackResponse {
	var stack *models_cloudbreak.StackResponse
	if public {
		log.Infof("[createStack] sending create public stack request")
		resp, err := c.Cloudbreak.V2stacks.PostPublicStackV2(v2stacks.NewPostPublicStackV2Params().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		stack = resp.Payload
	} else {
		log.Infof("[createStack] sending create private stack request")
		resp, err := c.Cloudbreak.V2stacks.PostPrivateStackV2(v2stacks.NewPostPrivateStackV2Params().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		stack = resp.Payload
	}
	log.Infof("[createStack] stack created: %s (id: %d)", *stack.Name, stack.ID)
	return stack
}

func (c *Cloudbreak) deleteStack(name string, forced bool, public bool) {
	defer utils.TimeTrack(time.Now(), "delete stack by name")

	log.Infof("[deleteStack] deleting stack, name: %s", name)
	var err error
	if public {
		err = c.Cloudbreak.V1stacks.DeletePublicStack(v1stacks.NewDeletePublicStackParams().WithName(name).WithForced(&forced))
	} else {
		err = c.Cloudbreak.V1stacks.DeletePrivateStack(v1stacks.NewDeletePrivateStackParams().WithName(name).WithForced(&forced))
	}
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}
