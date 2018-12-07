package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/strategy"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "strategy",
		Usage: "startegy related operations",
		Subcommands: []cli.Command{
			{
				Name:   "types",
				Usage:  "list all types",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlagsDP,
				Action: strategy.ListStrategyTypes,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
