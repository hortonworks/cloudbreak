package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	"github.com/hortonworks/cb-cli/dataplane/env"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "env",
		Usage: "environment related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "creates a new Environment",
				Flags: fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlEnvironmentCredential, fl.FlEnvironmentRegions,
					fl.FlLdapNamesOptional, fl.FlProxyNamesOptional, fl.FlKerberosNamesOptional, fl.FlRdsNamesOptional, fl.FlEnvironmentLocationName,
					fl.FlEnvironmentLongitudeOptional, fl.FlEnvironmentLatitudeOptional).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.CreateEnvironment,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlEnvironmentCredential, fl.FlEnvironmentRegions,
						fl.FlLdapNamesOptional, fl.FlProxyNamesOptional, fl.FlKerberosNamesOptional, fl.FlRdsNamesOptional, fl.FlEnvironmentLocationName,
						fl.FlEnvironmentLongitudeOptional, fl.FlEnvironmentLatitudeOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "from-file",
				Usage:  "creates a new Environment from JSON template",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentTemplateFile, fl.FlNameOptional).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.CreateEnvironmentFromTemplate,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentTemplateFile, fl.FlNameOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:  "generate-template",
				Usage: "creates an environment JSON template",
				Subcommands: []cli.Command{
					{
						Name:  "aws",
						Usage: "creates an aws specific environment JSON template",
						Flags: fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCredentialOptional,
							fl.FlEnvironmentLocationNameOptional, fl.FlEnvironmentRegions).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: env.GenerateAwsEnvironmentTemplate,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCredentialOptional,
								fl.FlEnvironmentLocationNameOptional, fl.FlEnvironmentRegions).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "azure",
						Usage: "creates an azure specific environment JSON template",
						Flags: fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCredentialOptional,
							fl.FlEnvironmentLocationNameOptional, fl.FlEnvironmentRegions).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: env.GenerateAzureEnvironmentTemplate,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCredentialOptional,
								fl.FlEnvironmentLocationNameOptional, fl.FlEnvironmentRegions).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:  "cumulus",
				Usage: "cumulus related operations",
				Subcommands: []cli.Command{
					{
						Name:  "register-datalake",
						Usage: "register an existing Cumulus based Data Lake",
						Flags: fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName, fl.FlLdapNameOptional, fl.FlRdsNamesOptional,
							fl.FlKerberosNameOptional, fl.FlRangerAdminPasswordOptional).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: env.RegisterCumulusDatalake,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName, fl.FlLdapNameOptional, fl.FlRdsNamesOptional,
								fl.FlKerberosNameOptional, fl.FlRangerAdminPasswordOptional).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
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
			{
				Name:   "delete",
				Usage:  "deletes an environment",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.DeleteEnvironment,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "change-cred",
				Usage:  "change the credential of an environment. also changes the credential of the clusters in the environment.",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlCredential).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.ChangeCredential,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlCredential).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:  "edit",
				Usage: "edit an environment. description, regions and location can be changed.",
				Flags: fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlEnvironmentRegions, fl.FlEnvironmentLocationNameOptional,
					fl.FlEnvironmentLongitudeOptional, fl.FlEnvironmentLatitudeOptional).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.EditEnvironment,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlEnvironmentRegions, fl.FlEnvironmentLocationNameOptional,
						fl.FlEnvironmentLongitudeOptional, fl.FlEnvironmentLatitudeOptional).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
