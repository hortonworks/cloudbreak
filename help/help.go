package help

import (
	"fmt"
	"io"
	"os"
	"strings"
	"text/tabwriter"
	"text/template"

	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/utils"

	"github.com/urfave/cli"
)

var AppHelpTemplate = `NAME:
   Cloudbreak command line tool
USAGE:
   {{if .UsageText}}{{.UsageText}}{{else}}{{.Name}} {{if .VisibleFlags}}[global options]{{end}}{{if .Commands}} command [command options]{{end}} {{if .ArgsUsage}}{{.ArgsUsage}}{{end}}{{end}}
   {{if .Version}}{{if not .HideVersion}}
VERSION:
   {{.Version}}
   {{end}}{{end}}{{if len .Authors}}
AUTHOR(S):
   {{range .Authors}}{{.}}{{end}}
   {{end}}{{if .VisibleCommands}}
COMMANDS:{{range .VisibleCategories}}{{if .Name}}
   {{.Name}}:{{end}}{{range .VisibleCommands}}
     {{join .Names ", "}}{{"\t"}}{{.Usage}}{{end}}
{{end}}{{end}}{{if .VisibleFlags}}
GLOBAL OPTIONS:
   {{range .VisibleFlags}}{{.}}
   {{end}}{{end}}{{if .Copyright}}
COPYRIGHT:
   {{.Copyright}}
   {{end}}
`

var CommandHelpTemplate = `NAME:
   Cloudbreak command line tool

USAGE:
   {{.HelpName}}{{if .VisibleFlags}} [command options]{{end}} {{if .ArgsUsage}}{{.ArgsUsage}}{{end}}{{if .Category}}

CATEGORY:
   {{.Category}}{{end}}{{if .Description}}

DESCRIPTION:
   {{.Description}}{{end}}{{if .VisibleFlags}}{{if requiredFlags .Flags}}

REQUIRED OPTIONS:{{range requiredFlags .Flags}}
   {{.}}{{end}}{{end}}{{if optionalFlags .Flags}}

OPTIONS:
   {{range optionalFlags .Flags}}{{.}}
   {{end}}{{end}}{{end}}
`

var SubCommandHelpTemplate = `NAME:
   Cloudbreak command line tool

USAGE:
   {{.HelpName}} command{{if .VisibleFlags}} [command options]{{end}} {{if .ArgsUsage}}{{.ArgsUsage}}{{end}} {{if .Description}}

DESCRIPTION:
   {{.Description}}{{end}}

COMMANDS:{{range .VisibleCategories}}{{if .Name}}
   {{.Name}}:{{end}}{{range .VisibleCommands}}
     {{join .Names ", "}}{{"\t"}}{{.Usage}}{{end}}
{{end}}{{if .VisibleFlags}}{{if requiredFlags .Flags}}
REQUIRED OPTIONS:{{range requiredFlags .Flags}}
   {{.}}{{end}}{{end}}{{if optionalFlags .Flags}}

OPTIONS:
   {{range optionalFlags .Flags}}{{.}}
   {{end}}{{end}}{{end}}
`

var HiddenAppHelpTemplate = `NAME:
   Cloudbreak command line tool

USAGE:
   {{if .UsageText}}{{.UsageText}}{{else}}{{.HelpName}} {{if .VisibleFlags}}[global options]{{end}}{{if .Commands}} command [command options]{{end}} {{if .ArgsUsage}}{{.ArgsUsage}}{{end}}{{end}}
   {{if .Version}}{{if not .HideVersion}}
VERSION:
   {{.Version}}
   {{end}}{{end}}{{if len .Authors}}
AUTHOR(S):
   {{range .Authors}}{{.}}{{end}}
   {{end}}
COMMANDS:{{range hiddenCommands .}}
     {{join .Names ", "}}{{"\t"}}{{.Usage}}{{end}}
{{if .VisibleFlags}}
GLOBAL OPTIONS:
   {{range .VisibleFlags}}{{.}}
   {{end}}{{end}}{{if .Copyright}}
COPYRIGHT:
   {{.Copyright}}
   {{end}}
`

func HiddenCommands(app cli.App) []cli.Command {
	var commands []cli.Command
	for _, command := range app.Commands {
		if command.Hidden {
			commands = append(commands, command)
		}
	}
	return commands
}

func ShowHiddenCommands(c *cli.Context) error {
	cli.AppHelpTemplate = HiddenAppHelpTemplate
	if err := cli.ShowAppHelp(c); err != nil {
		utils.LogErrorAndExit(err)
	}
	return nil
}

func PrintHelp(out io.Writer, templ string, data interface{}) {
	funcMap := template.FuncMap{
		"join":           strings.Join,
		"requiredFlags":  fl.RequiredFlags,
		"optionalFlags":  fl.OptionalFlags,
		"hiddenCommands": HiddenCommands,
	}
	w := tabwriter.NewWriter(out, 1, 8, 2, ' ', 0)
	t := template.Must(template.New("help").Funcs(funcMap).Parse(templ))
	if err := t.Execute(w, data); err != nil {
		// If the writer is closed, t.Execute will fail, and there's nothing
		// we can do to recover.
		if os.Getenv("CLI_TEMPLATE_ERROR_DEBUG") != "" {
			fmt.Fprintf(cli.ErrWriter, "CLI TEMPLATE ERROR: %#v\n", err)
		}
		return
	}
	if err := w.Flush(); err != nil {
		utils.LogErrorAndExit(err)
	}
}
