package cmd

import (
	"github.com/hortonworks/cb-cli/dataplane/audit"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "audit",
		Usage: "audit related operations",
		Subcommands: []cli.Command{
			{
				Name:  "list",
				Usage: "list audit for a resource",
				Subcommands: []cli.Command{
					{
						Name:   "clusterdefinition",
						Usage:  "list audit for cluster definitions",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: audit.ListClusterDefinitionAudits,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "cluster",
						Usage:  "list audit for clusters",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: audit.ListClusterAudits,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "credential",
						Usage:  "list audit for credentials",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: audit.ListCredentialAudits,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "database",
						Usage:  "list audit for database configurations",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: audit.ListDatabaseAudits,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "imagecatalog",
						Usage:  "list audit for imagecatalogs",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: audit.ListImagecatalogAudits,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "ldap",
						Usage:  "list audit for ldap configurations",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: audit.ListLdapAudits,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "recipe",
						Usage:  "list audit for recipes",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: audit.ListRecipeAudits,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "describe",
				Usage:  "describe an audit entry",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlAuditID).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: audit.DescribeAudit,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlAuditID).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
