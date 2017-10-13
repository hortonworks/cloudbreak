package main

import (
	"fmt"
	"os"

	log "github.com/Sirupsen/logrus"
	hdc "github.com/hortonworks/hdc-cli/cli"
	_ "github.com/hortonworks/hdc-cli/cli/cloud/aws"
	_ "github.com/hortonworks/hdc-cli/cli/cloud/azure"
	_ "github.com/hortonworks/hdc-cli/cli/cloud/gcp"
	_ "github.com/hortonworks/hdc-cli/cli/cloud/openstack"
	"github.com/hortonworks/hdc-cli/cli/utils"
	"github.com/urfave/cli"
)

func ConfigRead(c *cli.Context) error {
	args := c.Args()
	if args.Present() {
		name := args.First()
		if k := c.App.Command(name); k != nil {
			// this is a sub-command invocation
			return nil
		}
	}

	server := c.String(hdc.FlServer.Name)
	username := c.String(hdc.FlUsername.Name)
	password := c.String(hdc.FlPassword.Name)
	output := c.String(hdc.FlOutput.Name)

	config, err := hdc.ReadConfig(hdc.GetHomeDirectory())
	if err == nil {
		if len(output) == 0 {
			c.Set(hdc.FlOutput.Name, config.Output)
		}
	}

	if len(server) == 0 || len(username) == 0 || len(password) == 0 {
		if err != nil {
			log.Error(fmt.Sprintf("configuration is not set, see: hdc configure --help or provide the following flags: %v",
				[]string{"--" + hdc.FlServer.Name, "--" + hdc.FlUsername.Name, "--" + hdc.FlPassword.Name}))
			os.Exit(1)
		}

		PrintConfig(*config)

		if len(server) == 0 {
			c.Set(hdc.FlServer.Name, config.Server)
		}
		if len(username) == 0 {
			c.Set(hdc.FlUsername.Name, config.Username)
		}
		if len(password) == 0 {
			c.Set(hdc.FlPassword.Name, config.Password)
		}
	}
	return nil
}

func PrintConfig(cfg hdc.Config) {
	cfg.Password = "*"
	log.Infof("[ConfigRead] Config read from file, setting as global variable:\n%s", cfg.Yaml())
}

func printFlagCompletion(f cli.Flag) {
	fmt.Printf("--%s\n", f.GetName())
}

