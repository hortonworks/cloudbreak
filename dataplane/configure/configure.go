package configure

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/caasauth"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

func Configure(c *cli.Context) {
	server := c.String(fl.FlServerOptional.Name)
	token := c.String(fl.FlRefreshTokenOptional.Name)
	if len(token) == 0 {
		token = caasauth.NewRefreshToken(server)
	}
	err := cf.WriteConfigToFile(cf.GetHomeDirectory(), server,
		c.String(fl.FlOutputOptional.Name), c.String(fl.FlProfileOptional.Name),
		c.String(fl.FlWorkspaceOptional.Name), token)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}
