package cmd

import (
	_ "github.com/hortonworks/cb-cli/cloudbreak/cloud/aws"
	_ "github.com/hortonworks/cb-cli/cloudbreak/cloud/azure"
	_ "github.com/hortonworks/cb-cli/cloudbreak/cloud/gcp"
	_ "github.com/hortonworks/cb-cli/cloudbreak/cloud/openstack"
	_ "github.com/hortonworks/cb-cli/cloudbreak/cloud/yarn"
	cf "github.com/hortonworks/cb-cli/cloudbreak/config"
	cb "github.com/hortonworks/cb-cli/cloudbreak/connectors"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/urfave/cli"
)

func init() {
	CloudbreakCommands = append(CloudbreakCommands, cli.Command{
		Name:  "cloud",
		Usage: "information about cloud provider resources",
		Subcommands: []cli.Command{
			{
				Name:   "availability-zones",
				Usage:  "lists the available availabilityzones in a region",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlCredential, fl.FlRegion).AddOutputFlag().Build(),
				Before: cf.ConfigRead,
				Action: cb.ListAvailabilityZones,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlCredential, fl.FlRegion).AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "instances",
				Usage:  "lists the available instance types",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlCredential, fl.FlRegion, fl.FlAvailabilityZoneOptional).AddOutputFlag().Build(),
				Before: cf.ConfigRead,
				Action: cb.ListInstanceTypes,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlCredential, fl.FlRegion, fl.FlAvailabilityZoneOptional).AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "regions",
				Usage:  "lists the available regions",
				Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlCredential).AddOutputFlag().Build(),
				Before: cf.ConfigRead,
				Action: cb.ListRegions,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddFlags(fl.FlCredential).AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:  "volumes",
				Usage: "lists the available volume types",
				Subcommands: []cli.Command{
					{
						Name:   "aws",
						Usage:  "list the available aws volume types",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
						Before: cf.ConfigRead,
						Action: cb.ListAwsVolumeTypes,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "azure",
						Usage:  "list the available azure volume types",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
						Before: cf.ConfigRead,
						Action: cb.ListAzureVolumeTypes,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "gcp",
						Usage:  "list the available gcp volume types",
						Flags:  fl.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
						Before: cf.ConfigRead,
						Action: cb.ListGcpVolumeTypes,
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
