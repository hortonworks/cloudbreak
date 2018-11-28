package cmd

import (
	"fmt"

	"github.com/hortonworks/cb-cli/dataplane/common"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	"github.com/hortonworks/cb-cli/dataplane/configure"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:   "configure",
		Before: fl.CheckRequiredFlagsAndArguments,
		Description: fmt.Sprintf("it will save the provided server address and credential "+
			"to %s/%s/%s", cf.GetHomeDirectory(), common.Config_dir, common.Config_file),
		Usage:  "configure the server address and credentials used to communicate with this server",
		Flags:  fl.NewFlagBuilder().AddFlags(fl.FlServerRequired, fl.FlProfileOptional, fl.FlWorkspaceOptional, fl.FlRefreshTokenOptional).AddOutputFlag().Build(),
		Action: configure.Configure,
		BashComplete: func(c *cli.Context) {
			for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlServerRequired, fl.FlProfileOptional, fl.FlWorkspaceOptional, fl.FlRefreshTokenOptional).AddOutputFlag().Build() {
				fl.PrintFlagCompletion(f)
			}
		},
	})
}
