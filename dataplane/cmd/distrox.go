package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	"github.com/hortonworks/cb-cli/dataplane/distrox"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	mm "github.com/hortonworks/cb-cli/dataplane/maintenancemode"
	"github.com/urfave/cli"
)

var distroXTemplateDescription = `Template parameters to fill in the generated template:
		userName:	Name of the CM user
		password:	Password of the CM user
		name:	Name of the DistroX cluster
		region:	Region of the DistroX cluster
		availabilityZone:	Availability zone of the DistroX cluster, on AZURE it is the same as the region
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
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "distrox",
		Usage: "DistroX related operations",
		Subcommands: []cli.Command{
			{
				Name:   "change-image",
				Usage:  "changes image of the DistroX cluster - will be used when creating new instances or repairing failed ones",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlImageId, fl.FlImageCatalogOptional).AddAuthenticationFlags().Build(),
				Action: distrox.ChangeImage,
				Hidden: true,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlImageId).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:        "create",
				Usage:       "creates a new DistroX cluster",
				Description: `use 'dp cluster generate-template' for cluster request JSON generation`,
				Flags:       fl.NewFlagBuilder().AddResourceFlagsWithOptionalName().AddFlags(fl.FlInputJson, fl.FlCMUserOptional, fl.FlCMPasswordOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build(),
				Before:      cf.CheckConfigAndCommandFlags,
				Action:      distrox.CreateDistroX,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceFlagsWithOptionalName().AddFlags(fl.FlInputJson, fl.FlCMUserOptional, fl.FlCMPasswordOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a DistroX cluster",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlForceOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: distrox.DeleteDistroX,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlForceOptional, fl.FlWaitOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a DistroX cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: distrox.DescribeDistroX,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "lists the running DistroX clusters",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: distrox.ListDistroXs,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:        "repair",
				Usage:       "repairs a DistroX",
				Description: "repairs a DistroX",
				Subcommands: []cli.Command{
					{
						Name:   "host-groups",
						Usage:  "repairs host-groups of a cluster",
						Before: cf.CheckConfigAndCommandFlags,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlHostGroups, fl.FlRemoveOnly, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: distrox.RepairDistroXHostGroups,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlHostGroups, fl.FlRemoveOnly, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "nodes",
						Usage:  "repairs nodes of a DistroX",
						Before: cf.CheckConfigAndCommandFlags,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlNodes, fl.FlDeleteVolumes, fl.FlRemoveOnly, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: distrox.RepairDistroXNodes,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlNodes, fl.FlDeleteVolumes, fl.FlRemoveOnly, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "retry",
				Usage:  "retries the creation of a cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: distrox.RetryDistroX,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "scale",
				Usage:  "scales a DistroX cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlGroupName, fl.FlDesiredNodeCount, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: distrox.ScaleDistroX,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlGroupName, fl.FlDesiredNodeCount, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "start",
				Usage:  "starts a DistroX cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: distrox.StartDistroX,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "stop",
				Usage:  "stops a DistroX cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: distrox.StopDistroX,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName, fl.FlWaitOptional).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "sync",
				Usage:  "synchronizes a DistroX cluster",
				Before: cf.CheckConfigAndCommandFlags,
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Action: distrox.SyncDistroX,
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
						Usage:  "validate repository configurations with CM",
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
						Hidden: true,
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
						Hidden: true,
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
						Hidden: true,
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
						Hidden: true,
					},
				},
			},
		},
	})
}
