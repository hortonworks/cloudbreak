package cli

import (
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1stacks"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v2stacks"
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

func (s *stackOut) DataAsStringArray() []string {
	arr := s.cloudResourceOut.DataAsStringArray()
	arr = append(arr, s.StackStatus)
	arr = append(arr, s.ClusterStatus)
	return arr
}

type stackDetailsOut struct {
	*models_cloudbreak.StackResponse
	resp *models_cloudbreak.StackResponse
}

func (s *stackDetailsOut) DataAsStringArray() []string {
	return convertResponseToStack(s.resp).DataAsStringArray()
}

func CreateStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "create cluster")

	req := assembleStackRequest(c)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	cbClient.createStack(req, c.Bool(FlPublicOptional.Name))

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(*req.Name, AVAILABLE, AVAILABLE)
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
		req.Name = &name
		for _, ig := range req.InstanceGroups {
			if _, ok := ig.Parameters[availabilitySet]; ok {
				as := ig.Parameters[availabilitySet].(map[string]interface{})
				as["name"] = *req.Name + "-" + *ig.Group + "-as"
				ig.Parameters[availabilitySet] = as
			}
		}
	}
	if req.Name == nil || len(*req.Name) == 0 {
		utils.LogErrorMessageAndExit("Name of the cluster must be set either in the template or with the --name command line option.")
	}

	ambariPassword := c.String(FlAmbariPasswordOptional.Name)
	if len(ambariPassword) != 0 {
		if req.ClusterRequest != nil && req.ClusterRequest.AmbariRequest != nil {
			req.ClusterRequest.AmbariRequest.Password = &ambariPassword
		} else {
			utils.LogErrorMessageAndExit("Missing clusterRequest.ambariRequest node in JSON")
		}
	}

	return &req
}

func convertResponseToStack(s *models_cloudbreak.StackResponse) *stackOut {
	return &stackOut{
		cloudResourceOut{*s.Name, s.Cluster.Description, s.CloudPlatform},
		s.Status,
		s.Cluster.Status,
	}
}

func DescribeStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	resp, err := cbClient.Cloudbreak.V1stacks.GetPublicStack(v1stacks.NewGetPublicStackParams().WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	output.Write(stackHeader, &stackDetailsOut{resp.Payload, resp.Payload})
}

type getPublicStack interface {
	GetPublicStack(client *v1stacks.GetPublicStackParams) (*v1stacks.GetPublicStackOK, error)
}

func fetchStack(name string, client getPublicStack) *models_cloudbreak.StackResponse {
	resp, err := client.GetPublicStack(v1stacks.NewGetPublicStackParams().WithName(name))
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
	name := c.String(FlName.Name)
	log.Infof("[ScaleStack] scaling stack, name: %s", name)
	err = cbClient.Cloudbreak.V2stacks.PutscalingStackV2(v2stacks.NewPutscalingStackV2Params().WithName(name).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ScaleStack] stack scaled, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(name, AVAILABLE, AVAILABLE)
	}
}

func StartStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "start stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	name := c.String(FlName.Name)
	log.Infof("[StartStack] starting stack, name: %s", name)
	err := cbClient.Cloudbreak.V2stacks.PutstartStackV2(v2stacks.NewPutstartStackV2Params().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[StartStack] stack started, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(name, AVAILABLE, AVAILABLE)
	}
}

func StopStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "stop stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	name := c.String(FlName.Name)
	log.Infof("[StopStack] stopping stack, name: %s", name)
	err := cbClient.Cloudbreak.V2stacks.PutstopStackV2(v2stacks.NewPutstopStackV2Params().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[StopStack] stack stopted, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(name, STOPPED, STOPPED)
	}
}

func SyncStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "sync stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	name := c.String(FlName.Name)
	log.Infof("[SyncStack] syncing stack, name: %s", name)
	err := cbClient.Cloudbreak.V2stacks.PutsyncStackV2(v2stacks.NewPutsyncStackV2Params().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[SyncStack] stack synced, name: %s", name)
}

func RepairStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "repair stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	name := c.String(FlName.Name)
	log.Infof("[RepairStack] repairing stack, name: %s", name)
	err := cbClient.Cloudbreak.V2stacks.PutrepairStackV2(v2stacks.NewPutrepairStackV2Params().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[RepairStack] stack repaired, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(name, AVAILABLE, AVAILABLE)
	}
}

func ReinstallStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "reinstall stack")

	req := assembleReinstallRequest(c)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	name := c.String(FlName.Name)
	log.Infof("[RepairStack] reinstalling stack, name: %s", name)
	err := cbClient.Cloudbreak.V2stacks.PutreinstallStackV2(v2stacks.NewPutreinstallStackV2Params().WithName(name).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ReinstallStack] stack reinstalled, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(name, AVAILABLE, AVAILABLE)
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
	if len(*req.BlueprintName) == 0 {
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
	name := c.String(FlName.Name)
	stack := cbClient.getStackByName(name)
	cbClient.deleteStack(name, c.Bool(FlForceOptional.Name), *stack.Public)
	log.Infof("[DeleteStack] stack deleted, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(name, DELETE_COMPLETED, SKIP)
	}
}

func ListStacks(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list stacks")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	listStacksImpl(cbClient.Cloudbreak.V1stacks, output.WriteList)
}

type getPublicsStackClient interface {
	GetPublicsStack(*v1stacks.GetPublicsStackParams) (*v1stacks.GetPublicsStackOK, error)
}

func listStacksImpl(client getPublicsStackClient, writer func([]string, []utils.Row)) {
	log.Infof("[listStacksImpl] sending stack list request")
	stacksResp, err := client.GetPublicsStack(v1stacks.NewGetPublicsStackParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, stack := range stacksResp.Payload {
		tableRows = append(tableRows, convertResponseToStack(stack))
	}

	writer(stackHeader, tableRows)
}
