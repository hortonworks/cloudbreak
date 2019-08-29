package freeipa

import (
	"encoding/json"
	"fmt"
	"strconv"
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
	log.Infof("[deleteFreeIpa] FreeIpa cluster delete requested in environment %s", envName)
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
	defer commonutils.TimeTrack(time.Now(), "sync all users to FreeIpa")

	users := c.StringSlice(fl.FlIpaUserCrnsSlice.Name)
	machineUsers := c.StringSlice(fl.FlIpaMachineUserCrnsSlice.Name)
	environments := c.StringSlice(fl.FlIpaEnvironmentCrnsSlice.Name)
	SynchronizeAllUsersV1Request := freeIpaModel.SynchronizeAllUsersV1Request{
		Users:        users,
		MachineUsers: machineUsers,
		Environments: environments,
	}

	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.SynchronizeAllUsersV1(v1freeipauser.NewSynchronizeAllUsersV1Params().WithBody(&SynchronizeAllUsersV1Request))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	syncOperationStatus := resp.Payload

	if c.Bool(fl.FlWaitOptional.Name) && syncOperationStatus.Status == "RUNNING" {
		getSyncOperationStatus(c, *syncOperationStatus.OperationID, "synchronizeAllUsers")
	} else {
		writeSyncOperationStatus(c, syncOperationStatus)
	}
}

func SynchronizeCurrentUser(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "sync current user to FreeIpa")

	environmentCrns := c.StringSlice(fl.FlIpaEnvironmentCrnsSlice.Name)
	environmentNames := c.StringSlice(fl.FlIpaEnvironmentNamesOptionalSlice.Name)

	envCrnSet := make(map[string]bool, 0)

	for _, envName := range environmentNames {
		envCrn := env.GetEnvCrnByName(envName, c)
		envCrnSet[envCrn] = true
	}
	for _, envCrn := range environmentCrns {
		envCrnSet[envCrn] = true
	}

	crns := make([]string, 0, len(envCrnSet))
	for crn := range envCrnSet {
		crns = append(crns, crn)
	}

	SynchronizeUserV1Request := freeIpaModel.SynchronizeUserV1Request{
		Environments: crns,
	}

	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.SynchronizeUserV1(v1freeipauser.NewSynchronizeUserV1Params().WithBody(&SynchronizeUserV1Request))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	syncOperationStatus := resp.Payload

	if c.Bool(fl.FlWaitOptional.Name) && syncOperationStatus.Status == "RUNNING" {
		getSyncOperationStatus(c, *syncOperationStatus.OperationID, "synchronizeUser")
	} else {
		writeSyncOperationStatus(c, syncOperationStatus)
	}
}

func SetPassword(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "set user password in FreeIpa")

	environments := c.StringSlice(fl.FlIpaEnvironmentCrnsSlice.Name)
	password := c.String(fl.FlIpaUserPassword.Name)
	SetPasswordV1Request := freeIpaModel.SetPasswordV1Request{
		Environments: environments,
		Password:     password,
	}

	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.SetPasswordV1(v1freeipauser.NewSetPasswordV1Params().WithBody(&SetPasswordV1Request))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	syncOperationStatus := resp.Payload

	if c.Bool(fl.FlWaitOptional.Name) && syncOperationStatus.Status == "RUNNING" {
		getSyncOperationStatus(c, *syncOperationStatus.OperationID, "setPassword")
	} else {
		writeSyncOperationStatus(c, syncOperationStatus)
	}
}

func GetSyncOperationStatus(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "get status of a sync operation")

	operationId := c.String(fl.FlIpaSyncOperationId.Name)
	getSyncOperationStatus(c, operationId, "getSyncOperationStatus")
}

func getSyncOperationStatus(c *cli.Context, operationId string, command string) {
	freeIpaClient := ClientFreeIpa(*oauth.NewFreeIpaClientFromContext(c)).FreeIpa
	resp, err := freeIpaClient.V1freeipauser.GetSyncOperationStatusV1(v1freeipauser.NewGetSyncOperationStatusV1Params().WithOperationID(operationId))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	syncOperationStatus := resp.Payload
	for c.Bool(fl.FlWaitOptional.Name) && syncOperationStatus.Status == "RUNNING" {
		log.Infof("[%s] Status is RUNNING. Sleeping", command)
		time.Sleep(5 * time.Second)
		resp, err := freeIpaClient.V1freeipauser.GetSyncOperationStatusV1(v1freeipauser.NewGetSyncOperationStatusV1Params().WithOperationID(operationId))
		if err != nil {
			commonutils.LogErrorAndExit(err)
		}
		syncOperationStatus = resp.Payload
	}
	log.Infof("[%s] Operation completed: %s", command, syncOperationStatus.Status)
	writeSyncOperationStatus(c, syncOperationStatus)
}

func writeSyncOperationStatus(c *cli.Context, syncOperationStatus *freeIpaModel.SyncOperationV1Status) {
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	syncStatus := &freeIpaOutSyncOperation{
		ID:        *syncOperationStatus.OperationID,
		Status:    syncOperationStatus.Status,
		SyncType:  *syncOperationStatus.SyncOperationType,
		Success:   convertSuccessDetailsModel(syncOperationStatus.Success),
		Failure:   convertFailureDetailsModel(syncOperationStatus.Failure),
		Error:     syncOperationStatus.Error,
		StartTime: strconv.FormatInt(syncOperationStatus.StartTime, 10),
		EndTime:   strconv.FormatInt(syncOperationStatus.EndTime, 10),
	}
	output.Write(syncStatusHeader, syncStatus)
}

func convertSuccessDetailsModel(sd []*freeIpaModel.SuccessDetailsV1) []successDetail {
	var successDetails = []successDetail{}
	for _, success := range sd {
		successDetails = append(successDetails, successDetail{
			Environment: success.Environment,
		})
	}
	return successDetails
}

func convertFailureDetailsModel(fd []*freeIpaModel.FailureDetailsV1) []failureDetail {
	var failureDetails = []failureDetail{}
	for _, failure := range fd {
		failureDetails = append(failureDetails, failureDetail{
			Environment: failure.Environment,
			Details:     failure.Message,
		})
	}
	return failureDetails
}
