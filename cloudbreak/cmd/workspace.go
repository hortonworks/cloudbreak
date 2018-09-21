package cmd

import (
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/workspace"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "workspace",
		Usage: "workspace related operations",
		Subcommands: []cli.Command{
			{
				Name:  "add-user",
				Usage: "add user to the workspace",
				Subcommands: []cli.Command{
					{
						Name:   "read",
						Usage:  "add user to the workspace with read permission",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: workspace.AddReadUser,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "read-write",
						Usage:  "add user to the workspace with read and write permission",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: workspace.AddReadWriteUser,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "create",
				Usage:  "create a new workspace",
				Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: workspace.CreateWorkspace,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a workspace",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: workspace.DeleteWorkspace,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list workspaces",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: workspace.ListWorkspaces,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a workspace",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddFlags(fl.FlName).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: workspace.DescribeWorkspace,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddFlags(fl.FlName).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "remove-user",
				Usage:  "remove user from the workspace",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: workspace.RemoveUser,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
