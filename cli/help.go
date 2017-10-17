package cli

import (
	"fmt"
	"io"
	"os"
	"strings"
	"text/tabwriter"
	"text/template"

	"github.com/urfave/cli"
)

var StackTemplateHelp = `
{
	"availabilityZone": "____",							// Availability zone of the cluster on AZURE it is the same as the region
	"clusterRequest": {
		"ambariRequest": {
			"blueprintName": "____",					// Name of the selected blueprint
			"password": "",								// Password of the Ambari user
			"userName": "____"							// Name of the Ambari user
		}
	},
	"credentialName": "____",							// Name of the selected credential
	"instanceGroups": [
		{
			"group": "master",							// Name of the instance group
			"nodeCount": 1,								// Number of nodes in the group
			"securityGroup": {
				"securityRules": [
					{
						"ports": "22",
						"protocol": "tcp",
						"subnet": "0.0.0.0/0"
					},
					{
						"ports": "443",
						"protocol": "tcp",
						"subnet": "0.0.0.0/0"
					},
					{
						"ports": "9443",
						"protocol": "tcp",
						"subnet": "0.0.0.0/0"
					}
				]
			},
			"template": {
				"instanceType": "____",					// Name of the selected template
				"parameters": {
					"encrypted": false,
					"sshLocation": "0.0.0.0/0"
				},
				"volumeCount": 1,						// Number of volumes
				"volumeSize": 10						// Size of Volumes in Gb
			},
			"type": "GATEWAY"							// Type of the group [GATEWAY, CORE]
		}
	],
	"name": "____",										// Name of the cluster
	"orchestrator": {
		"type": "SALT"									// Type of the orhestrator [SALT, CONTAINER]
	},
	"region": "____",									// Region of the cluster
	"stackAuthentication": {
		"publicKey": "____"								// Public key
	}
}`

var AppHelpTemplate = `NAME:
   Cloudbreak command line tool
USAGE:
   {{if .UsageText}}{{.UsageText}}{{else}}{{.Name}} {{if .VisibleFlags}}[global options]{{end}}{{if .Commands}} command [command options]{{end}} {{if .ArgsUsage}}{{.ArgsUsage}}{{else}}[arguments...]{{end}}{{end}}
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
   {{.HelpName}}{{if .VisibleFlags}} [command options]{{end}} {{if .ArgsUsage}}{{.ArgsUsage}}{{else}}[arguments...]{{end}}{{if .Category}}

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
   {{.HelpName}} command{{if .VisibleFlags}} [command options]{{end}} {{if .ArgsUsage}}{{.ArgsUsage}}{{else}}[arguments...]{{end}}

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
   {{if .UsageText}}{{.UsageText}}{{else}}{{.HelpName}} {{if .VisibleFlags}}[global options]{{end}}{{if .Commands}} command [command options]{{end}} {{if .ArgsUsage}}{{.ArgsUsage}}{{else}}[arguments...]{{end}}{{end}}
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
	cli.ShowAppHelp(c)
	return nil
}

func PrintHelp(out io.Writer, templ string, data interface{}) {
	funcMap := template.FuncMap{
		"join":           strings.Join,
		"requiredFlags":  requiredFlags,
		"optionalFlags":  optionalFlags,
		"hiddenCommands": HiddenCommands,
	}
	w := tabwriter.NewWriter(out, 1, 8, 2, ' ', 0)
	t := template.Must(template.New("help").Funcs(funcMap).Parse(templ))
	err := t.Execute(w, data)
	if err != nil {
		// If the writer is closed, t.Execute will fail, and there's nothing
		// we can do to recover.
		if os.Getenv("CLI_TEMPLATE_ERROR_DEBUG") != "" {
			fmt.Fprintf(cli.ErrWriter, "CLI TEMPLATE ERROR: %#v\n", err)
		}
		return
	}
	w.Flush()
}
