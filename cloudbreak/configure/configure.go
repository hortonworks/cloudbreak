package configure

import (
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/utils"
	"github.com/urfave/cli"
)

func Configure(c *cli.Context) {
	fl.CheckRequiredFlagsAndArguments(c)

	err := cf.WriteConfigToFile(cf.GetHomeDirectory(), c.String(fl.FlServerOptional.Name),
		c.String(fl.FlUsername.Name), c.String(fl.FlPassword.Name),
		c.String(fl.FlOutputOptional.Name), c.String(fl.FlProfileOptional.Name),
		c.String(fl.FlAuthTypeOptional.Name), c.String(fl.FlWorkspaceOptional.Name))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}
