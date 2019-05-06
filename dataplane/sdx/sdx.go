package sdx

import (
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/dataplane/api-sdx/client/sdx"
	sdxModel "github.com/hortonworks/cb-cli/dataplane/api-sdx/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	commonutils "github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var sdxClusterHeader = []string{"Name"}

type sdxClusterOutput struct {
	SdxName string `json:"Name" yaml:"Name"`
	Status  string `json:"Status" yaml:"Status"`
}

func (r *sdxClusterOutput) DataAsStringArray() []string {
	return []string{r.SdxName}
}

type ClientSdx oauth.Sdx

type clientSdx interface {
	ListSdx(params *sdx.ListSdxParams) (*sdx.ListSdxOK, error)
}

func CreateSdx(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "create SDX cluster")

	name := c.String(fl.FlEnvironmentName.Name)
	SdxRequest := &sdxModel.SdxClusterRequest{
		AccessCidr:   "",
		ClusterShape: "",
		Tags:         nil,
	}

	sdxClient := ClientSdx(*oauth.NewSDXClientFromContext(c)).Sdx
	resp, err := sdxClient.Sdx.CreateSdx(sdx.NewCreateSdxParams().WithEnvName(name).WithBody(SdxRequest))

	if err != nil {
		utils.LogErrorAndExit(err)
	}
	sdxCluster := resp.Payload

	log.Infof("[createSdx] SDX cluster created in environment: %s, with name: %s", name, sdxCluster.SdxName)
}

func DeleteSdx(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "delete SDX cluster")
	name := c.String(fl.FlEnvironmentName.Name)

	sdxClient := ClientSdx(*oauth.NewSDXClientFromContext(c)).Sdx
	err := sdxClient.Sdx.DeleteSdx(sdx.NewDeleteSdxParams().WithEnvName(name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteSdx] SDX cluster deleted in environment: %s", name)
}

func ListSdx(c *cli.Context) {
	envName := c.String(fl.FlEnvironmentName.Name)
	sdxClient := ClientSdx(*oauth.NewSDXClientFromContext(c))
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	writer := output.WriteList
	listSdxClusterImpl(sdxClient.Sdx.Sdx, envName, writer)
	log.Infof("[ListSdx] SDX cluster list in environment: %s", envName)
}

func listSdxClusterImpl(client clientSdx, envName string, writer func([]string, []utils.Row)) {
	resp, err := client.ListSdx(sdx.NewListSdxParams().WithEnvName(envName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, sdxCluster := range resp.Payload {
		tableRows = append(tableRows, &sdxClusterOutput{sdxCluster.SdxName, sdxCluster.Status})
	}

	writer(sdxClusterHeader, tableRows)
}

func DescribeSdx(c *cli.Context) {
	log.Infof("[DescribeSdx] Not supported")
}
