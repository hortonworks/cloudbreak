package cmd

import (
	ct "github.com/hortonworks/cb-cli/dataplane/clustertemplate"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "clustertemplate",
		Usage: "cluster template related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "adds a new cluster template from a file or from a URL",
				Subcommands: []cli.Command{
					{
						Name:   "from-url",
						Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlURL, fl.FlDlOptional).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: ct.CreateClusterTemplateFromUrl,
						Usage:  "creates a cluster template by downloading it from a URL location",
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlURL, fl.FlDlOptional).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "from-file",
						Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlFile, fl.FlDlOptional).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: ct.CreateClusterTemplateFromFile,
						Usage:  "creates a cluster template by reading it from a local file",
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlFile, fl.FlDlOptional).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes one or more cluster templates",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlNames).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: ct.DeleteClusterTemplates,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlNames).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a cluster template",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: ct.DescribeClusterTemplate,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "lists the available cluster templates",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: ct.ListClusterTemplates,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
