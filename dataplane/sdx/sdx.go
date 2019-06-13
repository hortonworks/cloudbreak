package sdx

import (
	"encoding/json"
	"fmt"
	"github.com/hortonworks/cb-cli/dataplane/api-sdx/client/internalsdx"
	"github.com/hortonworks/cb-cli/dataplane/api-sdx/client/sdx"
	sdxModel "github.com/hortonworks/cb-cli/dataplane/api-sdx/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	commonutils "github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
	"os"
	"time"
)

var sdxClusterHeader = []string{"Name"}

type sdxClusterOutput struct {
	Crn            string `json:"Crn" yaml:"Crn"`
	Name           string `json:"Name" yaml:"Name"`
	Environment    string `json:"environmentName" yaml:"environmentName"`
	EnvironmentCrn string `json:"environmentCrn" yaml:"environmentCrn"`
	Status         string `json:"Status" yaml:"Status"`
}

func (r *sdxClusterOutput) DataAsStringArray() []string {
	return []string{r.Crn, r.Name, r.Environment, r.EnvironmentCrn, r.Status}
}

type ClientSdx oauth.Sdx

type clientSdx interface {
	ListSdx(params *sdx.ListSdxParams) (*sdx.ListSdxOK, error)
}

func assembleStackRequest(c *cli.Context) *sdxModel.StackV4Request {
	path := c.String(fl.FlInputJson.Name)

	if path == "" {
		return nil
	}

	if _, err := os.Stat(path); os.IsNotExist(err) {
		commonutils.LogErrorAndExit(err)
	}

	log.Infof("[assembleStackTemplateForSdx] read cluster create json from file: %s", path)
	content := commonutils.ReadFile(path)

	var req sdxModel.StackV4Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid json format: %s. Please make sure that the json is valid (check for commas and double quotes).`, err.Error())
		commonutils.LogErrorMessageAndExit(msg)
	}

	return &req
}

func CreateSdx(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "create SDX cluster")

	name := c.String(fl.FlName.Name)
	envName := c.String(fl.FlEnvironmentName.Name)
	cidrOptional := c.String(fl.FlCidrOptional.Name)
	clusterShape := c.String(fl.FlClusterShape.Name)

	var cidr string
	if cidr = cidrOptional; cidrOptional == "" {
		cidr = "0.0.0.0/0"
	}

	inputJson := assembleStackRequest(c)

	if inputJson != nil {
		createInternalSdx(cidr, clusterShape, envName, inputJson, c, name)
	} else {
		createSdx(cidr, clusterShape, envName, c, name)
	}
}

func createSdx(cidr string, clusterShape string, envName string, c *cli.Context, name string) {
	SdxRequest := &sdxModel.SdxClusterRequest{
		AccessCidr:   &cidr,
		ClusterShape: &clusterShape,
		Environment:  &envName,
		Tags:         nil,
	}
	sdxClient := ClientSdx(*oauth.NewSDXClientFromContext(c)).Sdx
	resp, err := sdxClient.Sdx.CreateSdx(sdx.NewCreateSdxParams().WithName(name).WithBody(SdxRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	sdxCluster := resp.Payload
	log.Infof("[createSdx] SDX cluster created in environment: %s, with name: %s", envName, sdxCluster.Name)
}

func createInternalSdx(cidr string, clusterShape string, envName string, inputJson *sdxModel.StackV4Request, c *cli.Context, name string) {
	SdxInternalRequest := &sdxModel.SdxInternalClusterRequest{
		AccessCidr:     &cidr,
		ClusterShape:   &clusterShape,
		Environment:    &envName,
		Tags:           nil,
		StackV4Request: inputJson,
	}
	sdxClient := ClientSdx(*oauth.NewSDXClientFromContext(c)).Sdx
	resp, err := sdxClient.Internalsdx.CreateInternalSdx(internalsdx.NewCreateInternalSdxParams().WithName(name).WithBody(SdxInternalRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	sdxCluster := resp.Payload
	log.Infof("[createInternalSdx] SDX cluster created in environment: %s, with name: %s", envName, sdxCluster.Name)
}

func DeleteSdx(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "delete SDX cluster")
	name := c.String(fl.FlName.Name)

	sdxClient := ClientSdx(*oauth.NewSDXClientFromContext(c)).Sdx
	err := sdxClient.Sdx.DeleteSdx(sdx.NewDeleteSdxParams().WithName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteSdx] SDX cluster deleted in environment: %s", name)
}

func ListSdx(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "List sdx clusters in environment")
	envName := c.String(fl.FlEnvironmentNameOptional.Name)
	sdxClient := ClientSdx(*oauth.NewSDXClientFromContext(c))
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	writer := output.WriteList
	listSdxClusterImpl(sdxClient.Sdx.Sdx, envName, writer)
	log.Infof("[ListSdx] SDX cluster list in environment: %s", envName)
}

func listSdxClusterImpl(client clientSdx, envName string, writer func([]string, []utils.Row)) {
	resp, err := client.ListSdx(sdx.NewListSdxParams().WithEnvName(&envName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, sdxCluster := range resp.Payload {
		tableRows = append(tableRows, &sdxClusterOutput{sdxCluster.Crn, sdxCluster.Name,
			sdxCluster.EnvironmentName,
			sdxCluster.EnvironmentCrn,
			sdxCluster.Status})
	}

	writer(sdxClusterHeader, tableRows)
}

func DescribeSdx(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe sdx cluster")
	name := c.String(fl.FlName.Name)
	sdxClient := ClientSdx(*oauth.NewSDXClientFromContext(c)).Sdx.Sdx
	resp, err := sdxClient.GetSdx(sdx.NewGetSdxParams().WithName(name))

	if err != nil {
		utils.LogErrorAndExit(err)
	}

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	sdxCluster := resp.Payload
	if output.Format != "table" {
		output.Write(append(sdxClusterHeader, "ContentAsBase64", "ID"), &sdxClusterOutput{sdxCluster.Crn, sdxCluster.Name,
			sdxCluster.EnvironmentName,
			sdxCluster.EnvironmentCrn,
			sdxCluster.Status})
	} else {
		output.Write(append(sdxClusterHeader, "ID"), &sdxClusterOutput{sdxCluster.Crn, sdxCluster.Name,
			sdxCluster.EnvironmentName,
			sdxCluster.EnvironmentCrn,
			sdxCluster.Status})
	}
	log.Infof("[DescribeSdx] Describe a particular SDX cluster")
}
