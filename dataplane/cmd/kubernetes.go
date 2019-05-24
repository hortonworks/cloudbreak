package cmd

import (
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/kubernetes"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "kubernetes",
		Usage: "kubernetes management related operations",
		Subcommands: []cli.Command{
			{
				Name:   "create",
				Usage:  "create kubernetes configuration",
				Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlKubernetesConfigFile, fl.FlEnvironmentsOptional).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kubernetes.CreateKubernetes,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlKubernetesConfigFile, fl.FlEnvironmentsOptional).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "edit",
				Usage:  "edit kubernetes configuration",
				Flags:  fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlKubernetesConfigFile).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kubernetes.EditKubernetes,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(fl.FlKubernetesConfigFile).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "delete",
				Usage:  "deletes a kubernetes configuration",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kubernetes.DeleteKubernetes,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "describe",
				Usage:  "describes a kubernetes configuration",
				Flags:  fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kubernetes.DescribeKubernetes,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
			{
				Name:   "list",
				Usage:  "list the available kubernetes configurations",
				Flags:  fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build(),
				Before: cf.CheckConfigAndCommandFlags,
				Action: kubernetes.ListAllKubernetes,
				BashComplete: func(c *cli.Context) {
					for _, f := range fl.NewFlagBuilder().AddOutputFlag().AddAuthenticationFlags().Build() {
						fl.PrintFlagCompletion(f)
					}
				},
			},
		},
	})
}
