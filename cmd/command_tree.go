package cmd

import (
	"fmt"

	dpcmd "github.com/hortonworks/cb-cli/dataplane/cmd"
	"github.com/urfave/cli"
)

const indent = "    "

func init() {
	AppCommands = append(AppCommands, cli.Command{
		Name:   "command-tree",
		Usage:  "prints the command tree",
		Action: printCommandTree,
		Hidden: true,
	})
}

func printCommandTree(c *cli.Context) {
	printCommandTreeDepth(c, dpcmd.DataPlaneCommands, 0)
}

func printCommandTreeDepth(c *cli.Context, commands []cli.Command, depth int) error {
	for _, command := range commands {
		fmt.Println(spaces(depth), "⌞", command.Name, " - ", command.Usage)
		if command.Subcommands != nil && len(command.Subcommands) > 0 {
			printCommandTreeDepth(c, command.Subcommands, depth+1)
		}
	}
	return nil
}

func spaces(depth int) string {
	result := ""
	for i := 0; i < depth; i++ {
		result = result + indent
	}
	return result
}
