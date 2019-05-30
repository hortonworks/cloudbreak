package main

import (
	"errors"
	"fmt"
	"os"
	"sort"

	"github.com/hortonworks/cb-cli/cmd"
	cb "github.com/hortonworks/cb-cli/dataplane/cmd"
	"github.com/hortonworks/cb-cli/dataplane/common"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/help"
	"github.com/hortonworks/cb-cli/plugin"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
	"golang.org/x/crypto/ssh/terminal"
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

	logOutput := os.Stderr
	formatter := &utils.CBFormatter{
		IsTerminal: terminal.IsTerminal(int(logOutput.Fd())),
	}
	log.SetFormatter(formatter)
	log.SetOutput(logOutput)
	log.SetLevel(log.ErrorLevel)
	if len(os.Args) > 1 && os.Args[1] == "--"+fl.FlDebugOptional.Name || os.Getenv(fl.FlDebugOptional.EnvVar) == "1" {
		log.SetLevel(log.DebugLevel)
	}

	app := cli.NewApp()
	app.Name = "dp"
	app.HelpName = "Hortonworks DataPlane command line tool"
	app.Version = common.Version + "-" + common.BuildTime
	app.Author = "Hortonworks"
	app.EnableBashCompletion = true

	app.Flags = []cli.Flag{
		fl.FlDebugOptional,
	}

	cli.AppHelpTemplate = help.AppHelpTemplate
	cli.HelpPrinter = help.PrintHelp
	cli.CommandHelpTemplate = help.CommandHelpTemplate
	cli.SubcommandHelpTemplate = help.SubCommandHelpTemplate
	app.CommandNotFound = func(c *cli.Context, command string) {
		fmt.Fprintf(c.App.Writer, "Command not found: %q\n", command)
	}

	app.Commands = append(app.Commands, cb.DataPlaneCommands...)
	app.Commands = append(app.Commands, cmd.AppCommands...)
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

	if plugin.Enabled == "true" && isNonHelperArgProvided() {
		subCmd := os.Args[1]
		argsIndex := 2
		if os.Args[1] == "--"+fl.FlDebugOptional.Name {
			if len(os.Args) < 3 {
				utils.LogErrorAndExit(errors.New("please provide a command"))
			}
			subCmd = os.Args[2]
			argsIndex = 3
		}
		found := false
		for _, c := range app.Commands {
			if c.Name == subCmd {
				found = true
				break
			}
		}
		if !found {
			plugin.DelegateCommand(subCmd, argsIndex, os.Exit)
		}
	}

	if err := app.Run(os.Args); err != nil {
		panic(err)
	}
}

func isNonHelperArgProvided() bool {
	helperArgs := map[string]bool{
		"--generate-bash-completion": true,
		"h":                          true,
		"-h":                         true,
		"help":                       true,
		"--help":                     true,
		"-v":                         true,
	}
	return len(os.Args) > 1 && !helperArgs[os.Args[1]]
}
