package cmd

import (
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/ldap"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "ldap",
		Usage: "ldap related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "creates a new LDAP",
				Flags: fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlLdapServer, fl.FlLdapDomain,
					fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapDirectoryType, fl.FlLdapUserSearchBase, fl.FlLdapUserDnPattern,
					fl.FlLdapUserNameAttribute, fl.FlLdapUserObjectClass, fl.FlLdapGroupMemberAttribute, fl.FlEnvironmentsOptional,
					fl.FlLdapGroupNameAttribute, fl.FlLdapGroupObjectClass, fl.FlLdapGroupSearchBase, fl.FlLdapAdminGroup).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: ldap.CreateLDAP,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlLdapServer, fl.FlLdapDomain,
						fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapDirectoryType, fl.FlLdapUserSearchBase, fl.FlLdapUserDnPattern,
						fl.FlLdapUserNameAttribute, fl.FlLdapUserObjectClass, fl.FlLdapGroupMemberAttribute, fl.FlEnvironmentsOptional,
						fl.FlLdapGroupNameAttribute, fl.FlLdapGroupObjectClass, fl.FlLdapGroupSearchBase, fl.FlLdapAdminGroup).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "attach",
				Usage:  "attach an LDAP to environments",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: ldap.AttachLdapToEnvs,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "detach",
				Usage:  "detach an LDAP from environments",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: ldap.DetachLdapFromEnvs,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlEnvironments).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes an LDAP",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: ldap.DeleteLdap,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes an LDAP",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: ldap.DescribeLdap,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list the available ldaps",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: ldap.ListLdaps,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "user",
				Usage:  "manage LDAP users",
				Hidden: true,
				Subcommands: []cli.Command{
					{
						Name:  "create",
						Usage: "create a new LDAP user in the given base",
						Flags: fl.NewFlagBuilder().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
							fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapUserToCreate, fl.FlLdapUserToCreateEmail, fl.FlLdapUserToCreatePassword,
							fl.FlLdapUserToCreateBase, fl.FlLdapUserToCreateGroups, fl.FlLdapDirectoryType).Build(),
						Before: fl.CheckRequiredFlagsAndArguments,
						Action: ldap.CreateLdapUser,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
								fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapUserToCreate, fl.FlLdapUserToCreateEmail, fl.FlLdapUserToCreatePassword,
								fl.FlLdapUserToCreateBase, fl.FlLdapUserToCreateGroups, fl.FlLdapDirectoryType).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "list",
						Usage: "list the LDAP users in the given base",
						Flags: fl.NewFlagBuilder().AddOutputFlag().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
							fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapUserSearchBase, fl.FlLdapDirectoryType).Build(),
						Before: fl.CheckRequiredFlagsAndArguments,
						Action: ldap.ListLdapUsers,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
								fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapUserSearchBase, fl.FlLdapDirectoryType).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "delete",
						Usage: "delete a user from LDAP",
						Flags: fl.NewFlagBuilder().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
							fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapUserToDelete,
							fl.FlLdapUserToDeleteBase, fl.FlLdapDirectoryType).Build(),
						Before: fl.CheckRequiredFlagsAndArguments,
						Action: ldap.DeleteLdapUser,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
								fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapUserToDelete,
								fl.FlLdapUserToDeleteBase, fl.FlLdapDirectoryType).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "group",
				Usage:  "manage LDAP groups",
				Hidden: true,
				Subcommands: []cli.Command{
					{
						Name:  "create",
						Usage: "create a new LDAP group in the given base",
						Flags: fl.NewFlagBuilder().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
							fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapGroupToCreate, fl.FlLdapGroupToCreateBase, fl.FlLdapDirectoryType).Build(),
						Before: fl.CheckRequiredFlagsAndArguments,
						Action: ldap.CreateLdapGroup,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
								fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapGroupToCreate, fl.FlLdapGroupToCreateBase, fl.FlLdapDirectoryType).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "list",
						Usage: "list the LDAP groups in the given base",
						Flags: fl.NewFlagBuilder().AddOutputFlag().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
							fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapGroupSearchBase, fl.FlLdapDirectoryType).Build(),
						Before: fl.CheckRequiredFlagsAndArguments,
						Action: ldap.ListLdapGroups,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
								fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapGroupSearchBase, fl.FlLdapDirectoryType).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "delete",
						Usage: "delete a group from LDAP",
						Flags: fl.NewFlagBuilder().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
							fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapGroupToDelete, fl.FlLdapGroupToDeleteBase, fl.FlLdapDirectoryType).Build(),
						Before: fl.CheckRequiredFlagsAndArguments,
						Action: ldap.DeleteLdapGroup,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlLdapServer, fl.FlLdapSecureOptional,
								fl.FlLdapBindDN, fl.FlLdapBindPassword, fl.FlLdapGroupToDelete, fl.FlLdapGroupToDeleteBase, fl.FlLdapDirectoryType).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
		},
	})
}
