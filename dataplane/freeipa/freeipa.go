package freeipa

import (
	"encoding/json"
	"fmt"
	"os"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/client/v1freeipa"
	freeIpaModel "github.com/hortonworks/cb-cli/dataplane/api-freeipa/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	commonutils "github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

type ClientFreeIpa oauth.FreeIpa

func CreateFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "create FreeIpa cluster")
	FreeIpaRequest := assembleFreeIpaRequest(c)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipa.CreateFreeIpaV1(v1freeipa.NewCreateFreeIpaV1Params().WithBody(FreeIpaRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	freeIpaCluster := resp.Payload
	if freeIpaCluster.Name != nil {
		log.Infof("[createFreeIpa] FreeIpa cluster created with name: %s", *freeIpaCluster.Name)
	}
}

func DeleteFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "delete FreeIpa cluster")
	envName := c.String(fl.FlEnvironmentName.Name)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	err := freeIpaClient.V1freeipa.DeleteFreeIpaByEnvironmentV1(v1freeipa.NewDeleteFreeIpaByEnvironmentV1Params().WithEnvironment(&envName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteFreeIpa] FreeIpa cluster delete requested in enviornment %s", envName)
}

func assembleFreeIpaRequest(c *cli.Context) *freeIpaModel.CreateFreeIpaV1Request {
	path := c.String(fl.FlInputJson.Name)
	if _, err := os.Stat(path); os.IsNotExist(err) {
		commonutils.LogErrorAndExit(err)
	}

	log.Infof("[assembleStackTemplate] read cluster create json from file: %s", path)
	content := commonutils.ReadFile(path)

	var req freeIpaModel.CreateFreeIpaV1Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		commonutils.LogErrorMessageAndExit(msg)
	}

	name := c.String(fl.FlName.Name)
	if len(name) != 0 {
		req.Name = &name
	}
	if req.Name == nil || len(*req.Name) == 0 {
		commonutils.LogErrorMessageAndExit("Name of the cluster must be set either in the template or with the --name command line option.")
	}
	return &req
}
