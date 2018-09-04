package cli

import (
	"strings"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_organization_id_stacks"
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

func (c *Cloudbreak) waitForOperationToFinish(orgID int64, name string, stackStatus, clusterStatus status) {
	defer utils.TimeTrack(time.Now(), "wait for operation to finish")

	log.Infof("[waitForOperationToFinish] start waiting")
	waitForOperationToFinishImpl(orgID, name, stackStatus, clusterStatus, c.Cloudbreak.V3OrganizationIDStacks)
}

type getStackClient interface {
	GetStackInOrganization(*v3_organization_id_stacks.GetStackInOrganizationParams) (*v3_organization_id_stacks.GetStackInOrganizationOK, error)
}

func waitForOperationToFinishImpl(orgID int64, name string, desiredStackStatus, desiredClusterStatus status, client getStackClient) {
	for {
		resp, err := client.GetStackInOrganization(v3_organization_id_stacks.NewGetStackInOrganizationParams().WithOrganizationID(orgID).WithName(name))
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

func (c *Cloudbreak) getStackByName(orgID int64, name string) *models_cloudbreak.StackResponse {
	defer utils.TimeTrack(time.Now(), "get stack by name")

	log.Infof("[getStackByName] fetch stack, name: %s", name)
	stack, err := c.Cloudbreak.V3OrganizationIDStacks.GetStackInOrganization(v3_organization_id_stacks.NewGetStackInOrganizationParams().WithOrganizationID(orgID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return stack.Payload
}

func (c *Cloudbreak) createStack(orgID int64, req *models_cloudbreak.StackV2Request) *models_cloudbreak.StackResponse {
	var stack *models_cloudbreak.StackResponse
	log.Infof("[createStack] sending create stack request")
	resp, err := c.Cloudbreak.V3OrganizationIDStacks.CreateStackInOrganization(v3_organization_id_stacks.NewCreateStackInOrganizationParams().WithOrganizationID(orgID).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	stack = resp.Payload

	log.Infof("[createStack] stack created: %s (id: %d)", *stack.Name, stack.ID)
	return stack
}

func (c *Cloudbreak) deleteStack(orgID int64, name string, forced bool) {
	defer utils.TimeTrack(time.Now(), "delete stack by name")

	log.Infof("[deleteStack] deleting stack, name: %s", name)
	err := c.Cloudbreak.V3OrganizationIDStacks.DeleteStackInOrganization(v3_organization_id_stacks.NewDeleteStackInOrganizationParams().WithOrganizationID(orgID).WithName(name).WithForced(&forced))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}
