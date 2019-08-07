package cmd

import (
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/aws"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/azure"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/gcp"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/openstack"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/yarn"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/redbeams"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "redbeams",
		Usage: "manage relational databases and database servers",
		Subcommands: []cli.Command{
			{
				Name:  "dbserver",
				Usage: "manage redbeams database servers",
				Subcommands: []cli.Command{
					{
						Name:   "list",
						Usage:  "list all database servers",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.ListDatabaseServers,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "describe",
						Usage:  "describe a database server, getting it by CRN",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.GetDatabaseServer,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "describe-by-name",
						Usage:  "describe a database server, getting it by name",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn, fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.GetDatabaseServerByName,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn, fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "get-status",
						Usage:  "get the status of a database server by CRN",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.GetDatabaseServerStatus,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "get-status-by-name",
						Usage:  "get the status of a database server by name",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn, fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.GetDatabaseServerStatusByName,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn, fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "terminate",
						Usage:  "terminate a managed database server",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.TerminateManagedDatabaseServer,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "delete",
						Usage:  "delete a registered database server",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().Build(),
						Action: redbeams.DeleteDatabaseServer,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
		},
	})
}
