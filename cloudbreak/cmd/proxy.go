package cmd

import (
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/proxy"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "proxy",
		Usage: "proxy related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "creates a new proxy",
				Flags: fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlProxyHost, fl.FlProxyPort,
					fl.FlProxyProtocol, fl.FlProxyUser, fl.FlProxyPassword).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: proxy.CreateProxy,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlProxyHost, fl.FlProxyPort,
						fl.FlProxyProtocol, fl.FlProxyUser, fl.FlProxyPassword).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "attach",
				Usage:  "attach a proxy to environments",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: proxy.AttachProxyToEnvs,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "detach",
				Usage:  "detach a proxy from environments",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: proxy.DetachProxyFromEnvs,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a proxy",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: proxy.DeleteProxy,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list the available proxies",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: proxy.ListProxies,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
