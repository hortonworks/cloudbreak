package cmd

import (
	"github.com/hortonworks/cb-cli/cloudbreak/cluster"
	"github.com/hortonworks/cb-cli/cloudbreak/common"
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/stack"
	"github.com/hortonworks/cb-cli/cloudbreak/tag"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "cluster",
		Usage: "cluster related operations",
		Subcommands: []cli.Command{
			{
				Name:   "change-ambari-password",
				Usage:  "changes Ambari password",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlOldPassword, fl.FlNewPassword, fl.FlAmbariUser).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: cluster.ChangeAmbariPassword,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlOldPassword, fl.FlNewPassword, fl.FlAmbariUser).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "change-image",
				Usage:  "changes image of the cluster - will be used when creating new instances or repairing failed ones",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlImageId, fl.FlImageCatalogOptional).AddAuthenticationFlags().Build(),
				Action: stack.ChangeImage,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlImageId).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:        "create",
				Usage:       "creates a new cluster",
				Description: `use 'cb cluster generate-template' for cluster request JSON generation`,
				Flags:       fl.NewFlagBuilder().AddResourceFlagsWithOptionalName().AddFlags(fl.FlInputJson, fl.FlAmbariPasswordOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build(),
				Before:      cf.CheckConfigAndCommandFlags,
				Action:      stack.CreateStack,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceFlagsWithOptionalName().AddFlags(fl.FlInputJson, fl.FlAmbariPasswordOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a cluster",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlForceOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: stack.DeleteStack,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlForceOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.DescribeStack,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:        "generate-template",
				Usage:       "creates a cluster JSON template",
				Description: common.StackTemplateDescription,
				Subcommands: []cli.Command{
					{
						Name:   "yarn",
						Usage:  "creates an yarn cluster JSON template",
						Before: cf.CheckConfigAndCommandFlags,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().AddTemplateFlags().Build(),
						Action: stack.GenerateYarnStackTemplate,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().AddTemplateFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "aws",
						Usage: "creates an aws cluster JSON template",
						Subcommands: []cli.Command{
							{
								Name:   "new-network",
								Usage:  "creates an aws cluster JSON template with new network",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlDefaultEncryptionOptional, fl.FlCustomEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateAwsStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlDefaultEncryptionOptional, fl.FlCustomEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "existing-network",
								Usage:  "creates an aws cluster JSON template with existing network",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlDefaultEncryptionOptional, fl.FlCustomEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateAwsStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlDefaultEncryptionOptional, fl.FlCustomEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "existing-subnet",
								Usage:  "creates an aws cluster JSON template with existing subnet",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlDefaultEncryptionOptional, fl.FlCustomEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateAwsStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlDefaultEncryptionOptional, fl.FlCustomEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "azure",
						Usage: "creates an azure cluster JSON template",
						Subcommands: []cli.Command{
							{
								Name:   "new-network",
								Usage:  "creates an azure cluster JSON template with new network",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateAzureStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "existing-subnet",
								Usage:  "creates an azure cluster JSON template with existing subnet",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateAzureStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "gcp",
						Usage: "creates an gcp cluster JSON template",
						Subcommands: []cli.Command{
							{
								Name:   "new-network",
								Usage:  "creates a gcp cluster JSON template with new network",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlRawEncryptionOptional, fl.FlRsaEncryptionOptional, fl.FlKmsEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateGcpStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlRawEncryptionOptional, fl.FlRsaEncryptionOptional, fl.FlKmsEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "existing-network",
								Usage:  "creates a gcp cluster JSON template with existing network",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlRawEncryptionOptional, fl.FlRsaEncryptionOptional, fl.FlKmsEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateGcpStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlRawEncryptionOptional, fl.FlRsaEncryptionOptional, fl.FlKmsEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "existing-subnet",
								Usage:  "creates a gcp cluster JSON template with existing subnet",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlRawEncryptionOptional, fl.FlRsaEncryptionOptional, fl.FlKmsEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateGcpStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional, fl.FlRawEncryptionOptional, fl.FlRsaEncryptionOptional, fl.FlKmsEncryptionOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "legacy-network",
								Usage:  "creates a gcp cluster JSON template with legacy network",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateGcpStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:  "openstack",
						Usage: "creates an openstack cluster JSON template",
						Subcommands: []cli.Command{
							{
								Name:   "new-network",
								Usage:  "creates a openstack cluster JSON template with new network",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateOpenstackStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "existing-network",
								Usage:  "creates a openstack cluster JSON template with existing network",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateOpenstackStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "existing-subnet",
								Usage:  "creates a openstack cluster JSON template with existing subnet",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
								Action: stack.GenerateOpenstackStackTemplate,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
				},
			},
			{
				Name:   "generate-reinstall-template",
				Hidden: true,
				Usage:  "generates reinstall template",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlBlueprintName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.GenerateReinstallTemplate,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlBlueprintName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "generate-attached-cluster-template",
				Usage:  "generates attached cluster template",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlWithSourceCluster, fl.FlBlueprintName, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.GenerateAtachedStackTemplate,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlWithSourceCluster, fl.FlBlueprintName, fl.FlBlueprintFileOptional, fl.FlCloudStorageTypeOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "lists the running clusters",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: stack.ListStacks,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "reinstall",
				Hidden: true,
				Usage:  "reinstalls a cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlBlueprintNameOptional, fl.FlKerberosPasswordOptional, fl.FlKerberosPrincipalOptional, fl.FlInputJson, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.ReinstallStack,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlBlueprintNameOptional, fl.FlKerberosPasswordOptional, fl.FlKerberosPrincipalOptional, fl.FlInputJson, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "repair",
				Usage:  "repairs a cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlHostGroups, fl.FlRemoveOnly, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.RepairStack,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlHostGroups, fl.FlRemoveOnly, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "retry",
				Usage:  "retries the creation of a cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.RetryCluster,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "scale",
				Usage:  "scales a cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlGroupName, fl.FlDesiredNodeCount, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.ScaleStack,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlGroupName, fl.FlDesiredNodeCount, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "start",
				Usage:  "starts a cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.StartStack,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "stop",
				Usage:  "stops a cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.StopStack,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "sync",
				Usage:  "synchronizes a cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: stack.SyncStack,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "tags",
				Hidden: true,
				Usage:  "default tags related operations",
				Subcommands: []cli.Command{
					{
						Name:  "account",
						Usage: "manipulates default tags for the account",
						Subcommands: []cli.Command{
							{
								Name:   "add",
								Usage:  "adds a new default tag to the account",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlKey, fl.FlValue).AddAuthenticationFlags().AddOutputFlag().Build(),
								Action: tag.AddAccountTag,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlKey, fl.FlValue).AddAuthenticationFlags().AddOutputFlag().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "delete",
								Usage:  "deletes a default tag of the account",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlKey).AddAuthenticationFlags().AddOutputFlag().Build(),
								Action: tag.DeleteAccountTag,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlKey).AddAuthenticationFlags().AddOutputFlag().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "list",
								Usage:  "lists the default tags for the account",
								Before: cf.CheckConfigAndCommandFlags,
								Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
								Action: tag.ListAccountTags,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
				},
			},
		},
	})
}
