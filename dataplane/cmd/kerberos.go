package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/kerberos"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "kerberos",
		Usage: "kerberos related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "creates a new Kerberos",
				Subcommands: []cli.Command{
					{
						Name:   "ad",
						Usage:  "creates a new AD Kerberos",
						Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddCommonKerberosCreateFlags().AddFlags(fl.FlKerberosUrl, fl.FlKerberosAdminUrl, fl.FlKerberosRealm, fl.FlKerberosLdapUrl, fl.FlKerberosContainerDn).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: kerberos.CreateAdKerberos,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddCommonKerberosCreateFlags().AddFlags(fl.FlKerberosUrl, fl.FlKerberosAdminUrl, fl.FlKerberosRealm, fl.FlKerberosLdapUrl, fl.FlKerberosContainerDn).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "freeipa",
						Usage:  "creates a new FreeIpa Kerberos",
						Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddCommonKerberosCreateFlags().AddFlags(fl.FlKerberosUrl, fl.FlKerberosAdminUrl, fl.FlKerberosRealm).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: kerberos.CreateFreeIpaKerberos,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddCommonKerberosCreateFlags().AddFlags(fl.FlKerberosUrl, fl.FlKerberosAdminUrl, fl.FlKerberosRealm).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "mit",
						Usage:  "creates a new mit Kerberos",
						Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddCommonKerberosCreateFlags().AddFlags(fl.FlKerberosUrl, fl.FlKerberosAdminUrl, fl.FlKerberosRealm).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: kerberos.CreateMitKerberos,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddCommonKerberosCreateFlags().AddFlags(fl.FlKerberosUrl, fl.FlKerberosAdminUrl, fl.FlKerberosRealm).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a Kerberos",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kerberos.DeleteKerberos,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a Kerberos",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kerberos.GetKerberos,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
