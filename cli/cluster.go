package cli

import (
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v2stacks"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

func ChangeAmbariPassword(c *cli.Context) {
	checkRequiredFlags(c)
	defer utils.TimeTrack(time.Now(), "update ambari password")

	cbClient := NewCloudbreakOAuth2HTTPClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	name := c.String(FlName.Name)
	log.Infof("[ChangeAmbariPassword] updating ambari password, name: %s", name)
	req := &models_cloudbreak.UserNamePassword{
		OldPassword: &(&types.S{S: c.String(FlOldPassword.Name)}).S,
		Password:    &(&types.S{S: c.String(FlNewPassword.Name)}).S,
		UserName:    &(&types.S{S: c.String(FlAmbariUser.Name)}).S,
	}
	err := cbClient.Cloudbreak.V2stacks.PutpasswordStackV2(v2stacks.NewPutpasswordStackV2Params().WithName(name).WithBody(req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[RepairStack] ambari password updated, name: %s", name)
}