func main() {

	app := cli.NewApp()
	app.Name = "hdc"
	app.HelpName = "Hortonworks Data Cloud command line tool"
	app.Version = hdc.Version + "-" + hdc.BuildTime
	app.Author = "Hortonworks"
	app.EnableBashCompletion = true

	app.Flags = []cli.Flag{
		hdc.FlDebug,
	}

	app.Before = func(c *cli.Context) error {
		log.SetOutput(os.Stderr)
		log.SetLevel(log.ErrorLevel)
		formatter := &utils.CBFormatter{}
		log.SetFormatter(formatter)
		if c.Bool(hdc.FlDebug.Name) {
			log.SetLevel(log.DebugLevel)
		}
		return nil
	}

	cli.AppHelpTemplate = hdc.AppHelpTemplate
	cli.HelpPrinter = hdc.PrintHelp
	cli.CommandHelpTemplate = hdc.CommandHelpTemplate
	cli.SubcommandHelpTemplate = hdc.SubCommandHelpTemplate

	app.Commands = []cli.Command{
		{
			Name: "configure",
			Description: fmt.Sprintf("it will save the provided server address and credential "+
				"to %s/%s/%s", hdc.GetHomeDirectory(), hdc.Hdc_dir, hdc.Config_file),
			Usage:  "configure the server address and credentials used to communicate with this server",
			Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlServerRequired, hdc.FlUsernameRequired, hdc.FlPassword).AddOutputFlag().Build(),
			Action: hdc.Configure,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlServerRequired, hdc.FlUsernameRequired, hdc.FlPassword).AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:  "create-credential",
			Usage: "creates a new credential",
			Subcommands: []cli.Command{
				{
					Name:  "aws",
					Usage: "creates a new aws credential",
					Subcommands: []cli.Command{
						{
							Name:   "role-based",
							Usage:  "creates a new role based aws credential",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlRoleARN).AddAuthenticationFlags().Build(),
							Action: hdc.CreateAwsCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlRoleARN).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "key-based",
							Usage:  "creates a new key based aws credential",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlAccessKey, hdc.FlSecretKey).AddAuthenticationFlags().Build(),
							Action: hdc.CreateAwsCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlAccessKey, hdc.FlSecretKey).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
					},
				},
				{
					Name:  "azure",
					Usage: "creates a new azure credential",
					Subcommands: []cli.Command{
						{
							Name:   "app-based",
							Usage:  "creates a new app based azure credential",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlSubscriptionId, hdc.FlTenantId, hdc.FlAppId, hdc.FlAppPassword).AddAuthenticationFlags().Build(),
							Action: hdc.CreateAzureCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlSubscriptionId, hdc.FlTenantId, hdc.FlAppId, hdc.FlAppPassword).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
					},
				},
				{
					Name:   "gcp",
					Usage:  "creates a new gcp credential",
					Before: ConfigRead,
					Flags:  hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlProjectId, hdc.FlServiceAccountId, hdc.FlServiceAccountPrivateKeyFile).AddAuthenticationFlags().Build(),
					Action: hdc.CreateGcpCredential,
					BashComplete: func(c *cli.Context) {
						for _, f := range hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlProjectId, hdc.FlServiceAccountId, hdc.FlServiceAccountPrivateKeyFile).AddAuthenticationFlags().Build() {
							printFlagCompletion(f)
						}
					},
				},
				{
					Name:  "openstack",
					Usage: "creates a new openstack credential",
					Subcommands: []cli.Command{
						{
							Name:   "keystone-v2",
							Usage:  "creates a new keystone version 2 openstack credential",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlTenantUser, hdc.FlTenantPassword, hdc.FlTenantName, hdc.FlEndpoint, hdc.FlFacing).AddAuthenticationFlags().Build(),
							Action: hdc.CreateOpenstackCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlTenantUser, hdc.FlTenantPassword, hdc.FlTenantName, hdc.FlEndpoint, hdc.FlFacing).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "keystone-v3",
							Usage:  "creates a new keystone version 3 openstack credential",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlTenantUser, hdc.FlTenantPassword, hdc.FlUserDomain, hdc.FlKeystoneScope, hdc.FlEndpoint, hdc.FlFacing).AddAuthenticationFlags().Build(),
							Action: hdc.CreateOpenstackCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlTenantUser, hdc.FlTenantPassword, hdc.FlUserDomain, hdc.FlKeystoneScope, hdc.FlEndpoint, hdc.FlFacing).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
					},
				},
			},
		},
		{
			Name:   "describe-credential",
			Usage:  "describes a credential",
			Before: ConfigRead,
			Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: hdc.DescribeCredential,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-credentials",
			Usage:  "lists the credentials",
			Before: ConfigRead,
			Flags:  hdc.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: hdc.ListCredentials,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "delete-credential",
			Usage:  "deletes a credential",
			Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddAuthenticationFlags().Build(),
			Before: ConfigRead,
			Action: hdc.DeleteCredential,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddAuthenticationFlags().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "create-blueprint",
			Usage:  "adds a new Ambari blueprint from a file or from a URL",
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Subcommands: []cli.Command{
				{
					Name:   "from-url",
					Flags:  hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlBlueprintURL).AddAuthenticationFlags().Build(),
					Before: ConfigRead,
					Action: hdc.CreateBlueprintFromUrl,
					Usage:  "creates a blueprint by downloading it from a URL location",
					BashComplete: func(c *cli.Context) {
						for _, f := range hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlBlueprintURL).AddAuthenticationFlags().Build() {
							printFlagCompletion(f)
						}
					},
				},
				{
					Name:   "from-file",
					Flags:  hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlBlueprintFileLocation).AddAuthenticationFlags().Build(),
					Before: ConfigRead,
					Action: hdc.CreateBlueprintFromFile,
					Usage:  "creates a blueprint by reading it from a local file",
					BashComplete: func(c *cli.Context) {
						for _, f := range hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlBlueprintFileLocation).AddAuthenticationFlags().Build() {
							printFlagCompletion(f)
						}
					},
				},
			},
		},
		{
			Name:   "describe-blueprint",
			Usage:  "describes a blueprint",
			Before: ConfigRead,
			Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: hdc.DescribeBlueprint,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-blueprints",
			Usage:  "lists the available blueprints",
			Flags:  hdc.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
			Before: ConfigRead,
			Action: hdc.ListBlueprints,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "delete-blueprint",
			Usage:  "deletes a blueprint from Cloudbreak",
			Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
			Before: ConfigRead,
			Action: hdc.DeleteBlueprint,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:  "generate-cluster-template",
			Usage: "creates a cluster JSON template",
			Subcommands: []cli.Command{
				{
					Name:  "aws",
					Usage: "creates an aws cluster JSON template",
					Subcommands: []cli.Command{
						{
							Name:   "new-network",
							Usage:  "creates an aws cluster JSON template with new network",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateAwsStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-network",
							Usage:  "creates an aws cluster JSON template with existing network",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateAwsStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-subnet",
							Usage:  "creates an aws cluster JSON template with existing subnet",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateAwsStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
					},
				},
				{
					Name:  "azure",
					Usage: "creates an azure cluster JSON template",
					Subcommands: []cli.Command{
						{
							Name:   "new-network",
							Usage:  "creates an azure cluster JSON template with new network",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateAzureStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-subnet",
							Usage:  "creates an azure cluster JSON template with existing subnet",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateAzureStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
					},
				},
				{
					Name:  "gcp",
					Usage: "creates an gcp cluster JSON template",
					Subcommands: []cli.Command{
						{
							Name:   "new-network",
							Usage:  "creates a gcp cluster JSON template with new network",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateGcpStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-network",
							Usage:  "creates a gcp cluster JSON template with existing network",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateGcpStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-subnet",
							Usage:  "creates a gcp cluster JSON template with existing subnet",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateGcpStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "legacy-network",
							Usage:  "creates a gcp cluster JSON template with legacy network",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateGcpStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
					},
				},
				{
					Name:  "openstack",
					Usage: "creates an openstack cluster JSON template",
					Subcommands: []cli.Command{
						{
							Name:   "new-network",
							Usage:  "creates a openstack cluster JSON template with new network",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateOpenstackStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-network",
							Usage:  "creates a openstack cluster JSON template with existing network",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateOpenstackStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-subnet",
							Usage:  "creates a openstack cluster JSON template with existing subnet",
							Before: ConfigRead,
							Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: hdc.GenerateOpenstackStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlBlueprintName, hdc.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
					},
				},
			},
		},
		{
			Name:      "create-cluster",
			Usage:     "creates a new cluster",
			Flags:     hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlInputJson, hdc.FlAmbariPasswordOptional, hdc.FlWait).AddAuthenticationFlags().Build(),
			Before:    ConfigRead,
			Action:    hdc.CreateStack,
			ArgsUsage: hdc.StackTemplateHelp,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(hdc.FlInputJson, hdc.FlAmbariPasswordOptional, hdc.FlWait).AddAuthenticationFlags().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "describe-cluster",
			Usage:  "describes a cluster",
			Before: ConfigRead,
			Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: hdc.DescribeStack,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-clusters",
			Usage:  "lists the running clusters",
			Flags:  hdc.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
			Before: ConfigRead,
			Action: hdc.ListStacks,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "delete-cluster",
			Usage:  "deletes a cluster",
			Flags:  hdc.NewFlagBuilder().AddFlags(hdc.FlName, hdc.FlForce).AddAuthenticationFlags().Build(),
			Before: ConfigRead,
			Action: hdc.DeleteStack,
			BashComplete: func(c *cli.Context) {
				for _, f := range hdc.NewFlagBuilder().AddFlags(hdc.FlName, hdc.FlForce).AddAuthenticationFlags().Build() {
					printFlagCompletion(f)
				}
			},
		},
	}

	// internal commands
	app.Commands = append(app.Commands, []cli.Command{
		{
			Name:   "internal",
			Usage:  "shows the internal commands",
			Hidden: true,
			Action: hdc.ShowHiddenCommands,
		},
	}...)

	app.Run(os.Args)
}
