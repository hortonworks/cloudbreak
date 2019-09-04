package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/rds"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "database",
		Usage: "database management related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "create a new database configuration",
				Subcommands: []cli.Command{
					{
						Name:  "mysql",
						Usage: "create mysql database configuration",
						Flags: fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRdsUserName, fl.FlRdsPassword, fl.FlRdsURL, fl.FlRdsDriverOptional, fl.FlRdsDatabaseEngineOptional,
							fl.FlRdsType, fl.FlRdsValidatedOptional, fl.FlRdsConnectorJarURLOptional).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: rds.CreateRds,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRdsUserName, fl.FlRdsPassword, fl.FlRdsURL, fl.FlRdsDriverOptional, fl.FlRdsDatabaseEngineOptional,
								fl.FlRdsType, fl.FlRdsValidatedOptional, fl.FlRdsConnectorJarURLOptional).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "oracle11",
						Usage: "create oracle 11 database configuration",
						Flags: fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRdsUserName, fl.FlRdsPassword, fl.FlRdsURL, fl.FlRdsDriverOptional, fl.FlRdsDatabaseEngineOptional, fl.FlRdsType, fl.FlRdsValidatedOptional,
							fl.FlRdsConnectorJarURLOptional).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: rds.CreateRdsOracle11,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRdsUserName, fl.FlRdsPassword, fl.FlRdsURL, fl.FlRdsDriverOptional, fl.FlRdsDatabaseEngineOptional, fl.FlRdsType,
								fl.FlRdsValidatedOptional, fl.FlRdsConnectorJarURLOptional).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "oracle12",
						Usage: "create oracle 12 database configuration",
						Flags: fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRdsUserName, fl.FlRdsPassword, fl.FlRdsURL, fl.FlRdsDriverOptional, fl.FlRdsDatabaseEngineOptional, fl.FlRdsType, fl.FlRdsValidatedOptional,
							fl.FlRdsConnectorJarURLOptional).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: rds.CreateRdsOracle12,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRdsUserName, fl.FlRdsPassword, fl.FlRdsURL, fl.FlRdsDriverOptional, fl.FlRdsDatabaseEngineOptional, fl.FlRdsType,
								fl.FlRdsValidatedOptional, fl.FlRdsConnectorJarURLOptional).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "postgres",
						Usage: "create postgres database configuration",
						Flags: fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRdsUserName, fl.FlRdsPassword, fl.FlRdsURL, fl.FlRdsDriverOptional, fl.FlRdsDatabaseEngineOptional, fl.FlRdsType,
							fl.FlRdsValidatedOptional, fl.FlRdsConnectorJarURLOptional).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: rds.CreateRds,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRdsUserName, fl.FlRdsPassword, fl.FlRdsURL, fl.FlRdsDriverOptional,
								fl.FlRdsDatabaseEngineOptional, fl.FlRdsType, fl.FlRdsValidatedOptional, fl.FlRdsConnectorJarURLOptional).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a database configuration",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: rds.DeleteRds,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a database configuration",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: rds.DescribeRds,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list the available database configurations",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: rds.ListAllRds,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
