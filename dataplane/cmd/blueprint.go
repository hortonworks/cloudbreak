package cmd

import (
	bp "github.com/hortonworks/cb-cli/dataplane/blueprint"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "blueprint",
		Usage: "blueprint related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "adds a new Ambari blueprint from a file or from a URL",
				Subcommands: []cli.Command{
					{
						Name:   "from-url",
						Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlURL, fl.FlDlOptional).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: bp.CreateBlueprintFromUrl,
						Usage:  "creates a blueprint by downloading it from a URL location",
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
						Action: bp.CreateBlueprintFromFile,
						Usage:  "creates a blueprint by reading it from a local file",
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
				Usage:  "deletes a blueprint",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: bp.DeleteBlueprint,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a blueprint",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: bp.DescribeBlueprint,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "lists the available blueprints",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: bp.ListBlueprints,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
