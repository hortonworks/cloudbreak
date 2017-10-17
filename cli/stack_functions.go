package cli

import (
	"errors"
	"strings"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1stacks"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

func (c *Cloudbreak) waitForClusterToFinish(stackID int64, context *cli.Context) {
	if context.Bool(FlWait.Name) {
		defer utils.TimeTrack(time.Now(), "cluster installation/update")

		log.Infof("[WaitForClusterToFinish] wait for cluster to finish")
		waitForClusterToFinishImpl(stackID, c.Cloudbreak.V1stacks)
	}
}

type getStackClient interface {
	GetStack(*v1stacks.GetStackParams) (*v1stacks.GetStackOK, error)
}

func waitForClusterToFinishImpl(stackID int64, client getStackClient) {
	for {
		resp, err := client.GetStack(&v1stacks.GetStackParams{ID: stackID})

		if err != nil {
			utils.LogErrorAndExit(err)
		}

		desiredStatus := "AVAILABLE"
		stackStatus := resp.Payload.Status
		clusterStatus := resp.Payload.Cluster.Status
		log.Infof("[waitForClusterToFinishImpl] stack status: %s, cluster status: %s", stackStatus, clusterStatus)

		if stackStatus == desiredStatus && clusterStatus == desiredStatus {
			log.Infof("[waitForClusterToFinishImpl] cluster operation successfully finished")
			break
		}
		if strings.Contains(stackStatus, "FAILED") || strings.Contains(clusterStatus, "FAILED") {
			utils.LogErrorAndExit(errors.New("cluster operation failed"))
		}

		log.Infof("[waitForClusterToFinishImpl] cluster is in progress, wait for 20 seconds")
		time.Sleep(20 * time.Second)
	}
}

func (c *Cloudbreak) getStackByName(name string) *models_cloudbreak.StackResponse {
	defer utils.TimeTrack(time.Now(), "get stack by name")

	log.Infof("[getStackByName] delete autoscaling cluster, name: %s", name)
	stack, err := c.Cloudbreak.V1stacks.GetPublicStack(v1stacks.NewGetPublicStackParams().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return stack.Payload
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
