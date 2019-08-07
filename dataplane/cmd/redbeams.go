package cmd

import (
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/aws"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/azure"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/gcp"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/openstack"
	_ "github.com/hortonworks/cb-cli/dataplane/cloud/yarn"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/redbeams"
	"github.com/urfave/cli"
)

func init() {
	DataPlaneCommands = append(DataPlaneCommands, cli.Command{
		Name:  "redbeams",
		Usage: "manage RBDMS instances and databases",
		Subcommands: []cli.Command{
			{
				Name:  "dbserver",
				Usage: "manage redbeams instances",
				Subcommands: []cli.Command{
					{
						Name:   "list",
						Usage:  "list all RBDMS instances",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn).AddAuthenticationFlags().AddOutputFlag().Build(),
						Action: redbeams.ListRBDMSInstances,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlEnvironmentCrn).AddAuthenticationFlags().AddOutputFlag().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
					{
						Name:   "delete",
						Usage:  "deletes an RBDMS instance",
						Before: cf.CheckConfigAndCommandFlagsWithoutWorkspace,
						Flags:  fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().Build(),
						Action: redbeams.DeleteRBDMSInstance,
						BashComplete: func(c *cli.Context) {
							for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlCrn).AddAuthenticationFlags().Build() {
								fl.PrintFlagCompletion(f)
							}
						},
					},
				},
			},
		},
	})
}
