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
	"availabilityZone": "____",
	"cloudPlatform": "OPENSTACK",
	"clusterRequest": {
		"blueprintInputs": null,
		"blueprintName": "____",
		"gateway": {
			"enableGateway": false,
			"exposedServices": null,
			"gatewayType": "INDIVIDUAL"
		},
		"hostGroups": [
			{
				"constraint": {
					"hostCount": 1,
					"instanceGroupName": "master"
				},
				"name": "master",
				"recipeIds": null,
				"recipeNames": null,
				"recipes": null,
				"recoveryMode": "AUTO"
			},
			{
				"constraint": {
					"hostCount": 1,
					"instanceGroupName": "slave"
				},
				"name": "slave",
				"recipeIds": null,
				"recipeNames": null,
				"recipes": null,
				"recoveryMode": "AUTO"
			}
		],
		"name": "____",
		"password": "",
		"rdsConfigIds": null,
		"rdsConfigJsons": null,
		"userName": "____"
	},
	"credentialName": "____",
	"instanceGroups": [
		{
			"group": "master",
			"nodeCount": 1,
			"securityGroup": {
				"cloudPlatform": "OPENSTACK",
				"name": null,
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
				"cloudPlatform": "OPENSTACK",
				"instanceType": "____",
				"name": null,
				"parameters": {
					"encrypted": false,
					"sshLocation": "0.0.0.0/0"
				},
				"volumeCount": 1,
				"volumeSize": 10
			},
			"type": "GATEWAY"
		},
		{
			"group": "slave",
			"nodeCount": 1,
			"securityGroup": {
				"cloudPlatform": "OPENSTACK",
				"name": null,
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
				"cloudPlatform": "OPENSTACK",
				"instanceType": "____",
				"name": null,
				"parameters": {
					"encrypted": false,
					"sshLocation": "0.0.0.0/0"
				},
				"volumeCount": 1,
				"volumeSize": 10
			},
			"type": "CORE"
		}
	],
	"name": "____",
	"network": {
		"cloudPlatform": "OPENSTACK",
		"name": null,
		"parameters": {
			"publicNetId": ""
		},
		"subnetCIDR": "10.0.0.0/16"
	},
	"orchestrator": {
		"type": "SALT"
	},
	"parameters": {
		"instanceProfileStrategy": "CREATE"
	},
	"region": "____",
	"stackAuthentication": {
		"publicKey": "____"
	}
}`

var AppHelpTemplate = `NAME:
   Hortonworks Data Cloud command line tool
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
   Hortonworks Data Cloud command line tool

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
   Hortonworks Data Cloud command line tool

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
   Hortonworks Data Cloud command line tool

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
