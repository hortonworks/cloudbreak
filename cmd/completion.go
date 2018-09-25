package cmd

import (
	"github.com/hortonworks/cb-cli/completion"
	"github.com/urfave/cli"
)

func init() {
	AppCommands = append(AppCommands, cli.Command{
		Name:   "completion",
		Usage:  "prints the bash completion function",
		Action: completion.PrintBashCompletion,
	})
}
