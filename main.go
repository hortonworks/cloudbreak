package main

import (
	hdc "github.com/sequenceiq/hdc-cli/cli"
	"github.com/urfave/cli"
	"os"
)

func main() {

	app := cli.NewApp()
	app.Name = "hdc-cli"
	app.Usage = ""
	app.Version = "0.0.1"
	app.Author = "Hortonworks"

	app.Commands = []cli.Command{
		{
			Name:    "configure",
			Aliases: []string{"conf"},
			Usage:   "configure the server address and credentials used to communicate with this server",
			Flags:   []cli.Flag{hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
			Action:  hdc.Configure,
		},
		{
			Name:    "blueprints",
			Aliases: []string{"bp"},
			Usage:   "list the available blueprints",
			Flags:   []cli.Flag{hdc.FlCBServer},
			Action:  hdc.ListBlueprints,
		},
	}

	app.Run(os.Args)
}
