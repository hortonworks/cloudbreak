package cli

import (
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_workspace_id_stacks"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

const availabilitySet = "availabilitySet"

var stackHeader []string = []string{"Name", "Description", "CloudPlatform", "StackStatus", "ClusterStatus"}

type stackOut struct {
	cloudResourceOut
	StackStatus   string `json:"StackStatus" yaml:"StackStatus"`
	ClusterStatus string `json:"ClusterStatus" yaml:"ClusterStatus"`
}

type stackOutDescribe struct {
	*stackOut
	Response *models_cloudbreak.StackResponse `json:"Response" yaml:"Response"`
	ID       string                           `json:"ID" yaml:"ID"`
}

func (s *stackOut) DataAsStringArray() []string {
	arr := s.cloudResourceOut.DataAsStringArray()
	arr = append(arr, s.StackStatus)
	arr = append(arr, s.ClusterStatus)
	return arr
}

func (s *stackOutDescribe) DataAsStringArray() []string {
	return append(s.stackOut.DataAsStringArray(), s.ID)
}

func CreateStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "create cluster")

	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	req := assembleStackRequest(c)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	utils.CheckClientVersion(cbClient.Cloudbreak.V1util, Version)
	cbClient.createStack(workspaceID, req)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(workspaceID, *req.General.Name, AVAILABLE, AVAILABLE)
	}
}

func ChangeImage(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	imageId := c.String(FlImageId.Name)
	imageCatalogName := c.String(FlImageCatalog.Name)
	clusterName := c.String(FlName.Name)
	log.Infof("[ChangeImage] changing image for stack stack, name: %s, imageid: %s, imageCatalog: %s", clusterName, imageId, imageCatalogName)
	req := models_cloudbreak.StackImageChangeRequest{ImageCatalogName: imageCatalogName, ImageID: &imageId}
	err := cbClient.Cloudbreak.V3WorkspaceIDStacks.ChangeImageV3(v3_workspace_id_stacks.NewChangeImageV3Params().WithWorkspaceID(workspaceID).WithName(clusterName).WithBody(&req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}

func assembleStackRequest(c *cli.Context) *models_cloudbreak.StackV2Request {
	path := c.String(FlInputJson.Name)
	if _, err := os.Stat(path); os.IsNotExist(err) {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[assembleStackTemplate] read cluster create json from file: %s", path)
	content := utils.ReadFile(path)

	var req models_cloudbreak.StackV2Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		utils.LogErrorMessageAndExit(msg)
	}

	name := c.String(FlName.Name)
	if len(name) != 0 {
		if req.General == nil {
			req.General = &models_cloudbreak.GeneralSettings{}
		}
		req.General.Name = &name
		if req.InstanceGroups != nil {
			for _, ig := range req.InstanceGroups {
				if _, ok := ig.Parameters[availabilitySet]; ok {
					as := ig.Parameters[availabilitySet].(map[string]interface{})
					as["name"] = *req.General.Name + "-" + *ig.Group + "-as"
					ig.Parameters[availabilitySet] = as
				}
			}
		}
	}
	if req.General == nil || req.General.Name == nil || len(*req.General.Name) == 0 {
		utils.LogErrorMessageAndExit("Name of the cluster must be set either in the template or with the --name command line option.")
	}

	ambariPassword := c.String(FlAmbariPasswordOptional.Name)
	if len(ambariPassword) != 0 {
		if req.Cluster != nil && req.Cluster.Ambari != nil {
			req.Cluster.Ambari.Password = &ambariPassword
		} else {
			utils.LogErrorMessageAndExit("Missing clusterRequest.ambariRequest node in JSON")
		}
	}

	return &req
}

func convertResponseToStack(s *models_cloudbreak.StackResponse) *stackOut {
	return &stackOut{
		cloudResourceOut{*s.Name, s.Cluster.Description, GetPlatformName(s.Credential)},
		s.Status,
		s.Cluster.Status,
	}
}

func DescribeStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe stack")

	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	resp, err := cbClient.Cloudbreak.V3WorkspaceIDStacks.GetStackInWorkspace(v3_workspace_id_stacks.NewGetStackInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	s := resp.Payload
	output.Write(append(stackHeader, "ID"), &stackOutDescribe{
		&stackOut{cloudResourceOut{*s.Name, s.Cluster.Description, GetPlatformName(s.Credential)},
			s.Status,
			s.Cluster.Status},
		s,
		strconv.FormatInt(s.ID, 10)})
}

type getStackInWorkspace interface {
	GetStackInWorkspace(client *v3_workspace_id_stacks.GetStackInWorkspaceParams) (*v3_workspace_id_stacks.GetStackInWorkspaceOK, error)
}

func fetchStack(workspaceID int64, name string, client getStackInWorkspace) *models_cloudbreak.StackResponse {
	resp, err := client.GetStackInWorkspace(v3_workspace_id_stacks.NewGetStackInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func ScaleStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	desiredCount, err := strconv.Atoi(c.String(FlDesiredNodeCount.Name))
	if err != nil {
		utils.LogErrorMessageAndExit("Unable to parse as number: " + c.String(FlDesiredNodeCount.Name))
	}
	defer utils.TimeTrack(time.Now(), "scale stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	req := &models_cloudbreak.StackScaleRequestV2{
		DesiredCount: &(&types.I32{I: int32(desiredCount)}).I,
		Group:        &(&types.S{S: c.String(FlGroupName.Name)}).S,
	}
	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[ScaleStack] scaling stack, name: %s", name)
	err = cbClient.Cloudbreak.V3WorkspaceIDStacks.PutscalingStackV3(v3_workspace_id_stacks.NewPutscalingStackV3Params().WithWorkspaceID(workspaceID).WithName(name).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ScaleStack] stack scaled, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(workspaceID, name, AVAILABLE, AVAILABLE)
	}
}

func StartStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "start stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[StartStack] starting stack, name: %s", name)
	err := cbClient.Cloudbreak.V3WorkspaceIDStacks.PutstartStackV3(v3_workspace_id_stacks.NewPutstartStackV3Params().WithWorkspaceID(workspaceID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[StartStack] stack started, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(workspaceID, name, AVAILABLE, AVAILABLE)
	}
}

func StopStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "stop stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[StopStack] stopping stack, name: %s", name)
	err := cbClient.Cloudbreak.V3WorkspaceIDStacks.PutstopStackV3(v3_workspace_id_stacks.NewPutstopStackV3Params().WithWorkspaceID(workspaceID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[StopStack] stack stopted, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(workspaceID, name, STOPPED, STOPPED)
	}
}

func SyncStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "sync stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[SyncStack] syncing stack, name: %s", name)
	err := cbClient.Cloudbreak.V3WorkspaceIDStacks.PutsyncStackV3(v3_workspace_id_stacks.NewPutsyncStackV3Params().WithWorkspaceID(workspaceID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[SyncStack] stack synced, name: %s", name)
}

func RepairStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "repair stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[RepairStack] repairing stack, id: %s", name)

	hostGroups := strings.Split(c.String(FlHostGroups.Name), ",")
	removeOnly := c.Bool(FlRemoveOnly.Name)

	var request models_cloudbreak.ClusterRepairRequest
	request.HostGroups = hostGroups
	request.RemoveOnly = &removeOnly

	err := cbClient.Cloudbreak.V3WorkspaceIDStacks.RepairClusterV3(v3_workspace_id_stacks.NewRepairClusterV3Params().WithWorkspaceID(workspaceID).WithName(name).WithBody(&request))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[RepairStack] stack repaired, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(workspaceID, name, AVAILABLE, AVAILABLE)
	}
}

func RetryCluster(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "retry cluster creation")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[RetryCluster] retrying cluster creation, name: %s", name)
	err := cbClient.Cloudbreak.V3WorkspaceIDStacks.RetryStackV3(v3_workspace_id_stacks.NewRetryStackV3Params().WithWorkspaceID(workspaceID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[RetryCluster] cluster creation retried, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(workspaceID, name, AVAILABLE, AVAILABLE)
	}
}

func ReinstallStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "reinstall stack")

	req := assembleReinstallRequest(c)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[RepairStack] reinstalling stack, name: %s", name)
	err := cbClient.Cloudbreak.V3WorkspaceIDStacks.PutreinstallStackV3(v3_workspace_id_stacks.NewPutreinstallStackV3Params().WithWorkspaceID(workspaceID).WithName(name).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ReinstallStack] stack reinstalled, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(workspaceID, name, AVAILABLE, AVAILABLE)
	}
}

func assembleReinstallRequest(c *cli.Context) *models_cloudbreak.ReinstallRequestV2 {
	path := c.String(FlInputJson.Name)
	if _, err := os.Stat(path); os.IsNotExist(err) {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[assembleReinstallRequest] read cluster reinstall json from file: %s", path)
	content := utils.ReadFile(path)

	var req models_cloudbreak.ReinstallRequestV2
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		utils.LogErrorMessageAndExit(msg)
	}

	bpName := c.String(FlBlueprintNameOptional.Name)
	if len(bpName) != 0 {
		req.BlueprintName = &bpName
	}
	if req.BlueprintName == nil || len(*req.BlueprintName) == 0 {
		utils.LogErrorMessageAndExit("Name of the blueprint must be set either in the template or with the --blueprint-name command line option.")
	}

	req.KerberosPassword = c.String(FlKerberosPasswordOptional.Name)
	req.KerberosPrincipal = c.String(FlKerberosPrincipalOptional.Name)

	return &req
}

func DeleteStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	name := c.String(FlName.Name)
	cbClient.deleteStack(workspaceID, name, c.Bool(FlForceOptional.Name))
	log.Infof("[DeleteStack] stack deleted, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(workspaceID, name, DELETE_COMPLETED, SKIP)
	}
}

func ListStacks(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list stacks")

	workspaceID := c.Int64(FlWorkspaceOptional.Name)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	listStacksImpl(cbClient.Cloudbreak.V3WorkspaceIDStacks, output.WriteList, workspaceID)
}

type listStacksByWorkspaceClient interface {
	ListStacksByWorkspace(*v3_workspace_id_stacks.ListStacksByWorkspaceParams) (*v3_workspace_id_stacks.ListStacksByWorkspaceOK, error)
}

func listStacksImpl(client listStacksByWorkspaceClient, writer func([]string, []utils.Row), workspaceID int64) {
	log.Infof("[listStacksImpl] sending stack list request")
	stacksResp, err := client.ListStacksByWorkspace(v3_workspace_id_stacks.NewListStacksByWorkspaceParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, stack := range stacksResp.Payload {
		tableRows = append(tableRows, convertResponseToStack(stack))
	}

	writer(stackHeader, tableRows)
}
