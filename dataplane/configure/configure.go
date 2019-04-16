package configure

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

func Configure(c *cli.Context) {
	server := c.String(fl.FlServerOptional.Name)
	apiKeyID := c.String(fl.FlApiKeyIDOptional.Name)
	privateKey := c.String(fl.FlPrivateKeyOptional.Name)

	err := cf.WriteConfigToFile(cf.GetHomeDirectory(), server,
		c.String(fl.FlOutputOptional.Name), c.String(fl.FlProfileOptional.Name),
		c.String(fl.FlWorkspaceOptional.Name), apiKeyID, privateKey)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
}
