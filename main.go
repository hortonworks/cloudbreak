package main

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cloudbreak/cmd"
	"github.com/hortonworks/cb-cli/cloudbreak/common"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/cloudbreak/help"
	"github.com/hortonworks/cb-cli/utils"
	"github.com/urfave/cli"
	"os"
	"sort"
)

func sortByName(commands []cli.Command) {
	sort.Slice(commands, func(i, j int) bool {
		return commands[i].Name < commands[j].Name
	})
}

func main() {
	defer func() {
		if r := recover(); r != nil {
			log.Debug(r)
			os.Exit(1)
		}
	}()

	app := cli.NewApp()
	app.Name = "cb"
	app.HelpName = "Hortonworks Data Cloud command line tool"
	app.Version = common.Version + "-" + common.BuildTime
	app.Author = "Hortonworks"
	app.EnableBashCompletion = true

	app.Flags = []cli.Flag{
		fl.FlDebugOptional,
	}

	app.Before = func(c *cli.Context) error {
		log.SetOutput(os.Stderr)
		log.SetLevel(log.ErrorLevel)
		formatter := &utils.CBFormatter{}
		log.SetFormatter(formatter)
		if c.Bool(fl.FlDebugOptional.Name) {
			log.SetLevel(log.DebugLevel)
		}
		return nil
	}

	cli.AppHelpTemplate = common.AppHelpTemplate
	cli.HelpPrinter = help.PrintHelp
	cli.CommandHelpTemplate = common.CommandHelpTemplate
	cli.SubcommandHelpTemplate = common.SubCommandHelpTemplate
	app.CommandNotFound = func(c *cli.Context, command string) {
		fmt.Fprintf(c.App.Writer, "Command not found: %q\n", command)
	}

	app.Commands = append(app.Commands, cmd.CloudbreakCommands...)
	sortByName(app.Commands)

	// internal commands
	app.Commands = append(app.Commands, []cli.Command{
		{
			Name:   "internal",
			Usage:  "shows the internal commands",
			Hidden: true,
			Action: help.ShowHiddenCommands,
		},
	}...)

	if err := app.Run(os.Args); err != nil {
		panic(err)
	}
}
