package distrox

import (
	"net/http"
	"strings"
	"time"

	v1distrox "github.com/hortonworks/cb-cli/dataplane/api/client/v1distrox"
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

type DistroX oauth.Cloudbreak

func (c *DistroX) WaitForDistroXOperationToFinish(name string, stackStatus, clusterStatus status) {
	defer utils.TimeTrack(time.Now(), "wait for operation to finish")

	log.Infof("[waitForOperationToFinish] start waiting")
	waitForDistroXOperationToFinishImpl(name, stackStatus, clusterStatus, c.Cloudbreak.V1distrox)
}

type getDistroXClient interface {
	GetDistroXV1ByName(params *v1distrox.GetDistroXV1ByNameParams) (*v1distrox.GetDistroXV1ByNameOK, error)
}

func waitForDistroXOperationToFinishImpl(name string, desiredStackStatus status, desiredClusterStatus status, client getDistroXClient) {
	for {
		resp, err := client.GetDistroXV1ByName(v1distrox.NewGetDistroXV1ByNameParams().WithName(name))
		var stackStatus string
		var dxStatus string
		if err != nil {
			if err, ok := err.(*utils.RESTError); ok {
				if desiredStackStatus == DELETE_COMPLETED {
					if err.Code != http.StatusForbidden {
						utils.LogErrorAndExit(err)
					} else {
						stackStatus = DELETE_COMPLETED.String()
						dxStatus = DELETE_COMPLETED.String()
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
			dxStatus = resp.Payload.Cluster.Status
		}
		log.Infof("[waitForDistroXToFinishImpl] DistroX status: %s, DistroX status: %s", stackStatus, dxStatus)

		if (desiredStackStatus == SKIP || stackStatus == desiredStackStatus.String()) && (desiredClusterStatus == SKIP || dxStatus == desiredClusterStatus.String()) {
			log.Infof("[waitForDistroXToFinishImpl] DistroX operation successfully finished")
			break
		}
		if strings.Contains(stackStatus, "FAILED") || strings.Contains(dxStatus, "FAILED") {
			utils.LogErrorMessageAndExit("DistroX operation failed")
		}

		log.Infof("[waitForDistroXToFinishImpl] DistroX operation is in progress, wait for 20 seconds")
		time.Sleep(20 * time.Second)
	}
}

func (c *DistroX) deleteDistroX(name string, forced bool) {
	defer utils.TimeTrack(time.Now(), "delete DistroX by name")

	log.Infof("[deleteDistroX] deleting DistroX, name: %s", name)
	err := c.Cloudbreak.V1distrox.DeleteDistroXV1ByName(v1distrox.NewDeleteDistroXV1ByNameParams().WithName(name).WithForced(&forced))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func (c *DistroX) createDistroX(req *model.DistroXV1Request) *model.StackV4Response {
	var dx *model.StackV4Response
	log.Infof("[createDistroX] sending create DistroX request")
	resp, err := c.Cloudbreak.V1distrox.PostDistroXV1(v1distrox.NewPostDistroXV1Params().WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	dx = resp.Payload

	log.Infof("[createDistroX] DistroX created: %s (crn: %s)", *dx.Name, dx.Crn)
	return dx
}
