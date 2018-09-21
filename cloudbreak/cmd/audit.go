package cmd

import (
	"github.com/hortonworks/cb-cli/cloudbreak/audit"
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "audit",
		Usage: "audit related operations",
		Subcommands: []cli.Command{
			{
				Name:  "list",
				Usage: "list audit for a resource",
				Subcommands: []cli.Command{
					{
						Name:   "blueprint",
						Usage:  "list audit for blueprints",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlResourceID).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: audit.ListBlueprintAudits,
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
