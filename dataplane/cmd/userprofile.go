package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/userprofile"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "userprofile",
		Usage: "user profile related operations",
		Subcommands: []cli.Command{
			{
				Name:  "show-terminated-clusters",
				Usage: "operations related to showing terminated clusters",
				Subcommands: []cli.Command{
					{
						Name:   "describe",
						Usage:  "describe settings for showing terminated clusters",
						Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: userprofile.GetShowTerminatedClustersPreferences,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "on",
						Usage:  "turn on showing terminated clusters and set user timeout",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlTimeoutMinutes, fl.FlTimeoutHours, fl.FlTimeoutDays).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: userprofile.ActivateShowTerminatedClusters,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlTimeoutMinutes, fl.FlTimeoutHours, fl.FlTimeoutDays).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "off",
						Usage:  "turn off showing terminated clusters for user",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: userprofile.DectivateShowTerminatedClusters,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "delete",
						Usage:  "delete user preference for showing terminated clusters",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: userprofile.DeleteShowTerminatedClustersPreferences,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
		},
	})
}
