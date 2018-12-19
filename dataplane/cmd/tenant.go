package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/tenant"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:   "tenant",
		Usage:  "tenant related operation",
		Hidden: true,
		Subcommands: []cli.Command{
			{
				Name:   "list",
				Usage:  "list all tenant",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlagsDP,
				Action: tenant.ListTenants,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "register",
				Usage:  "create a new tenant",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCaasTenantName, fl.FlCaasTenantLabel, fl.FlCaasTenantEmail).AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlagsDP,
				Action: tenant.RegisterTenant,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCaasTenantName, fl.FlCaasTenantLabel, fl.FlCaasTenantEmail).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "send-activation-email",
				Usage:  "send activation email to super user",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCaasTenantName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlagsDP,
				Action: tenant.ResendEMail,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCaasTenantName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "send-password-reset-email",
				Usage:  "send password reset email to super user",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCaasTenantName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlagsDP,
				Action: tenant.SendPasswordResetEMail,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCaasTenantName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
