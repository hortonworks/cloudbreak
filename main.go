package main

import (
	"fmt"
	"os"

	log "github.com/Sirupsen/logrus"
	hdc "github.com/hortonworks/hdc-cli/cli"
	"github.com/urfave/cli"
)

func ConfigRead(c *cli.Context) error {
	server := c.String(hdc.FlServer.Name)
	username := c.String(hdc.FlUsername.Name)
	password := c.String(hdc.FlPassword.Name)
	output := c.String(hdc.FlOutput.Name)

	config, err := hdc.ReadConfig(hdc.GetHomeDirectory())
	if err == nil {
		if len(output) == 0 {
			c.Set(hdc.FlOutput.Name, config.Output)
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
			c.Set(hdc.FlServer.Name, config.Server)
		}
		if len(username) == 0 {
			c.Set(hdc.FlUsername.Name, config.Username)
		}
		if len(password) == 0 {
			c.Set(hdc.FlPassword.Name, config.Password)
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
	app.HelpName = "Hortonworks Data Cloud command line tool"
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

	cli.AppHelpTemplate = hdc.AppHelpTemplate
	cli.HelpPrinter = hdc.PrintHelp
	cli.CommandHelpTemplate = hdc.CommandHelpTemplate
	cli.SubcommandHelpTemplate = hdc.SubCommandHelpTemplate

	app.Commands = []cli.Command{
		{
			Name: "configure",
			Description: fmt.Sprintf("it will save the provided server address and credential "+
				"to %s/%s/%s", hdc.GetHomeDirectory(), hdc.Hdc_dir, hdc.Config_file),
			Usage:  "configure the server address and credentials used to communicate with this server",
			Flags:  []cli.Flag{hdc.FlServerRequired, hdc.FlUsernameRequired, hdc.FlPasswordRequired, hdc.FlOutput},
			Action: hdc.Configure,
		},
		{
			Name:   "create-cluster",
			Usage:  "creates a new cluster",
			Flags:  []cli.Flag{hdc.FlInputJson, hdc.FlWait, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			After:  StopSpinner,
			Action: hdc.CreateCluster,
			Subcommands: []cli.Command{
				{
					Name:      "generate-cli-skeleton",
					Action:    hdc.GenerateCreateClusterSkeleton,
					ArgsUsage: hdc.AWSCreateClusterSkeletonHelp,
					Usage:     "generate the cluster creation template",
				},
				{
					Name:        "generate-cli-shared-skeleton",
					Action:      hdc.GenerateCreateSharedClusterSkeleton,
					Description: hdc.SharedDescription,
					Before:      ConfigRead,
					Flags:       []cli.Flag{hdc.FlClusterType, hdc.FlClusterNameOptional, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
					Usage:       "generate the cluster creation template for a specified shared cluster",
				},
				{
					Name:   "validate-cli-skeleton",
					Flags:  []cli.Flag{hdc.FlInputJson},
					Action: hdc.ValidateCreateClusterSkeleton,
					Usage:  "validate the input json",
				},
			},
		},
		{
			Name:   "describe-cluster",
			Usage:  "get a detailed description of a cluster",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.DescribeCluster,
			Subcommands: []cli.Command{
				{
					Name:   "instances",
					Usage:  "list the available instances in the cluster",
					Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
					Before: ConfigRead,
					Action: hdc.ListClusterNodes,
				},
			},
		},
		{
			Name:   "list-cluster-types",
			Usage:  "list the available cluster types and HDP versions",
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.ListBlueprints,
		},
		{
			Name:   "list-clusters",
			Usage:  "list the available clusters",
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.ListClusters,
		},
		{
			Name:   "list-metastores",
			Usage:  "list the available metastores",
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.ListRDSConfigs,
		},
		{
			Name:  "register-metastore",
			Usage: "register a new Hive metastore",
			Flags: []cli.Flag{hdc.FlRdsName, hdc.FlRdsUsername, hdc.FlRdsPassword, hdc.FlRdsUrl, hdc.FlRdsDbType, hdc.FlHdpVersion,
				hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			After:  StopSpinner,
			Action: hdc.CreateRDSConfig,
		},
		{
			Name:   "resize-cluster",
			Usage:  "change the number of worker nodes of an existing cluster",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlNodeType, hdc.FlScalingAdjustment, hdc.FlWait, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			After:  StopSpinner,
			Action: hdc.ResizeCluster,
		},
		{
			Name:   "terminate-cluster",
			Usage:  "terminates a cluster",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlWait, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			After:  StopSpinner,
			Action: hdc.TerminateCluster,
		},
	}

	// hidden commands
	app.Commands = append(app.Commands, []cli.Command{
		{
			Name:   "hidden",
			Usage:  "shows the hidden commands",
			Hidden: true,
			Action: hdc.ShowHiddenCommands,
		},
		{
			Name:   "cleanup",
			Usage:  "remove the unused resources",
			Before: ConfigRead,
			After:  StopSpinner,
			Hidden: true,
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Action: hdc.CleanupResources,
		},
		{
			Name:  "create-credential",
			Usage: "create a new credential",
			Flags: []cli.Flag{hdc.FlCredentialName, hdc.FlRoleARN, hdc.FlSSHKeyURL, hdc.FlSSHKeyPair,
				hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Hidden: true,
			After:  StopSpinner,
			Action: hdc.CreateCredential,
		},
		{
			Name:  "create-network",
			Usage: "create a new network",
			Flags: []cli.Flag{hdc.FlNetworkName, hdc.FlSubnet, hdc.FlVPC, hdc.FlIGW,
				hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Hidden: true,
			After:  StopSpinner,
			Action: hdc.CreateNetworkCommand,
		},
		{
			Name:   "delete-credential",
			Usage:  "delete a credential",
			Flags:  []cli.Flag{hdc.FlCredentialName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Hidden: true,
			After:  StopSpinner,
			Action: hdc.DeleteCredential,
		},
		{
			Name:   "delete-network",
			Usage:  "delete a network",
			Flags:  []cli.Flag{hdc.FlNetworkName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Hidden: true,
			After:  StopSpinner,
			Action: hdc.DeleteNetwork,
		},
		{
			Name:   "list-credentials",
			Usage:  "list the credentials",
			Before: ConfigRead,
			Hidden: true,
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Action: hdc.ListPrivateCredentials,
		},
		{
			Name:   "list-networks",
			Usage:  "list the networks",
			Before: ConfigRead,
			Hidden: true,
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Action: hdc.ListPrivateNetworks,
		},
	}...)

	app.Run(os.Args)
}
