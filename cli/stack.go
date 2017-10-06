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

func CreateStack(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "create cluster")

	req := assembleStackRequest(c)

	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	createStackImpl(oAuth2Client.Cloudbreak.Stacks, req, c.Bool(FlPublic.Name))

	return nil
}

type createStackClient interface {
	PostPublicStack(*stacks.PostPublicStackParams) (*stacks.PostPublicStackOK, error)
	PostPrivateStack(*stacks.PostPrivateStackParams) (*stacks.PostPrivateStackOK, error)
}

func createStackImpl(client createStackClient, req *models_cloudbreak.StackRequest, public bool) *models_cloudbreak.StackResponse {
	var stack *models_cloudbreak.StackResponse
	log.Infof("[createStackImpl] sending stack create request with name: %s", *req.Name)
	if public {
		resp, err := client.PostPublicStack(stacks.NewPostPublicStackParams().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		stack = resp.Payload
	} else {
		resp, err := client.PostPrivateStack(stacks.NewPostPrivateStackParams().WithBody(req))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		stack = resp.Payload
	}

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

func DeleteStack(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete cluster")

	cbClient, asClient := NewOAuth2HTTPClients(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	name := c.String(FlName.Name)
	stack := cbClient.getStackByName(name)
	asClient.deleteCluster(name, stack.ID)
	cbClient.deleteStack(name, c.Bool(FlForce.Name), *stack.Public)

	return nil
}
