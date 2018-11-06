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
					fl.FlEnvironmentLdapsOptional, fl.FlEnvironmentProxiesOptional, fl.FlEnvironmentRdsesOptional, fl.FlEnvironmentLocationName,
					fl.FlEnvironmentLongitudeOptional, fl.FlEnvironmentLatitudeOptional).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.CreateEnvironment,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlEnvironmentCredential, fl.FlEnvironmentRegions,
						fl.FlEnvironmentLdapsOptional, fl.FlEnvironmentProxiesOptional, fl.FlEnvironmentRdsesOptional, fl.FlEnvironmentLocationName,
						fl.FlEnvironmentLongitudeOptional, fl.FlEnvironmentLatitudeOptional).AddAuthenticationFlags().Build() {
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
				Name:  "attach",
				Usage: "attach resources to an environment (LDAP, RDS or Proxy)",
				Flags: fl.NewFlagBuilder().AddFlags(fl.FlName,
					fl.FlEnvironmentLdapsOptional, fl.FlEnvironmentProxiesOptional, fl.FlEnvironmentRdsesOptional).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.AttachResources,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironmentLdapsOptional, fl.FlEnvironmentProxiesOptional,
						fl.FlEnvironmentRdsesOptional).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:  "detach",
				Usage: "detach resources from an environment (LDAP, RDS or Proxy)",
				Flags: fl.NewFlagBuilder().AddFlags(fl.FlName,
					fl.FlEnvironmentLdapsOptional, fl.FlEnvironmentProxiesOptional, fl.FlEnvironmentRdsesOptional).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: env.DetachResources,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironmentLdapsOptional, fl.FlEnvironmentProxiesOptional,
						fl.FlEnvironmentRdsesOptional).AddOutputFlag().AddAuthenticationFlags().Build() {
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
		},
	})
}
