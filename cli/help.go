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

var AWSCreateClusterSkeletonHelp = `
{
  "ClusterName": "my-cluster",                                             // Name of the cluster
  "SharedClusterName": "my-shared-cluster",                                // Connect to the shared cluster specified here
  "HDPVersion": "2.6",                                    	               // HDP version
  "ClusterType": "EDW-ETL: Apache Hive 1.2.1, Apache Spark 2.0",           // Cluster type
  "Master": {                                                              // Master instance group
    "InstanceType": "m4.4xlarge",                                          // Instance type of master instance group
    "VolumeType": "gp2",                                                   // Volume type of master instance group, accepted values: gp2, standard, ephemeral
    "VolumeSize": 32,                                                      // Volume size of master instance group
    "VolumeCount": 1,                                                      // Volume count of master instance group
    "Encrypted": true,                                                     // (Optional) Whether to encrypt the volumes or not, accepted values: true, false
    "Recipes": [                                                           // (Optional) List of recipes
      {
        "URI": "http://some-site.com/test.sh",                             // URI of the script
        "Phase": "pre"                                                     // Which phase to run the script on, accepted values: pre, post
      }
    ]
  },
  "Worker": {                                                              // Worker instance group
    "InstanceType": "m3.xlarge",                                           // Instance type of worker instance group
    "VolumeType": "ephemeral",                                             // Volume type of worker instance group, accepted values: gp2, standard, ephemeral
    "VolumeSize": 40,                                                      // Volume size of worker instance group
    "VolumeCount": 2,                                                      // Volume count of worker instance group
    "Encrypted": true,                                                     // (Optional) Whether to encrypt the volumes or not, accepted values: true, false
    "InstanceCount": 3,                                                    // Instance count of worker instance group, accepted value: >0
    "Recipes": [                                                           // (Optional) List of recipes
      {
        "URI": "http://some-site.com/test.sh",                             // URI of the script
        "Phase": "post"                                                    // Which phase to run the script on, accepted values: pre, post
      }
    ],
    "RecoveryMode": "AUTO"                                                 // Recovery mode: AUTO or MANUAL
  },
  "Compute": {                                                             // Compute instance group
    "InstanceType": "m3.xlarge",                                           // Instance type of compute instance group
    "VolumeType": "ephemeral",                                             // Volume type of compute instance group, accepted values: gp2, standard, ephemeral
    "VolumeSize": 40,                                                      // Volume size of compute instance group
    "VolumeCount": 2,                                                      // Volume count of compute instance group
    "Encrypted": true,                                                     // (Optional) Whether to encrypt the volumes or not, accepted values: true, false
    "InstanceCount": 0,                                                    // Instance count of compute instance group, accepted value: >=0
    "Recipes": [                                                           // (Optional) List of recipes
      {
        "URI": "http://some-site.com/test.sh",                             // URI of the script
        "Phase": "post"                                                    // Which phase to run the script on, accepted values: pre, post
      }
    ],
    "SpotPrice": "0.231",                                                  // (Optional) Bid price to use for spot instances in compute instance group
    "RecoveryMode": "MANUAL"                                               // Recovery mode: AUTO or MANUAL, if spotprice is given only manual is supported
  },
  "SSHKeyName": "my-existing-keypair-name",                                // Name of an existing EC2 KeyPair to enable SSH access to the cluster node instances.
  "RemoteAccess": "0.0.0.0/0",                                             // Allow connections from this address range. Must be a valid CIDR IP (for example: 0.0.0.0/0 will allow access from all)
  "WebAccess": true,                                                       // Protected Gateway Access to Ambari and Zeppelin UIs
  "HiveJDBCAccess": true,                                                  // Protected Gateway Access to Hive JDBC
  "ClusterComponentAccess": false,                                         // Protected Gateway Access to cluster management web UIs (Name Node, Resource Manager, Job History and Spark History)
  "ClusterAndAmbariUser": "admin",                                         // User name for Ambari and all services
  "ClusterAndAmbariPassword": "admin",                                     // Password for Ambari and all services
  "InstanceRole": "CREATE",                                                // (Optional) Instance role to access Amazon API, accepted values: "", null, CREATE, existing AWS instance role name
  "Network": {                                                             // (Optional) Use existing VPC and subnet
    "VpcId": "vpc-12345678",                                               // Identifier of an existing VPC where the cluster will be provisioned
    "SubnetId": "subnet-12345678"                                          // Identifier of an existing subnet where the cluster will be provisioned
  },
  "Tags": {                                                                // (Optional) User defined tags for the cloud provider resources, like instances
    "tag-1": "value-1",
    "tag-2": "value-2"
  },
  "AmbariDatabase": {                                                      // (Optional) External Ambari database to connect to
    "DatabaseName": "ambari",                                              // Name of the database
    "Host":"mydb.eu-west-1.rds.amazonaws.com",                             // Host of the database
    "Port":5432,                                                           // Port of the database
    "Username": "postgres",                                                // Username of the database
    "Password": "postgres",                                                // Password of the database
    "DatabaseType": "POSTGRES"                                             // Type of the database, accepted values: POSTGRES, MYSQL, MARIADB, MSSQL, ORACLE, SQLANYWHERE, EMBEDDED
  }
  "HiveMetastore": {                                                       // (Optional) You can specify an existing Hive metastore or register a new one
   "Name": "my-hive-metastore",                                            // Name of the Hive metastore, if it's an existing one only provide the name, otherwise one will be created with this name
   "Username": "hive-metastore-username",                                  // Username of the Hive metastore
   "Password": "hive-metastore-password",                                  // Password of the Hive metastore
   "URL": "hive.eu-west-1.rds.amazonaws.com:5432/hive",                    // Connection URL of the Hive metastore
   "DatabaseType": "POSTGRES"                                              // Database type of the Hive metastore, accepted value: POSTGRES
  },
  "DruidMetastore": {                                                      // (Optional) You can specify an existing Druid metastore or register a new one
   "Name": "my-druid-metastore",                                           // Name of the Druid metastore, if it's an existing one only provide the name, otherwise one will be created with this name
   "Username": "druid-metastore-username",                                 // Username of the Druid metastore
   "Password": "druid-metastore-password",                                 // Password of the Druid metastore
   "URL": "druid.eu-west-1.rds.amazonaws.com:5432/druid",                  // Connection URL of the Druid metastore
   "DatabaseType": "POSTGRES"                                              // Database type of the Druid metastore, accepted value: POSTGRES
  },
  "Configurations": [{"core-site":{"fs.trash.interval":"5000"}}],          // Custom configurations, format: [{"configuration-type": {"property-name": "property-value"}}, {"configuration-type2": {"property-name": "property-value"}}]
  "Autoscaling": {                                                         // (Optional) Enable autoscaling on the cluster by default (Note: autoscaling will be enabled effectively when the cluster is successfully installed)
    "Configurations": {                                                    // (Optional) Autoscaling configuration
      "CooldownTime": 30,                                                  // Specify the time between 2 scaling activity in minutes (default: 30)
      "ClusterMinSize": 3,                                                 // During autoscaling the cluster cannot be scaled below the given number (default: 3)
      "ClusterMaxSize": 100                                                // During autoscaling the cluster cannot be scaled above the given number (default: 100)
    },
    "Policies": [                                                          // List of autoscaling policies
      {
        "PolicyName": "upscale-on-high-hdfs-usage",                        // Name of the autoscaling policy
        "ScalingAdjustment": 1,                                            // The number of nodes to adjust the cluster size with, accepted value x>0 x<0
        "ScalingDefinition": "namenode_capacity_threshold_exceeded",       // The name of the definition, to see the available options use the list-autoscaling-definitions command
        "Operator": ">",                                                   // Evaluation operator, accepted values: '<' '>'
        "Threshold": 80,                                                   // Evaluation threshold
        "Period": 1,                                                       // Time in minutes for the scaling definition to be true
        "NodeType": "worker"                                               // Node type to scale, accepted value: worker/compute
      }
    ]
  }
}`

var SharedDescription = `You can either start a new shared cluster or connect to an existing one. To start a new cluster provide only the input
	 fields based on the cluster type. If you have an existing cluster you can provide it's name when generating the skeleton with
	 the --` + FlClusterNameOptional.Name + ` option.`

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
		"requiredFlags":  RequiredFlags,
		"optionalFlags":  OptionalFlags,
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
