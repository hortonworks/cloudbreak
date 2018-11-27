package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/user"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "user",
		Usage: "user related operations",
		Subcommands: []cli.Command{
			{
				Name:   "list",
				Usage:  "list users",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: user.ListUsers,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
