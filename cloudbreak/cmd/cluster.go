package cmd

import (
	"github.com/hortonworks/cb-cli/cloudbreak/cluster"
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	mm "github.com/hortonworks/cb-cli/cloudbreak/maintenancemode"
	"github.com/hortonworks/cb-cli/cloudbreak/stack"
	"github.com/urfave/cli"
)

var stackTemplateDescription = `Template parameters to fill in the generated template:
		userName:	Name of the Ambari user
		password:	Password of the Ambari user
		name:	Name of the cluster
		region:	Region of the cluster
		availabilityZone:	Availability zone of the cluster, on AZURE it is the same as the region
		blueprintName:	Name of the selected blueprint
		credentialName:	Name of the selected credential
		instanceGroups.group:	Name of the instance group
		instanceGroups.nodeCount:	Number of nodes in the group
		instanceGroups.template.instanceType:	Name of the selected template
		instanceGroups.template.volumeCount:	Number of volumes
		instanceGroups.template.volumeSize:	Size of Volumes in Gb
		stackAuthentication.publicKey:	Public key
`

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
				Description: stackTemplateDescription,
				Subcommands: []cli.Command{
					{
						Name:   "yarn",
						Usage:  "creates an yarn cluster JSON template",
						Before: cf.CheckConfigAndCommandFlags,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().Build(),
						Action: stack.GenerateYarnStackTemplate,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlBlueprintNameOptional, fl.FlBlueprintFileOptional).AddAuthenticationFlags().AddTemplateFlags().Build() {
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
				Name:  "maintenance-mode",
				Usage: "enable/disable maintenance mode, change stack repository configurations",
				Subcommands: []cli.Command{
					{
						Name:   "enable",
						Usage:  "enable maintenance mode",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName).Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: mm.EnableMaintenanceMode,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "disable",
						Usage:  "disable maintenance mode",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName).Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: mm.DisableMaintenanceMode,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "validate",
						Usage:  "validate repository configurations with Ambari",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName).Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: mm.ValidateRepositoryConfigurations,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "hdp",
						Usage:  "configure HDP repository data from file",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName, fl.FlInputJson).Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: mm.ChangeHdpRepo,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName, fl.FlInputJson).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "hdf",
						Usage:  "configure HDF repository data from file",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName, fl.FlInputJson).Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: mm.ChangeHdfRepo,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName, fl.FlInputJson).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "ambari",
						Usage:  "configure Ambari repository data from file",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName, fl.FlInputJson).Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: mm.ChangeAmbariRepo,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlName, fl.FlInputJson).Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:  "generate-template-json",
						Usage: "generate repository configuration json",
						Subcommands: []cli.Command{
							{
								Name:   "hdp",
								Usage:  "generate HDP repository configuration json",
								Action: mm.GenerateHdpRepoJson,
							},
							{
								Name:   "hdf",
								Usage:  "generate HDF repository configuration json",
								Action: mm.GenerateHdfRepoJson,
							},
							{
								Name:   "ambari",
								Usage:  "generate Ambari repository configuration json",
								Action: mm.GenerateAmbariRepoJson,
							},
						},
					},
				},
			},
		},
	})
}
