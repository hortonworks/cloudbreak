package cmd

import (
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/aws"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/azure"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/gcp"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/openstack"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/yarn"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/freeipa"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "freeipa",
		Usage: "create FreeIpa clusters",
		Subcommands: []cli.Command{
			{
				Name:        "create",
				Usage:       "creates a new FreeIpa cluster",
				Description: `basic FreeIpa cluster creation`,
				Before:      cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Flags:       fl.NewFlagBuilder().AddFlags(fl.FlNameOptional, fl.FlInputJson).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action:      freeipa.CreateFreeIpa,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlNameOptional, fl.FlInputJson).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a FreeIpa cluster",
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: freeipa.DeleteFreeIpa,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a FreeIpa cluster",
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: freeipa.DescribeFreeIpa,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list FreeIpa clusters",
				Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: freeipa.ListFreeIpa,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:  "user",
				Usage: "manage users on FreeIpa cluster",
				Subcommands: []cli.Command{
					{
						Name:        "syncall",
						Usage:       "syncs all users to FreeIpa clusters",
						Description: `syncs all users to FreeIpa clusters`,
						Before:      cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:       fl.NewFlagBuilder().AddFlags(fl.FlIpaUsersSlice, fl.FlIpaEnvironmentsSlice, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action:      freeipa.SynchronizeAllUsers,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlIpaUsersSlice, fl.FlIpaEnvironmentsSlice, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:        "sync",
						Usage:       "syncs current user to FreeIpa clusters",
						Description: `syncs current user to FreeIpa clusters`,
						Before:      cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:       fl.NewFlagBuilder().AddFlags(fl.FlWaitOptional).AddOutputFlag().AddAuthenticationFlags().AddOutputFlag().Build(),
						Action:      freeipa.SynchronizeCurrentUser,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlWaitOptional).AddOutputFlag().AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:        "passwd",
						Usage:       "sets password in FreeIpa clusters",
						Description: `sets password in FreeIpa clusters`,
						Before:      cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:       fl.NewFlagBuilder().AddFlags(fl.FlIpaUserPassword, fl.FlIpaEnvironmentsSlice, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action:      freeipa.SetPassword,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlIpaUserPassword, fl.FlIpaEnvironmentsSlice, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:        "status",
						Usage:       "gets operation status",
						Description: `gets operation status`,
						Before:      cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:       fl.NewFlagBuilder().AddFlags(fl.FlIpaSyncOperationId, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action:      freeipa.GetSyncOperationStatus,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlIpaSyncOperationId, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
		},
	})
}
