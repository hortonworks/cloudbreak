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
						Name:   "custom",
						Usage:  "creates a new custom Kerberos",
						Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddCommonKerberosCreateFlags().AddFlags(fl.FlKerberosDescriptor, fl.FlKerberosKrb5Conf).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: kerberos.CreateCustomKerberos,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddCommonKerberosCreateFlags().AddFlags(fl.FlKerberosDescriptor, fl.FlKerberosKrb5Conf).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "attach",
				Usage:  "attach a Kerberos to environments",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kerberos.AttachKerberos,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "detach",
				Usage:  "detach a Kerberos from environments",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kerberos.DetachKerberos,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a Kerberos",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kerberos.DeleteKerberos,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a Kerberos",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kerberos.GetKerberos,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list the available kerberos configs",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kerberos.ListKerberos,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
