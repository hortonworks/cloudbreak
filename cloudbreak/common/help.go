package common

var StackTemplateDescription = `Template parameters to fill in the generated template:
		userName:	Name of the Ambari user
		password:	Password of the Ambari user
		name:	Name of the cluster
		region:	Region of the cluster
		availabilityZone:	Availability zone of the cluster, on AZURE it is the same as the region
		blueprintName:	Name of the selected blueprint
		credentialName:	Name of the selected credential
		instanceGroups.group:	Name of the instance group
		instanceGroups.nodeCount:	Number of nodes in the group
		instanceGroups.template.instanceType:	Name of the selected template
		instanceGroups.template.volumeCount:	Number of volumes
		instanceGroups.template.volumeSize:	Size of Volumes in Gb
		stackAuthentication.publicKey:	Public key
`

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
