package freeipa

import (
	"encoding/json"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/client/freeipa_environment_name"
	freeIpaModel "github.com/hortonworks/cb-cli/dataplane/api-freeipa/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	commonutils "github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
	"os"
	"time"
)

type ClientFreeIpa oauth.FreeIpa

func CreateFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "create FreeIpa cluster")
	envName := c.String(fl.FlEnvironmentName.Name)
	FreeIpaRequest := assembleFreeIpaRequest(c)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.FreeipaEnvironmentName.CreateFreeIPA(freeipa_environment_name.NewCreateFreeIPAParams().WithEnvironmentName(envName).WithBody(FreeIpaRequest))
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
	name := c.String(fl.FlName.Name)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	err := freeIpaClient.FreeipaEnvironmentName.DeleteFreeIPA(freeipa_environment_name.NewDeleteFreeIPAParams().WithEnvironmentName(envName).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteFreeIpa] %s FreeIpa cluster delete requested", name)
}

func assembleFreeIpaRequest(c *cli.Context) *freeIpaModel.CreateFreeIpaRequest {
	path := c.String(fl.FlInputJson.Name)
	if _, err := os.Stat(path); os.IsNotExist(err) {
		commonutils.LogErrorAndExit(err)
	}

	log.Infof("[assembleStackTemplate] read cluster create json from file: %s", path)
	content := commonutils.ReadFile(path)

	var req freeIpaModel.CreateFreeIpaRequest
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		commonutils.LogErrorMessageAndExit(msg)
	}

	name := c.String(fl.FlName.Name)
	if len(name) != 0 {
		req.Name = &name
		if req.InstanceGroups != nil {
			for _, ig := range req.InstanceGroups {
				if ig.Azure != nil && ig.Azure.AvailabilitySet != nil {
					as := ig.Azure.AvailabilitySet
					as.Name = *req.Name + "-" + *ig.Name + "-as"
				}
			}
		}
	}
	if req.Name == nil || len(*req.Name) == 0 {
		commonutils.LogErrorMessageAndExit("Name of the cluster must be set either in the template or with the --name command line option.")
	}
	return &req
}
