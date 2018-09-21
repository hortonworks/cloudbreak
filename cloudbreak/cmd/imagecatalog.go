package cmd

import (
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/imagecatalog"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "imagecatalog",
		Usage: "imagecatalog related operations",
		Subcommands: []cli.Command{
			{
				Name:   "create",
				Usage:  "creates a new imagecatalog from a URL",
				Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlURL).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: imagecatalog.CreateImagecatalogFromUrl,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlURL).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes an imagecatalog",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: imagecatalog.DeleteImagecatalog,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes an imagecatalog",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: imagecatalog.DescribeImagecatalog,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:  "images",
				Usage: "lists available images from imagecatalog",
				Subcommands: []cli.Command{
					{
						Name:  "describe",
						Usage: "provides detailed information about an image",
						Subcommands: []cli.Command{
							{
								Name:   "aws",
								Usage:  "provides detailed information about an aws image",
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddFlags(fl.FlImageId).AddOutputFlag().AddAuthenticationFlags().Build(),
								Before: cf.CheckConfigAndCommandFlags,
								Action: imagecatalog.DescribeAwsImage,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddFlags(fl.FlImageId).AddOutputFlag().AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "azure",
								Usage:  "provides detailed information about an azure image",
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddFlags(fl.FlImageId).AddOutputFlag().AddAuthenticationFlags().Build(),
								Before: cf.CheckConfigAndCommandFlags,
								Action: imagecatalog.DescribeAzureImage,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddFlags(fl.FlImageId).AddOutputFlag().AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "gcp",
								Usage:  "provides detailed information about an gcp image",
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddFlags(fl.FlImageId).AddOutputFlag().AddAuthenticationFlags().Build(),
								Before: cf.CheckConfigAndCommandFlags,
								Action: imagecatalog.DescribeGcpImage,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddFlags(fl.FlImageId).AddOutputFlag().AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
							{
								Name:   "openstack",
								Usage:  "provides detailed information about an openstack image",
								Flags:  fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddFlags(fl.FlImageId).AddOutputFlag().AddAuthenticationFlags().Build(),
								Before: cf.CheckConfigAndCommandFlags,
								Action: imagecatalog.DescribeOpenstackImage,
								BashComplete: func(c *cli.Context) {
									for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddFlags(fl.FlImageId).AddOutputFlag().AddAuthenticationFlags().Build() {
										fl.PrintFlagCompletion(f)
									}
								},
							},
						},
					},
					{
						Name:   "aws",
						Usage:  "lists available aws images from an imagecatalog",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: imagecatalog.ListAwsImages,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "azure",
						Usage:  "lists available azure images from an imagecatalog",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: imagecatalog.ListAzureImages,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "gcp",
						Usage:  "lists available gcp images from an imagecatalog",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: imagecatalog.ListGcpImages,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "openstack",
						Usage:  "lists available openstack images from an imagecatalog",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: imagecatalog.ListOpenstackImages,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlImageCatalog).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "cluster-upgrade",
						Usage:  "lists images that are valid for upgrading the cluster",
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlClusterToUpgrade).AddFlags(fl.FlImageCatalogOptional).AddOutputFlag().AddAuthenticationFlags().Build(),
						Before: cf.CheckConfigAndCommandFlags,
						Action: imagecatalog.ListImagesValidForUpgrade,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlClusterToUpgrade).AddFlags(fl.FlImageCatalog).AddOutputFlag().AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
			{
				Name:   "list",
				Usage:  "lists the available imagecatalogs",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: imagecatalog.ListImagecatalogs,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "set-default",
				Usage:  "sets the default imagecatalog",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: imagecatalog.SetDefaultImagecatalog,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
