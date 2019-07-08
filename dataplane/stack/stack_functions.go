package stack

import (
	"net/http"
	"strings"
	"time"

	v4stack "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_stacks"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
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

type CloudbreakStack oauth.Cloudbreak

func (c *CloudbreakStack) waitForOperationToFinish(workspaceID int64, name string, stackStatus, clusterStatus status) {
	defer utils.TimeTrack(time.Now(), "wait for operation to finish")

	log.Infof("[waitForOperationToFinish] start waiting")
	waitForOperationToFinishImpl(workspaceID, name, stackStatus, clusterStatus, c.Cloudbreak.V4WorkspaceIDStacks)
}

type getStackClient interface {
	GetStackInWorkspaceV4(params *v4stack.GetStackInWorkspaceV4Params) (*v4stack.GetStackInWorkspaceV4OK, error)
}

func waitForOperationToFinishImpl(workspaceID int64, name string, desiredStackStatus, desiredClusterStatus status, client getStackClient) {
	for {
		resp, err := client.GetStackInWorkspaceV4(v4stack.NewGetStackInWorkspaceV4Params().WithWorkspaceID(workspaceID).WithName(name))
		var stackStatus string
		var clusterStatus string
		if err != nil {
			if err, ok := err.(*utils.RESTError); ok {
				if desiredStackStatus == DELETE_COMPLETED {
					if err.Code != http.StatusForbidden {
						utils.LogErrorAndExit(err)
					} else {
						stackStatus = DELETE_COMPLETED.String()
						clusterStatus = DELETE_COMPLETED.String()
					}
				} else {
					utils.LogErrorAndExit(err)
				}
			} else {
				utils.LogErrorAndExit(err)
			}
		}

		if stackStatus == "" {
			stackStatus = resp.Payload.Status
			clusterStatus = resp.Payload.Cluster.Status
		}
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

func (c *CloudbreakStack) getStackByName(workspaceID int64, name string) *model.StackV4Response {
	defer utils.TimeTrack(time.Now(), "get stack by name")

	log.Infof("[getStackByName] fetch stack, name: %s", name)
	stack, err := c.Cloudbreak.V4WorkspaceIDStacks.GetStackInWorkspaceV4(v4stack.NewGetStackInWorkspaceV4Params().WithWorkspaceID(workspaceID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return stack.Payload
}

func (c *CloudbreakStack) createStack(workspaceID int64, req *model.StackV4Request) *model.StackV4Response {
	var stack *model.StackV4Response
	log.Infof("[createStack] sending create stack request")
	resp, err := c.Cloudbreak.V4WorkspaceIDStacks.PostStackInWorkspaceV4(v4stack.NewPostStackInWorkspaceV4Params().WithWorkspaceID(workspaceID).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	stack = resp.Payload

	log.Infof("[createStack] stack created: %s (crn: %s)", *stack.Name, stack.Crn)
	return stack
}

func (c *CloudbreakStack) deleteStack(workspaceID int64, name string, forced bool) {
	defer utils.TimeTrack(time.Now(), "delete stack by name")

	log.Infof("[deleteStack] deleting stack, name: %s", name)
	err := c.Cloudbreak.V4WorkspaceIDStacks.DeleteStackInWorkspaceV4(v4stack.NewDeleteStackInWorkspaceV4Params().WithWorkspaceID(workspaceID).WithName(name).WithForced(&forced))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}
