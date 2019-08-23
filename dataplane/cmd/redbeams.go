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
						Name:        "describe",
						Usage:       "describe a database server",
						Description: "To specify a database server, either provide its CRN, or both its environment CRN and name.",
						Before: func(c *cli.Context) error {
							err := cf.CheckConfigAndCommandFlagsWithoutWorkspace(c)
							if err != nil {
								return err
							}
							return cf.CheckResourceAddressingFlags(c)
						},
						Flags:  fl.NewFlagBuilder().AddResourceAddressingFlags().AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.GetDatabaseServer,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceAddressingFlags().AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "create",
						Usage:  "create a managed database server",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlDatabaseServerCreationFile).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.CreateManagedDatabaseServer,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlDatabaseServerCreationFile).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "terminate",
						Usage:  "terminate a managed database server",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCrn, fl.FlForceOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.TerminateManagedDatabaseServer,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCrn, fl.FlForceOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "register",
						Usage:  "register a database server",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlDatabaseServerRegistrationFile).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.RegisterDatabaseServer,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlDatabaseServerRegistrationFile).AddAuthenticationFlags().AddOutputFlag().Build() {
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
			{
				Name:  "db",
				Usage: "manage redbeams databases",
				Subcommands: []cli.Command{
					{
						Name:   "list",
						Usage:  "list all databases",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.ListDatabases,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:        "describe",
						Usage:       "describe a database",
						Description: "To specify a database, either provide its CRN, or both its environment CRN and name.",
						Before: func(c *cli.Context) error {
							err := cf.CheckConfigAndCommandFlagsWithoutWorkspace(c)
							if err != nil {
								return err
							}
							return cf.CheckResourceAddressingFlags(c)
						},
						Flags:  fl.NewFlagBuilder().AddResourceAddressingFlags().AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.GetDatabase,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceAddressingFlags().AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "create",
						Usage:  "create a database",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlDatabaseCreationFile).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.CreateDatabase,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlDatabaseCreationFile).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "register",
						Usage:  "register a database",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlDatabaseRegistrationFile).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.RegisterDatabase,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlDatabaseRegistrationFile).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:        "delete",
						Usage:       "delete a database",
						Description: "To specify a database, either provide its CRN, or both its environment CRN and name.",
						Before: func(c *cli.Context) error {
							err := cf.CheckConfigAndCommandFlagsWithoutWorkspace(c)
							if err != nil {
								return err
							}
							return cf.CheckResourceAddressingFlags(c)
						},
						Flags:  fl.NewFlagBuilder().AddResourceAddressingFlags().AddAuthenticationFlags().Build(),
						Action: redbeams.DeleteDatabase,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceAddressingFlags().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
		},
	})
}
