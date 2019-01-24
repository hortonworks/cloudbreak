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
						Name:   "manage",
						Usage:  "add user to the workspace with workspace manage permission",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build(),
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Action: workspace.AddManageUser,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "read",
						Usage:  "add user to the workspace with read permission",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build(),
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Action: workspace.AddReadUser,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "read-write",
						Usage:  "add user to the workspace with read and write permission",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build(),
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Action: workspace.AddReadWriteUser,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "read-write-manage",
						Usage:  "add user to the workspace with read, write and manage workspace permission",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build(),
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Action: workspace.AddReadWriteManageUser,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "create",
				Usage:  "create a new workspace",
				Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddAuthenticationFlagsWithoutWorkspace().Build(),
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Action: workspace.CreateWorkspace,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddAuthenticationFlagsWithoutWorkspace().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a workspace",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlagsWithoutWorkspace().Build(),
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Action: workspace.DeleteWorkspace,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlagsWithoutWorkspace().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list workspaces",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlagsWithoutWorkspace().Build(),
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Action: workspace.ListWorkspaces,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlagsWithoutWorkspace().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a workspace",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddFlags(fl.FlName).AddAuthenticationFlagsWithoutWorkspace().Build(),
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Action: workspace.DescribeWorkspace,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddFlags(fl.FlName).AddAuthenticationFlagsWithoutWorkspace().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "remove-user",
				Usage:  "remove user from the workspace",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build(),
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Action: workspace.RemoveUser,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlUserID).AddAuthenticationFlagsWithoutWorkspace().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
