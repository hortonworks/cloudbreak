package cmd

import (
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	"github.com/hortonworks/cb-cli/cloudbreak/env"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "env",
		Usage: "environment related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "creates a new Environment",
				Flags: fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlEnvironmentCredential, fl.FlEnvironmentRegions,
					fl.FlEnvironmentLdaps, fl.FlEnvironmentProxies, fl.FlEnvironmentRdses).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.CreateEnvironment,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlEnvironmentCredential, fl.FlEnvironmentRegions,
						fl.FlEnvironmentLdaps, fl.FlEnvironmentProxies, fl.FlEnvironmentRdses).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list the available environments",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.ListEnvironments,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes an environment",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.DescribeEnvironment,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
