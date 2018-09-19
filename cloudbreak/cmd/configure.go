package cmd

import (
	"fmt"
	"github.com/hortonworks/cb-cli/cloudbreak/common"
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	"github.com/hortonworks/cb-cli/cloudbreak/configure"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name: "configure",
		Description: fmt.Sprintf("it will save the provided server address and credential "+
			"to %s/%s/%s", cf.GetHomeDirectory(), common.Config_dir, common.Config_file),
		Usage:  "configure the server address and credentials used to communicate with this server",
		Flags:  fl.NewFlagBuilder().AddFlags(fl.FlServerRequired, fl.FlUsernameRequired, fl.FlPassword, fl.FlProfileOptional, fl.FlAuthTypeOptional, fl.FlWorkspaceOptional).AddOutputFlag().Build(),
		Action: configure.Configure,
		BashComplete: func(c *cli.Context) {
			for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlServerRequired, fl.FlUsernameRequired, fl.FlPassword, fl.FlProfileOptional, fl.FlAuthTypeOptional, fl.FlWorkspaceOptional).AddOutputFlag().Build() {
				fl.PrintFlagCompletion(f)
			}
		},
	})
}
