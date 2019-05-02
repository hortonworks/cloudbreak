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

var stackHeader = []string{"Name", "CloudPlatform", "Environment", "StackStatus", "ClusterStatus"}

type ClientSdx oauth.Sdx

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
	log.Infof("[deleteSdx] Not supported")
}

func ListSdx(c *cli.Context) {
	log.Infof("[ListSdx] Not supported")
}

func DescribeSdx(c *cli.Context) {
	log.Infof("[DescribeSdx] Not supported")
}
