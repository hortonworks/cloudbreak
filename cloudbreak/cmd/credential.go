package cmd

import (
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	"github.com/hortonworks/cb-cli/cloudbreak/credential"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "credential",
		Usage: "credential related operations",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "creates a new credential",
				Subcommands: []cli.Command{
					{
						Name:  "aws",
						Usage: "creates a new aws credential",
						Subcommands: []cli.Command{
							{
								Name:   "role-based",
								Usage:  "creates a new role based aws credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRoleARN).AddAuthenticationFlags().Build(),
								Action: credential.CreateAwsCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRoleARN).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "key-based",
								Usage:  "creates a new key based aws credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlAccessKey, fl.FlSecretKey).AddAuthenticationFlags().Build(),
								Action: credential.CreateAwsCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlAccessKey, fl.FlSecretKey).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "aws-gov",
						Usage: "creates a new aws govcloud credential",
						Subcommands: []cli.Command{
							{
								Name:   "key-based",
								Usage:  "creates a new key based aws govcloud credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlAccessKey, fl.FlSecretKey).AddAuthenticationFlags().Build(),
								Action: credential.CreateAwsGovCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlAccessKey, fl.FlSecretKey).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "role-based",
								Usage:  "creates a new role based aws govcloud credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRoleARN).AddAuthenticationFlags().Build(),
								Action: credential.CreateAwsGovCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlRoleARN).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "azure",
						Usage: "creates a new azure credential",
						Subcommands: []cli.Command{
							{
								Name:   "app-based",
								Usage:  "creates a new app based azure credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlSubscriptionId, fl.FlTenantId, fl.FlAppId, fl.FlAppPassword).AddAuthenticationFlags().Build(),
								Action: credential.CreateAzureCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlSubscriptionId, fl.FlTenantId, fl.FlAppId, fl.FlAppPassword).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "gcp",
						Usage: "creates a new gcp credential",
						Subcommands: []cli.Command{
							{
								Name:   "p12-based",
								Usage:  "creates a new P12 based gcp credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlProjectId, fl.FlServiceAccountId, fl.FlServiceAccountPrivateKeyFile).AddAuthenticationFlags().Build(),
								Action: credential.CreateGcpCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlProjectId, fl.FlServiceAccountId, fl.FlServiceAccountPrivateKeyFile).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "json-based",
								Usage:  "creates a new JSON based gcp credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlServiceAccountJsonFile).AddAuthenticationFlags().Build(),
								Action: credential.CreateGcpCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlServiceAccountJsonFile).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "openstack",
						Usage: "creates a new openstack credential",
						Subcommands: []cli.Command{
							{
								Name:   "keystone-v2",
								Usage:  "creates a new keystone version 2 openstack credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlTenantUser, fl.FlTenantPassword, fl.FlTenantName, fl.FlEndpoint, fl.FlFacingOptional).AddAuthenticationFlags().Build(),
								Action: credential.CreateOpenstackCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlTenantUser, fl.FlTenantPassword, fl.FlTenantName, fl.FlEndpoint, fl.FlFacingOptional).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "keystone-v3",
								Usage:  "creates a new keystone version 3 openstack credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags: fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlTenantUser, fl.FlTenantPassword,
									fl.FlUserDomain, fl.FlProjectDomainNameOptional, fl.FlProjectNameOptional, fl.FlDomainNameOptional, fl.FlKeystoneScopeOptional, fl.FlEndpoint, fl.FlFacingOptional).AddAuthenticationFlags().Build(),
								Action: credential.CreateOpenstackCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlTenantUser, fl.FlTenantPassword,
										fl.FlUserDomain, fl.FlProjectDomainNameOptional, fl.FlProjectNameOptional, fl.FlDomainNameOptional, fl.FlKeystoneScopeOptional, fl.FlEndpoint, fl.FlFacingOptional).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:   "from-file",
						Usage:  "creates a new credential from input json file",
						Flags:  fl.NewFlagBuilder().AddResourceFlagsWithOptionalName().AddFlags(fl.FlInputJson).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: credential.CreateCredentialFromFile,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddResourceFlagsWithOptionalName().AddFlags(fl.FlInputJson).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:  "modify",
				Usage: "modify an existing credential",
				Subcommands: []cli.Command{
					{
						Name:  "aws",
						Usage: "modify an existing aws credential",
						Subcommands: []cli.Command{
							{
								Name:   "role-based",
								Usage:  "modify a role based aws credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlRoleARN).AddAuthenticationFlags().Build(),
								Action: credential.ModifyAwsCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlRoleARN).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "key-based",
								Usage:  "modify a key based aws credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlAccessKey, fl.FlSecretKey).AddAuthenticationFlags().Build(),
								Action: credential.ModifyAwsCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlAccessKey, fl.FlSecretKey).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "aws-gov",
						Usage: "modify an existing aws govcloud credential",
						Subcommands: []cli.Command{
							{
								Name:   "role-based",
								Usage:  "modify a role based aws govcloud credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlRoleARN).AddAuthenticationFlags().Build(),
								Action: credential.ModifyAwsGovCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlRoleARN).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "key-based",
								Usage:  "modify a key based aws govcloud credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlAccessKey, fl.FlSecretKey).AddAuthenticationFlags().Build(),
								Action: credential.ModifyAwsGovCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlAccessKey, fl.FlSecretKey).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "azure",
						Usage: "modify an existing azure credential",
						Subcommands: []cli.Command{
							{
								Name:   "app-based",
								Usage:  "modify an app based azure credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlSubscriptionId, fl.FlTenantId, fl.FlAppId, fl.FlAppPassword).AddAuthenticationFlags().Build(),
								Action: credential.ModifyAzureCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlSubscriptionId, fl.FlTenantId, fl.FlAppId, fl.FlAppPassword).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "gcp",
						Usage: "modify an existing gcp credential",
						Subcommands: []cli.Command{
							{
								Name:   "p12-based",
								Usage:  "modify a P12 based gcp credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlProjectId, fl.FlServiceAccountId, fl.FlServiceAccountPrivateKeyFile).AddAuthenticationFlags().Build(),
								Action: credential.ModifyGcpCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlProjectId, fl.FlServiceAccountId, fl.FlServiceAccountPrivateKeyFile).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "json-based",
								Usage:  "modify a JSON based gcp credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlServiceAccountJsonFile).AddAuthenticationFlags().Build(),
								Action: credential.ModifyGcpCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlServiceAccountJsonFile).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "openstack",
						Usage: "modify an existing openstack credential",
						Subcommands: []cli.Command{
							{
								Name:   "keystone-v2",
								Usage:  "modify a keystone version 2 openstack credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlTenantUser, fl.FlTenantPassword, fl.FlTenantName, fl.FlEndpoint, fl.FlFacingOptional).AddAuthenticationFlags().Build(),
								Action: credential.ModifyOpenstackCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlTenantUser, fl.FlTenantPassword, fl.FlTenantName, fl.FlEndpoint, fl.FlFacingOptional).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "keystone-v3",
								Usage:  "modify a keystone version 3 openstack credential",
								Before: cf.CheckConfigAndCommandFlags,
								Flags: fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlTenantUser, fl.FlTenantPassword,
									fl.FlUserDomain, fl.FlProjectDomainNameOptional, fl.FlProjectNameOptional, fl.FlDomainNameOptional, fl.FlKeystoneScopeOptional, fl.FlEndpoint, fl.FlFacingOptional).AddAuthenticationFlags().Build(),
								Action: credential.ModifyOpenstackCredential,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlDescriptionOptional, fl.FlTenantUser, fl.FlTenantPassword,
										fl.FlUserDomain, fl.FlProjectDomainNameOptional, fl.FlProjectNameOptional, fl.FlDomainNameOptional, fl.FlKeystoneScopeOptional, fl.FlEndpoint, fl.FlFacingOptional).AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:   "from-file",
						Usage:  "modify a credential from input json file",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlNameOptional, fl.FlInputJson).AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: credential.ModifyCredentialFromFile,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlNameOptional, fl.FlInputJson).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a credential",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: credential.DeleteCredential,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a credential",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: credential.DescribeCredential,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "lists the credentials",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: credential.ListCredentials,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:  "prerequisites",
				Usage: "get the necessary prerequisites for credential creation",
				Subcommands: []cli.Command{
					{
						Name:   "aws",
						Usage:  "get prerequisites for aws credential creation",
						Before: cf.CheckConfigAndCommandFlags,
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: credential.GetAwsCredentialPrerequisites,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
		},
	})
}
