package main

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	hdc "github.com/sequenceiq/hdc-cli/cli"
	"github.com/urfave/cli"
	"os"
)

func ConfigRead(c *cli.Context) error {
	server := c.String(hdc.FlCBServer.Name)
	username := c.String(hdc.FlCBUsername.Name)
	password := c.String(hdc.FlCBPassword.Name)

	if len(server) == 0 || len(username) == 0 || len(password) == 0 {
		config, err := hdc.ReadConfig()
		if err != nil {
			log.Error(fmt.Sprintf("[ConfigRead] %s", err.Error()))
			log.Error(fmt.Sprintf("[ConfigRead] configuration is not set, see: %s configure --help", c.App.Name))
			return cli.NewExitError("", 1)
		}

		log.Infof("[ConfigRead] Config read from file, setting as global variable:\n%s", config.Yaml())
		if len(server) == 0 {
			c.Set(hdc.FlCBServer.Name, config.Server)
		}
		if len(c.String(username)) == 0 {
			c.Set(hdc.FlCBUsername.Name, config.Username)
		}
		if len(c.String(password)) == 0 {
			c.Set(hdc.FlCBPassword.Name, config.Password)
		}
	}
	return nil
}

func main() {

	app := cli.NewApp()
	app.Name = "hdc"
	app.Usage = ""
	app.Version = hdc.Version + "-" + hdc.BuildTime
	app.Author = "Hortonworks"

	app.Flags = []cli.Flag{
		hdc.FlDebug,
	}

	app.Before = func(c *cli.Context) error {
		log.SetOutput(os.Stderr)
		log.SetLevel(log.ErrorLevel)
		if c.Bool(hdc.FlDebug.Name) {
			log.SetLevel(log.DebugLevel)
		}
		return nil
	}

	app.Commands = []cli.Command{
		{
			Name: "configure",
			Description: fmt.Sprintf("it will save the provided server address and credential "+
				"to %s/%s/%s", hdc.GetHomeDirectory(), hdc.Hdc_dir, hdc.Config_file),
			Usage:  "configure the server address and credentials used to communicate with this server",
			Flags:  []cli.Flag{hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
			Action: hdc.Configure,
		},
		{
			Name:   "create-cluster",
			Usage:  "creates a new cluster",
			Flags:  []cli.Flag{hdc.FlCBInputJson, hdc.FlCBWait, hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
			Before: ConfigRead,
			Action: hdc.CreateCluster,
			Subcommands: []cli.Command{
				{
					Name:   "generate-cli-skeleton",
					Action: hdc.GenerateCreateClusterSkeleton,
				},
			},
		},
		{
			Name:   "list-cluster-types",
			Usage:  "list the available blueprints",
			Flags:  []cli.Flag{hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
			Before: ConfigRead,
			Action: hdc.ListBlueprints,
		},
		{
			Name:   "describe-cluster",
			Usage:  "list the available clusters",
			Flags:  []cli.Flag{hdc.FlCBClusterName, hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
			Before: ConfigRead,
			Action: hdc.DescribeCluster,
		},
		{
			Name:   "list-clusters",
			Usage:  "list the available clusters",
			Flags:  []cli.Flag{hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
			Before: ConfigRead,
			Action: hdc.ListClusters,
		},
		{
			Name:   "terminate-cluster",
			Usage:  "terminates a cluster",
			Flags:  []cli.Flag{hdc.FlCBClusterName, hdc.FlCBWait, hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
			Before: ConfigRead,
			Action: hdc.TerminateCluster,
		},
	}

	app.Run(os.Args)
}
