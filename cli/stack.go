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
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_organization_id_stack"
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

	orgID := c.Int64(FlOrganizationOptional.Name)
	req := assembleStackRequest(c)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	utils.CheckClientVersion(cbClient.Cloudbreak.V1util, Version)
	cbClient.createStack(orgID, req)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(orgID, *req.General.Name, AVAILABLE, AVAILABLE)
	}
}

func ChangeImage(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	orgID := c.Int64(FlOrganizationOptional.Name)
	imageId := c.String(FlImageId.Name)
	imageCatalogName := c.String(FlImageCatalog.Name)
	clusterName := c.String(FlName.Name)
	log.Infof("[ChangeImage] changing image for stack stack, name: %s, imageid: %s, imageCatalog: %s", clusterName, imageId, imageCatalogName)
	req := models_cloudbreak.StackImageChangeRequest{ImageCatalogName: imageCatalogName, ImageID: &imageId}
	err := cbClient.Cloudbreak.V3OrganizationIDStack.ChangeImageV3(v3_organization_id_stack.NewChangeImageV3Params().WithOrganizationID(orgID).WithName(clusterName).WithBody(&req))
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

	orgID := c.Int64(FlOrganizationOptional.Name)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	resp, err := cbClient.Cloudbreak.V3OrganizationIDStack.GetStackInOrganization(v3_organization_id_stack.NewGetStackInOrganizationParams().WithOrganizationID(orgID).WithName(c.String(FlName.Name)))
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

type getStackInOrganization interface {
	GetStackInOrganization(client *v3_organization_id_stack.GetStackInOrganizationParams) (*v3_organization_id_stack.GetStackInOrganizationOK, error)
}

func fetchStack(orgID int64, name string, client getStackInOrganization) *models_cloudbreak.StackResponse {
	resp, err := client.GetStackInOrganization(v3_organization_id_stack.NewGetStackInOrganizationParams().WithOrganizationID(orgID).WithName(name))
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
	orgID := c.Int64(FlOrganizationOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[ScaleStack] scaling stack, name: %s", name)
	err = cbClient.Cloudbreak.V3OrganizationIDStack.PutscalingStackV3(v3_organization_id_stack.NewPutscalingStackV3Params().WithOrganizationID(orgID).WithName(name).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ScaleStack] stack scaled, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(orgID, name, AVAILABLE, AVAILABLE)
	}
}

func StartStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "start stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	orgID := c.Int64(FlOrganizationOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[StartStack] starting stack, name: %s", name)
	err := cbClient.Cloudbreak.V3OrganizationIDStack.PutstartStackV3(v3_organization_id_stack.NewPutstartStackV3Params().WithOrganizationID(orgID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[StartStack] stack started, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(orgID, name, AVAILABLE, AVAILABLE)
	}
}

func StopStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "stop stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	orgID := c.Int64(FlOrganizationOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[StopStack] stopping stack, name: %s", name)
	err := cbClient.Cloudbreak.V3OrganizationIDStack.PutstopStackV3(v3_organization_id_stack.NewPutstopStackV3Params().WithOrganizationID(orgID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[StopStack] stack stopted, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(orgID, name, STOPPED, STOPPED)
	}
}

func SyncStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "sync stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	orgID := c.Int64(FlOrganizationOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[SyncStack] syncing stack, name: %s", name)
	err := cbClient.Cloudbreak.V3OrganizationIDStack.PutsyncStackV3(v3_organization_id_stack.NewPutsyncStackV3Params().WithOrganizationID(orgID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[SyncStack] stack synced, name: %s", name)
}

func RepairStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "repair stack")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	orgID := c.Int64(FlOrganizationOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[RepairStack] repairing stack, id: %s", name)

	hostGroups := strings.Split(c.String(FlHostGroups.Name), ",")
	removeOnly := c.Bool(FlRemoveOnly.Name)

	var request models_cloudbreak.ClusterRepairRequest
	request.HostGroups = hostGroups
	request.RemoveOnly = &removeOnly

	err := cbClient.Cloudbreak.V3OrganizationIDStack.RepairClusterV3(v3_organization_id_stack.NewRepairClusterV3Params().WithOrganizationID(orgID).WithName(name).WithBody(&request))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[RepairStack] stack repaired, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(orgID, name, AVAILABLE, AVAILABLE)
	}
}

func RetryCluster(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "retry cluster creation")

	cbClient := NewCloudbreakHTTPClientFromContext(c)
	orgID := c.Int64(FlOrganizationOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[RetryCluster] retrying cluster creation, name: %s", name)
	err := cbClient.Cloudbreak.V3OrganizationIDStack.RetryStackV3(v3_organization_id_stack.NewRetryStackV3Params().WithOrganizationID(orgID).WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[RetryCluster] cluster creation retried, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(orgID, name, AVAILABLE, AVAILABLE)
	}
}

func ReinstallStack(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "reinstall stack")

	req := assembleReinstallRequest(c)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	orgID := c.Int64(FlOrganizationOptional.Name)
	name := c.String(FlName.Name)
	log.Infof("[RepairStack] reinstalling stack, name: %s", name)
	err := cbClient.Cloudbreak.V3OrganizationIDStack.PutreinstallStackV3(v3_organization_id_stack.NewPutreinstallStackV3Params().WithName(name).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ReinstallStack] stack reinstalled, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(orgID, name, AVAILABLE, AVAILABLE)
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
	orgID := c.Int64(FlOrganizationOptional.Name)
	name := c.String(FlName.Name)
	cbClient.deleteStack(orgID, name, c.Bool(FlForceOptional.Name))
	log.Infof("[DeleteStack] stack deleted, name: %s", name)

	if c.Bool(FlWaitOptional.Name) {
		cbClient.waitForOperationToFinish(orgID, name, DELETE_COMPLETED, SKIP)
	}
}

func ListStacks(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list stacks")

	orgID := c.Int64(FlOrganizationOptional.Name)
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	listStacksImpl(cbClient.Cloudbreak.V3OrganizationIDStack, output.WriteList, orgID)
}

type listStacksByOrganizationClient interface {
	ListStacksByOrganization(*v3_organization_id_stack.ListStacksByOrganizationParams) (*v3_organization_id_stack.ListStacksByOrganizationOK, error)
}

func listStacksImpl(client listStacksByOrganizationClient, writer func([]string, []utils.Row), orgID int64) {
	log.Infof("[listStacksImpl] sending stack list request")
	stacksResp, err := client.ListStacksByOrganization(v3_organization_id_stack.NewListStacksByOrganizationParams().WithOrganizationID(orgID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, stack := range stacksResp.Payload {
		tableRows = append(tableRows, convertResponseToStack(stack))
	}

	writer(stackHeader, tableRows)
}
