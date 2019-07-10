package freeipa

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/client/v1freeipa"
	"github.com/hortonworks/cb-cli/dataplane/api-freeipa/client/v1freeipauser"
	freeIpaModel "github.com/hortonworks/cb-cli/dataplane/api-freeipa/model"
	"github.com/hortonworks/cb-cli/dataplane/env"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	commonutils "github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
	"os"
)

type ClientFreeIpa oauth.FreeIpa

type freeIpaOutDescibe struct {
	FreeIpa *freeIpaModel.DescribeFreeIpaV1Response `json:"freeIpa" yaml:"freeIpa"`
}

var header = []string{"CRN", "Name", "Status", "Status reason"}

func CreateFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "create FreeIpa cluster")
	FreeIpaRequest := assembleFreeIpaRequest(c)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipa.CreateFreeIpaV1(v1freeipa.NewCreateFreeIpaV1Params().WithBody(FreeIpaRequest))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	freeIpaCluster := resp.Payload
	if freeIpaCluster.Name != nil {
		log.Infof("[createFreeIpa] FreeIpa cluster created with name: %s", *freeIpaCluster.Name)
	}
}

func DeleteFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "delete FreeIpa cluster")
	envName := c.String(fl.FlEnvironmentName.Name)
	envCrn := env.GetEnvirontmentCrnByName(c, envName)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	err := freeIpaClient.V1freeipa.DeleteFreeIpaByEnvironmentV1(v1freeipa.NewDeleteFreeIpaByEnvironmentV1Params().WithEnvironment(&envCrn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	log.Infof("[deleteFreeIpa] FreeIpa cluster delete requested in enviornment %s", envName)
}

func DescribeFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "describe FreeIpa cluster")
	envName := c.String(fl.FlEnvironmentName.Name)
	envCrn := env.GetEnvirontmentCrnByName(c, envName)
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipa.GetFreeIpaByEnvironmentV1(v1freeipa.NewGetFreeIpaByEnvironmentV1Params().WithEnvironment(&envCrn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	log.Infof("[describeFreeIpa] FreeIpa cluster describe requested in enviornment %s", envName)
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	iparesp := resp.Payload
	freeIpaOut := freeIpaOutDescibe{
		iparesp,
	}

	output.Write(header, &freeIpaOut)
}

func ListFreeIpa(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "list FreeIpa clusters")
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipa.ListFreeIpaClustersByAccountV1(v1freeipa.NewListFreeIpaClustersByAccountV1Params())
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	log.Infof("[listFreeIpa] FreeIpa clusters list requested.")
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}

	var tableRows []commonutils.Row

	for _, response := range resp.Payload {
		row := &freeIpaDetails{
			Name:           *response.Name,
			CRN:            *response.Crn,
			EnvironmentCrn: *response.EnvironmentCrn,
			Status:         response.Status,
		}
		tableRows = append(tableRows, row)
	}
	output.WriteList(listHeader, tableRows)
}

func (f *freeIpaOutDescibe) DataAsStringArray() []string {
	return []string{
		*f.FreeIpa.Crn,
		*f.FreeIpa.Name,
		f.FreeIpa.Status,
		f.FreeIpa.StatusReason,
	}
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
	if req.EnvironmentCrn == nil || len(*req.EnvironmentCrn) == 0 {
		environmentName := c.String(fl.FlEnvironmentNameOptional.Name)
		if len(environmentName) == 0 {
			commonutils.LogErrorMessageAndExit("Name of the environment must be set either in the template or with the --env-name command line option.")
		}
		crn := env.GetEnvirontmentCrnByName(c, environmentName)
		req.EnvironmentCrn = &crn
	}
	return &req
}

func SynchronizeAllUsers(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "sync users to FreeIpa")

	users := c.StringSlice(fl.FlIpaUsersSlice.Name)
	environments := c.StringSlice(fl.FlIpaEnvironmentsSlice.Name)
	SynchronizeAllUsersV1Request := freeIpaModel.SynchronizeAllUsersV1Request{
		Users:        users,
		Environments: environments,
	}

	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.SynchronizeAllUsersV1(v1freeipauser.NewSynchronizeAllUsersV1Params().WithBody(&SynchronizeAllUsersV1Request))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	synchronizeUsersResponse := resp.Payload
	log.Infof("[synchronizeAllUsers] User sync submitted with status: %s", *synchronizeUsersResponse.Status)
}

func SynchronizeCurrentUser(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "set user password in FreeIpa")

	var req freeIpaModel.SynchronizeUserV1Request

	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.SynchronizeUserV1(v1freeipauser.NewSynchronizeUserV1Params().WithBody(&req))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	synchronizeUserResponse := resp.Payload
	log.Infof("[synchronizeUser] Sync completed: %s", *synchronizeUserResponse)
}

func SetPassword(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "set user password in FreeIpa")

	password := c.String(fl.FlIpaUserPassword.Name)
	SetPasswordV1Request := freeIpaModel.SetPasswordV1Request{
		Password: password,
	}

	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.SetPasswordV1(v1freeipauser.NewSetPasswordV1Params().WithBody(&SetPasswordV1Request))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	setPasswordResponse := resp.Payload
	log.Infof("[setPassword] Set Password completed: %s", *setPasswordResponse)
}
