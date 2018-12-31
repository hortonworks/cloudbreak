package cmd

import (
	"fmt"
	dpcmd "github.com/hortonworks/cb-cli/dataplane/cmd"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/urfave/cli"
)

const (
	newLine      = "\n"
	emptySpace   = "    "
	middleItem   = "├── "
	continueItem = "│   "
	lastItem     = "└── "
)

func init() {
	AppCommands = append(AppCommands, cli.Command{
		Name:   "command-tree",
		Usage:  "prints the command tree",
		Action: printCommandTree,
		Flags:  fl.NewFlagBuilder().AddFlags(fl.FlShowUsage).Build(),
		BashComplete: func(c *cli.Context) {
			for _, f := range fl.NewFlagBuilder().AddFlags(fl.FlShowUsage).Build() {
				fl.PrintFlagCompletion(f)
			}
		},
		Hidden: true,
	})
}

func printCommandTree(c *cli.Context) {
	tree := New(c.App.Name)
	buildTree(dpcmd.DataPlaneCommands, tree, c.Bool(fl.FlShowUsage.Name))
	fmt.Println(tree.Print())
}

func buildTree(commands []cli.Command, tree Tree, showUsage bool) {
	for _, command := range commands {
		name := command.Name
		if showUsage {
			name += " - " + command.Usage
		}
		subTree := tree.Add(name)
		if command.Subcommands != nil && len(command.Subcommands) > 0 {
			buildTree(command.Subcommands, subTree, showUsage)
		}
	}
}

type (
	tree struct {
		text  string
		items []Tree
	}

	Tree interface {
		Add(text string) Tree
		Items() []Tree
		Text() string
		Print() string
	}

	printer struct {
	}

	Printer interface {
		Print(Tree) string
	}
)

func New(text string) Tree {
	return &tree{
		text:  text,
		items: []Tree{},
	}
}

func (t *tree) Add(text string) Tree {
	n := New(text)
	t.items = append(t.items, n)
	return n
}

func (t *tree) Text() string {
	return t.text
}

func (t *tree) Items() []Tree {
	return t.items
}

func (t *tree) Print() string {
	return newPrinter().Print(t)
}

func newPrinter() Printer {
	return &printer{}
}

func (p *printer) Print(t Tree) string {
	return t.Text() + newLine + p.printItems(t.Items(), []bool{})
}

func (p *printer) printText(text string, spaces []bool, last bool) string {
	var result string
	for _, space := range spaces {
		if space {
			result += emptySpace
		} else {
			result += continueItem
		}
	}

	indicator := middleItem
	if last {
		indicator = lastItem
	}

	return result + indicator + text + newLine
}

func (p *printer) printItems(t []Tree, spaces []bool) string {
	var result string
	for i, f := range t {
		last := i == len(t)-1
		result += p.printText(f.Text(), spaces, last)
		if len(f.Items()) > 0 {
			spacesChild := append(spaces, last)
			result += p.printItems(f.Items(), spacesChild)
		}
	}
	return result
}
