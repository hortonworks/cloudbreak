package cli

import (
	"fmt"
	"github.com/urfave/cli"
	"io"
	"os"
	"strings"
	"text/tabwriter"
	"text/template"
)

var AWSCreateClusterSkeletonHelp = `
{
  "ClusterName": "my-cluster",                            // Name of the cluster
  "HDPVersion": "2.5",                                    // HDP version
  "ClusterType": "EDW-ETL: Apache Spark 2.0-preview",     // Cluster type
  "Master": {                                             // Master instance group
    "InstanceType": "m4.xlarge",                          // Instance type of master instance group
    "VolumeType": "gp2",                                  // Volume type of master instance group, accepted values: gp2, standard, ephemeral
    "VolumeSize": 32,                                     // Volume size of master instace group
    "VolumeCount": 1                                      // Volume count of master instance group
  },
  "Worker": {                                             // Worker instance group
    "InstanceType": "m3.xlarge",                          // Instance type of worker instance group
    "VolumeType": "ephemeral",                            // Volume type of worker instance group, accepted values: gp2, standard, ephemeral
    "VolumeSize": 40,                                     // Volume size of worker instace group
    "VolumeCount": 2,                                     // Volume count of master instance group
    "InstanceCount": 1                                    // Instance count of workerk instance group, accepted value: >1
  },
  "SSHKeyName": "my-existing-keypair-name",               // Name of an existing EC2 KeyPair to enable SSH access to the cluster node instances.
  "RemoteAccess": "0.0.0.0/0",                            // Allow connections from this address range. Must be a valid CIDR IP (for example: 0.0.0.0/0 will allow access from all)
  "WebAccess": true,                                      // Open access to web UI (Ambari, Spark, Zeppelin)
  "ClusterAndAmbariUser": "admin",                        // User name for Ambari and all services
  "ClusterAndAmbariPassword": "admin",                    // Password for Ambari and all services
  "InstanceRole": "CREATE",                               // (Optional) Instance role to access S3, accepted values: "", null, CREATE, existing AWS instance role name
  "Network": {                                            // (Optional) Use existing VPC and subnet
    "VpcId": "vpc-12345678",                              // Identifier of an existing VPC where the cluster will be provisioned
    "SubnetId": "subnet-12345678"                         // Identifier of an existing subnet where the cluster will be provisioned
  },
  "HiveMetastore": {                                      // (Optional) You can specify an existing Hive metastore or register a new one
   "Name": "my-hive-metastore",                           // Name of the Hive metastore, if it's an existing one only provide the name, otherwise one will be created with this name
   "Username": "hive-metastore-username",                 // Username of the Hive metastore
   "Password": "hive-metastore-password",                 // Password of the Hive metastore
   "URL": "hive.eu-west-1.rds.amazonaws.com:5432/hive",   // Connection URL of the Hive metastore
   "DatabaseType": "POSTGRES"                             // Database type of the Hive metastore, accepted values: POSTGRES, MYSQL
  }
}
`

var CommandHelpTemplate = `NAME:
   {{.HelpName}} - {{.Usage}}

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
   {{.HelpName}} - {{.Usage}}

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

func PrintHelp(out io.Writer, templ string, data interface{}) {
	funcMap := template.FuncMap{
		"join":          strings.Join,
		"requiredFlags": RequiredFlags,
		"optionalFlags": OptionalFlags,
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
