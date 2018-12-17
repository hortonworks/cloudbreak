package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/strategy"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "login-strategy",
		Usage: "startegy related operations for login",
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
			{
				Name:   "list",
				Usage:  "list all strategies",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlagsDP,
				Action: strategy.ListLoginStrategies,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:  "create",
				Usage: "create a new startegy for connected tenant",
				Subcommands: []cli.Command{
					{
						Name:   "saml",
						Usage:  "create a new strategy of type SAML for the tenant",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCaasStrateyProvider).AddAuthenticationFlags().AddOutputFlag().Build(),
						Before: cf.CheckConfigAndCommandFlagsDP,
						Action: strategy.CreateStrategySAML,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCaasStrateyProvider).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:  "update",
				Usage: "update strategy for the tenant",
				Subcommands: []cli.Command{
					{
						Name:   "saml",
						Usage:  "update value for the saml for the tenant",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCaasStrateyProvider).AddAuthenticationFlags().AddOutputFlag().Build(),
						Before: cf.CheckConfigAndCommandFlagsDP,
						Action: strategy.UpdateStrategySAML,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCaasStrateyProvider).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
		},
	})
}
