package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/user"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "user",
		Usage: "user related operations",
		Subcommands: []cli.Command{
			{
				Name:   "list",
				Usage:  "list all users",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlagsDP,
				Action: user.ListUsers,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "roles",
				Usage:  "list roles assigned to a user",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlagsDP,
				Action: user.ListRoles,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "info",
				Usage:  "provide information about a user",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Action: user.Userinfo,
				Before: cf.CheckConfigAndCommandFlagsDP,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:  "add",
				Usage: "provide information about a user",
				Subcommands: []cli.Command{
					{
						Name:   "roles",
						Usage:  "Add roles to a user",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlUserID, fl.FlRolesIDs).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlagsDP,
						Action: user.AssignRoles,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlUserID, fl.FlRolesIDs).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:  "remove",
				Usage: "provide information about a user",
				Subcommands: []cli.Command{
					{
						Name:   "roles",
						Usage:  "revoke roles from a user",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlUserID, fl.FlRolesIDs).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlagsDP,
						Action: user.RevokeRoles,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlUserID, fl.FlRolesIDs).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
		},
	})
}
