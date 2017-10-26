package cli

import (
	"encoding/json"
	"errors"
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
	defer utils.TimeTrack(time.Now(), "create cluster")

	req := assembleStackRequest(c)
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	createStackImpl(cbClient.Cloudbreak.V2stacks, req, c.Bool(FlPublic.Name))
}

type createStackClient interface {
	PostPublicStackV2(*v2stacks.PostPublicStackV2Params) (*v2stacks.PostPublicStackV2OK, error)
	PostPrivateStackV2(*v2stacks.PostPrivateStackV2Params) (*v2stacks.PostPrivateStackV2OK, error)
}

func createStackImpl(client createStackClient, req *models_cloudbreak.StackV2Request, public bool) *models_cloudbreak.StackResponse {
	var stack *models_cloudbreak.StackResponse
	if public {
		log.Infof("[createStackImpl] sending create public stack request")
		resp, err := client.PostPublicStackV2(v2stacks.NewPostPublicStackV2Params().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		stack = resp.Payload
	} else {
		log.Infof("[createStackImpl] sending create private stack request")
		resp, err := client.PostPrivateStackV2(v2stacks.NewPostPrivateStackV2Params().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		stack = resp.Payload
	}
	log.Infof("[createStackImpl] stack created: %s (id: %d)", *stack.Name, stack.ID)
	return stack
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
		utils.LogErrorAndExit(errors.New(msg))
	}

	name := c.String(FlName.Name)
	req.Name = &name

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
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "describe stack")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	resp, err := cbClient.Cloudbreak.V1stacks.GetPublicStack(v1stacks.NewGetPublicStackParams().WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	output.Write(stackHeader, &stackDetailsOut{resp.Payload, resp.Payload})
}

func ScaleStack(c *cli.Context) {
	checkRequiredFlags(c)
	desiredCount, err := strconv.Atoi(c.String(FlDesiredNodeCount.Name))
	if err != nil {
		utils.LogErrorMessageAndExit("Unable to parse as number: " + c.String(FlDesiredNodeCount.Name))
	}
	defer utils.TimeTrack(time.Now(), "scale stack")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	name := c.String(FlGroupName.Name)
	req := &models_cloudbreak.StackScaleRequestV2{
		DesiredCount: &(&types.I32{I: int32(desiredCount)}).I,
		Group:        &name,
	}
	log.Infof("[ScaleStack] scaling stack, name: %s", name)
	err = cbClient.Cloudbreak.V2stacks.PutscalingStackV2(v2stacks.NewPutscalingStackV2Params().WithName(c.String(FlName.Name)).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[ScaleStack] stack scaled, name: %s", name)
}

func DeleteStack(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "delete stack")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	name := c.String(FlName.Name)
	stack := cbClient.getStackByName(name)
	cbClient.deleteStack(name, c.Bool(FlForce.Name), *stack.Public)
	log.Infof("[DeleteStack] stack deleted, name: %s", name)
}

func ListStacks(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "list stacks")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
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
