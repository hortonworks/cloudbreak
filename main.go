package main

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	hdc "github.com/hortonworks/hdc-cli/cli"
	"github.com/urfave/cli"
	"os"
)

func ConfigRead(c *cli.Context) error {
	server := c.String(hdc.FlCBServer.Name)
	username := c.String(hdc.FlCBUsername.Name)
	password := c.String(hdc.FlCBPassword.Name)
	output := c.String(hdc.FlCBOutput.Name)

	config, err := hdc.ReadConfig()
	if err == nil {
		if len(output) == 0 {
			c.Set(hdc.FlCBOutput.Name, config.Output)
		}
	}

	if len(server) == 0 || len(username) == 0 || len(password) == 0 {
		if err != nil {
			log.Error(fmt.Sprintf("[ConfigRead] %s", err.Error()))
			log.Error(fmt.Sprintf("[ConfigRead] configuration is not set, see: %s configure --help", c.App.Name))
			os.Exit(1)
		}

		PrintConfig(*config)

		if len(server) == 0 {
			c.Set(hdc.FlCBServer.Name, config.Server)
		}
		if len(username) == 0 {
			c.Set(hdc.FlCBUsername.Name, config.Username)
		}
		if len(password) == 0 {
			c.Set(hdc.FlCBPassword.Name, config.Password)
		}
	}
	return nil
}

func PrintConfig(cfg hdc.Config) {
	cfg.Password = "*"
	log.Infof("[ConfigRead] Config read from file, setting as global variable:\n%s", cfg.Yaml())
}

func StopSpinner(c *cli.Context) error {
	hdc.StopSpinner()
	return nil
}

func main() {

	app := cli.NewApp()
	app.Name = "hdc"
	app.Usage = "Hortonworks Data Cloud command line tool"
	app.Version = hdc.Version + "-" + hdc.BuildTime
	app.Author = "Hortonworks"

	app.Flags = []cli.Flag{
		hdc.FlDebug,
	}

	app.Before = func(c *cli.Context) error {
		log.SetOutput(os.Stderr)
		log.SetLevel(log.ErrorLevel)
		formatter := &hdc.CBFormatter{}
		log.SetFormatter(formatter)
		if c.Bool(hdc.FlDebug.Name) {
			log.SetLevel(log.DebugLevel)
			hdc.Spinner = nil
		}
		return nil
	}

	cli.HelpPrinter = hdc.PrintHelp
	cli.CommandHelpTemplate = hdc.CommandHelpTemplate
	cli.SubcommandHelpTemplate = hdc.SubCommandHelpTemplate

	app.Commands = []cli.Command{
		{
			Name: "configure",
			Description: fmt.Sprintf("it will save the provided server address and credential "+
				"to %s/%s/%s", hdc.GetHomeDirectory(), hdc.Hdc_dir, hdc.Config_file),
			Usage:  "configure the server address and credentials used to communicate with this server",
			Flags:  []cli.Flag{hdc.FlCBServerRequired, hdc.FlCBUsernameRequired, hdc.FlCBPasswordRequired, hdc.FlCBOutput},
			Action: hdc.Configure,
		},
		{
			Name:   "create-cluster",
			Usage:  "creates a new cluster",
			Flags:  []cli.Flag{hdc.FlCBInputJson, hdc.FlCBWait, hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
			Before: ConfigRead,
			After:  StopSpinner,
			Action: hdc.CreateCluster,
			Subcommands: []cli.Command{
				{
					Name:      "generate-cli-skeleton",
					Action:    hdc.GenerateCreateClusterSkeleton,
					ArgsUsage: hdc.AWSCreateClusterSkeletonHelp,
				},
				{
					Name:   "validate-cli-skeleton",
					Flags:  []cli.Flag{hdc.FlCBInputJson},
					Action: hdc.ValidateCreateClusterSkeleton,
					Usage:  "validate the input json",
				},
			},
		},
		{
			Name:   "describe-cluster",
			Usage:  "get a detailed description of a cluster",
			Flags:  []cli.Flag{hdc.FlCBClusterName, hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword, hdc.FlCBOutput},
			Before: ConfigRead,
			Action: hdc.DescribeCluster,
			Subcommands: []cli.Command{
				{
					Name:   "instances",
					Usage:  "list the available instances in the cluster",
					Flags:  []cli.Flag{hdc.FlCBClusterName, hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword, hdc.FlCBOutput},
					Before: ConfigRead,
					Action: hdc.ListClusterNodes,
				},
			},
		},
		{
			Name:   "list-cluster-types",
			Usage:  "list available cluster types and HDP versions",
			Flags:  []cli.Flag{hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword, hdc.FlCBOutput},
			Before: ConfigRead,
			Action: hdc.ListBlueprints,
		},
		{
			Name:   "list-clusters",
			Usage:  "list available clusters",
			Flags:  []cli.Flag{hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword, hdc.FlCBOutput},
			Before: ConfigRead,
			Action: hdc.ListClusters,
		},
		{
			Name:   "resize-cluster",
			Usage:  "change the number of worker nodes of an existing cluster",
			Flags:  []cli.Flag{hdc.FlCBClusterName, hdc.FlCBScalingAdjustment, hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword, hdc.FlCBOutput},
			Before: ConfigRead,
			After:  StopSpinner,
			Action: hdc.ResizeCluster,
		},
		{
			Name:   "terminate-cluster",
			Usage:  "terminates a cluster",
			Flags:  []cli.Flag{hdc.FlCBClusterName, hdc.FlCBWait, hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
			Before: ConfigRead,
			After:  StopSpinner,
			Action: hdc.TerminateCluster,
		},
	}

	// hidden commands
	app.Commands = append(app.Commands, []cli.Command{
		{
			Name:   "create-credential",
			Usage:  "create a new credential",
			Flags:  []cli.Flag{hdc.FlCBCredentialName, hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword, hdc.FlCBOutput},
			Before: ConfigRead,
			Hidden: true,
			After:  StopSpinner,
			Action: hdc.CreateCredential,
		},
	}...)

	app.Run(os.Args)
}
