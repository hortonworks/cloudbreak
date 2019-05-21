package cmd

import (
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/aws"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/azure"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/gcp"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/openstack"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/yarn"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/sdx"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "sdx",
		Usage: "create SDX clusters",
		Subcommands: []cli.Command{
			{
				Name:        "create",
				Usage:       "creates a new SDX cluster",
				Description: `basic SDX cluster creation`,
				Before:      cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Flags:       fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlEnvironmentName, fl.FlClusterShape, fl.FlCidrOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build(),
				Action:      sdx.CreateSdx,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceFlagsWithOptionalName().AddFlags(fl.FlEnvironmentName, fl.FlEnvironmentName, fl.FlClusterShape, fl.FlCidrOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes an SDX cluster",
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlWaitOptional).AddAuthenticationFlags().Build(),
				Action: sdx.DeleteSdx,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlWaitOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes an SDX cluster",
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlWaitOptional).AddAuthenticationFlags().Build(),
				Action: sdx.DescribeSdx,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlWaitOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list SDX clusters",
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName, fl.FlWaitOptional).AddAuthenticationFlags().Build(),
				Action: sdx.ListSdx,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName, fl.FlWaitOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
