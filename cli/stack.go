package cli

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/cli/utils"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/stacks"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
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

func CreateStack(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "create cluster")

	req := assembleStackRequest(c)
	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	createStackImpl(cbClient.Cloudbreak.Stacks, req, c.Bool(FlPublic.Name))
}

type createStackClient interface {
	PostPublicStack(*stacks.PostPublicStackParams) (*stacks.PostPublicStackOK, error)
	PostPrivateStack(*stacks.PostPrivateStackParams) (*stacks.PostPrivateStackOK, error)
}

func createStackImpl(client createStackClient, req *models_cloudbreak.StackRequest, public bool) *models_cloudbreak.StackResponse {
	var stack *models_cloudbreak.StackResponse
	if public {
		log.Infof("[createStackImpl] sending create public stack request")
		resp, err := client.PostPublicStack(stacks.NewPostPublicStackParams().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		stack = resp.Payload
	} else {
		log.Infof("[createStackImpl] sending create private stack request")
		resp, err := client.PostPrivateStack(stacks.NewPostPrivateStackParams().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		stack = resp.Payload
	}
	log.Infof("[createStackImpl] stack created: %s (id: %d)", *stack.Name, stack.ID)
	return stack
}

func assembleStackRequest(c *cli.Context) *models_cloudbreak.StackRequest {
	path := c.String(FlInputJson.Name)
	if _, err := os.Stat(path); os.IsNotExist(err) {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[assembleStackTemplate] read cluster create json from file: %s", path)
	content := utils.ReadFile(path)

	var req models_cloudbreak.StackRequest
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		utils.LogErrorAndExit(errors.New(msg))
	}

	name := c.String(FlName.Name)
	req.Name = &name
	if req.ClusterRequest != nil {
		req.ClusterRequest.Name = &name
	}

	ambariPassword := c.String(FlAmbariPasswordOptional.Name)
	if len(ambariPassword) != 0 {
		req.ClusterRequest.Password = &ambariPassword
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
	resp, err := cbClient.Cloudbreak.Stacks.GetPublicStack(stacks.NewGetPublicStackParams().WithName(c.String(FlName.Name)))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	output.Write(stackHeader, convertResponseToStack(resp.Payload))
}

func DeleteStack(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "delete stack")

	cbClient, asClient := NewOAuth2HTTPClients(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	name := c.String(FlName.Name)
	stack := cbClient.getStackByName(name)
	asClient.deleteCluster(name, stack.ID)
	log.Infof("[DeleteStack] autoscaling cluster deleted, name: %s", name)
	cbClient.deleteStack(name, c.Bool(FlForce.Name), *stack.Public)
	log.Infof("[DeleteStack] stack deleted, name: %s", name)
}

func ListStacks(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "list stacks")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := utils.Output{Format: c.String(FlOutput.Name)}
	listStacksImpl(cbClient.Cloudbreak.Stacks, output.WriteList)
}

type getPublicsStackClient interface {
	GetPublicsStack(*stacks.GetPublicsStackParams) (*stacks.GetPublicsStackOK, error)
}

func listStacksImpl(client getPublicsStackClient, writer func([]string, []utils.Row)) {
	log.Infof("[listStacksImpl] sending stack list request")
	stackResp, err := client.GetPublicsStack(stacks.NewGetPublicsStackParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, stack := range stackResp.Payload {
		tableRows = append(tableRows, convertResponseToStack(stack))
	}

	writer(stackHeader, tableRows)
}
