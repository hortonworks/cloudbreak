package main

import (
	"fmt"
	hdc "github.com/sequenceiq/hdc-cli/cli"
	"github.com/urfave/cli"
	"log"
	"os"
)

func ConfigRead(c *cli.Context) error {
	server := c.String(hdc.FlCBServer.Name)
	username := c.String(hdc.FlCBUsername.Name)
	password := c.String(hdc.FlCBPassword.Name)

	if len(server) == 0 || len(username) == 0 || len(password) == 0 {
		config, err := hdc.ReadConfig()
		if err != nil {
			log.Print(fmt.Sprintf("[ConfigRead] %s", err.Error()))
			log.Println(fmt.Sprintf("[ConfigRead] configuration is not set, see: %s configure --help", c.App.Name))
			return cli.NewExitError("", 1)
		}

		log.Printf("[ConfigRead] Config read from file, setting as global variable:\n%s", config.Yaml())
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
            Flags:   []cli.Flag{hdc.FlCBServer, hdc.FlCBUsername, hdc.FlCBPassword},
            Before:  ConfigRead,
			Action:  hdc.ListBlueprints,
		},
	}

	app.Run(os.Args)
}
