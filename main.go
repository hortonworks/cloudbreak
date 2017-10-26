package main

import (
	"fmt"
	"os"

	log "github.com/Sirupsen/logrus"
	cb "github.com/hortonworks/cb-cli/cli"
	_ "github.com/hortonworks/cb-cli/cli/cloud/aws"
	_ "github.com/hortonworks/cb-cli/cli/cloud/azure"
	_ "github.com/hortonworks/cb-cli/cli/cloud/gcp"
	_ "github.com/hortonworks/cb-cli/cli/cloud/openstack"
	"github.com/hortonworks/cb-cli/cli/utils"
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

	server := c.String(cb.FlServer.Name)
	username := c.String(cb.FlUsername.Name)
	password := c.String(cb.FlPassword.Name)
	output := c.String(cb.FlOutput.Name)
	profile := c.String(cb.FlProfile.Name)
	if len(profile) == 0 {
		profile = "default"
	}

	config, err := cb.ReadConfig(cb.GetHomeDirectory(), profile)
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	if len(output) == 0 {
		c.Set(cb.FlOutput.Name, config.Output)
	}

	if len(server) == 0 || len(username) == 0 || len(password) == 0 {
		if err != nil {
			log.Error(fmt.Sprintf("configuration is not set, see: cb configure --help or provide the following flags: %v",
				[]string{"--" + cb.FlServer.Name, "--" + cb.FlUsername.Name, "--" + cb.FlPassword.Name}))
			os.Exit(1)
		}

		PrintConfig(*config)

		if len(server) == 0 {
			c.Set(cb.FlServer.Name, config.Server)
		}
		if len(username) == 0 {
			c.Set(cb.FlUsername.Name, config.Username)
		}
		if len(password) == 0 {
			c.Set(cb.FlPassword.Name, config.Password)
		}
	}
	return nil
}

func PrintConfig(cfg cb.Config) {
	cfg.Password = "*"
	log.Infof("[ConfigRead] Config read from file, setting as global variable:\n%s", cfg.Yaml())
}

func printFlagCompletion(f cli.Flag) {
	fmt.Printf("--%s\n", f.GetName())
}

func main() {

	app := cli.NewApp()
	app.Name = "cb"
	app.HelpName = "Hortonworks Data Cloud command line tool"
	app.Version = cb.Version + "-" + cb.BuildTime
	app.Author = "Hortonworks"
	app.EnableBashCompletion = true

	app.Flags = []cli.Flag{
		cb.FlDebug,
	}

	app.Before = func(c *cli.Context) error {
		log.SetOutput(os.Stderr)
		log.SetLevel(log.ErrorLevel)
		formatter := &utils.CBFormatter{}
		log.SetFormatter(formatter)
		if c.Bool(cb.FlDebug.Name) {
			log.SetLevel(log.DebugLevel)
		}
		return nil
	}

	cli.AppHelpTemplate = cb.AppHelpTemplate
	cli.HelpPrinter = cb.PrintHelp
	cli.CommandHelpTemplate = cb.CommandHelpTemplate
	cli.SubcommandHelpTemplate = cb.SubCommandHelpTemplate

	app.Commands = []cli.Command{
		{
			Name: "configure",
			Description: fmt.Sprintf("it will save the provided server address and credential "+
				"to %s/%s/%s", cb.GetHomeDirectory(), cb.Config_dir, cb.Config_file),
			Usage:  "configure the server address and credentials used to communicate with this server",
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlServerRequired, cb.FlUsernameRequired, cb.FlPassword, cb.FlProfile).AddOutputFlag().Build(),
			Action: cb.Configure,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlServerRequired, cb.FlUsernameRequired, cb.FlPassword, cb.FlProfile).AddOutputFlag().Build() {
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
							Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlRoleARN).AddAuthenticationFlags().Build(),
							Action: cb.CreateAwsCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlRoleARN).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "key-based",
							Usage:  "creates a new key based aws credential",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlAccessKey, cb.FlSecretKey).AddAuthenticationFlags().Build(),
							Action: cb.CreateAwsCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlAccessKey, cb.FlSecretKey).AddAuthenticationFlags().Build() {
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
							Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlSubscriptionId, cb.FlTenantId, cb.FlAppId, cb.FlAppPassword).AddAuthenticationFlags().Build(),
							Action: cb.CreateAzureCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlSubscriptionId, cb.FlTenantId, cb.FlAppId, cb.FlAppPassword).AddAuthenticationFlags().Build() {
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
					Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlProjectId, cb.FlServiceAccountId, cb.FlServiceAccountPrivateKeyFile).AddAuthenticationFlags().Build(),
					Action: cb.CreateGcpCredential,
					BashComplete: func(c *cli.Context) {
						for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlProjectId, cb.FlServiceAccountId, cb.FlServiceAccountPrivateKeyFile).AddAuthenticationFlags().Build() {
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
							Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlTenantUser, cb.FlTenantPassword, cb.FlTenantName, cb.FlEndpoint, cb.FlFacing).AddAuthenticationFlags().Build(),
							Action: cb.CreateOpenstackCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlTenantUser, cb.FlTenantPassword, cb.FlTenantName, cb.FlEndpoint, cb.FlFacing).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "keystone-v3",
							Usage:  "creates a new keystone version 3 openstack credential",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlTenantUser, cb.FlTenantPassword, cb.FlUserDomain, cb.FlKeystoneScope, cb.FlEndpoint, cb.FlFacing).AddAuthenticationFlags().Build(),
							Action: cb.CreateOpenstackCredential,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlTenantUser, cb.FlTenantPassword, cb.FlUserDomain, cb.FlKeystoneScope, cb.FlEndpoint, cb.FlFacing).AddAuthenticationFlags().Build() {
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
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.DescribeCredential,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-credentials",
			Usage:  "lists the credentials",
			Before: ConfigRead,
			Flags:  cb.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.ListCredentials,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "delete-credential",
			Usage:  "deletes a credential",
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().Build(),
			Before: ConfigRead,
			Action: cb.DeleteCredential,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:  "create-blueprint",
			Usage: "adds a new Ambari blueprint from a file or from a URL",
			Subcommands: []cli.Command{
				{
					Name:   "from-url",
					Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlURL).AddAuthenticationFlags().Build(),
					Before: ConfigRead,
					Action: cb.CreateBlueprintFromUrl,
					Usage:  "creates a blueprint by downloading it from a URL location",
					BashComplete: func(c *cli.Context) {
						for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlURL).AddAuthenticationFlags().Build() {
							printFlagCompletion(f)
						}
					},
				},
				{
					Name:   "from-file",
					Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlFile).AddAuthenticationFlags().Build(),
					Before: ConfigRead,
					Action: cb.CreateBlueprintFromFile,
					Usage:  "creates a blueprint by reading it from a local file",
					BashComplete: func(c *cli.Context) {
						for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlFile).AddAuthenticationFlags().Build() {
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
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.DescribeBlueprint,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-blueprints",
			Usage:  "lists the available blueprints",
			Flags:  cb.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
			Before: ConfigRead,
			Action: cb.ListBlueprints,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "delete-blueprint",
			Usage:  "deletes a blueprint",
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
			Before: ConfigRead,
			Action: cb.DeleteBlueprint,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
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
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateAwsStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-network",
							Usage:  "creates an aws cluster JSON template with existing network",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateAwsStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-subnet",
							Usage:  "creates an aws cluster JSON template with existing subnet",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateAwsStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
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
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateAzureStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-subnet",
							Usage:  "creates an azure cluster JSON template with existing subnet",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateAzureStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
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
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateGcpStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-network",
							Usage:  "creates a gcp cluster JSON template with existing network",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateGcpStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-subnet",
							Usage:  "creates a gcp cluster JSON template with existing subnet",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateGcpStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "legacy-network",
							Usage:  "creates a gcp cluster JSON template with legacy network",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateGcpStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
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
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateOpenstackStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-network",
							Usage:  "creates a openstack cluster JSON template with existing network",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateOpenstackStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
									printFlagCompletion(f)
								}
							},
						},
						{
							Name:   "existing-subnet",
							Usage:  "creates a openstack cluster JSON template with existing subnet",
							Before: ConfigRead,
							Flags:  cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build(),
							Action: cb.GenerateOpenstackStackTemplate,
							BashComplete: func(c *cli.Context) {
								for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlBlueprintName, cb.FlBlueprintFile).AddAuthenticationFlags().Build() {
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
			Flags:     cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlInputJson, cb.FlAmbariPasswordOptional, cb.FlWait).AddAuthenticationFlags().Build(),
			Before:    ConfigRead,
			Action:    cb.CreateStack,
			ArgsUsage: cb.StackTemplateHelp,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlInputJson, cb.FlAmbariPasswordOptional, cb.FlWait).AddAuthenticationFlags().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "describe-cluster",
			Usage:  "describes a cluster",
			Before: ConfigRead,
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.DescribeStack,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "scale-cluster",
			Usage:  "scales a cluster",
			Before: ConfigRead,
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName, cb.FlGroupName, cb.FlDesiredNodeCount).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.ScaleStack,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName, cb.FlGroupName, cb.FlDesiredNodeCount).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "start-cluster",
			Usage:  "starts a cluster",
			Before: ConfigRead,
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.StartStack,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "stop-cluster",
			Usage:  "stops a cluster",
			Before: ConfigRead,
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.StopStack,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "sync-cluster",
			Usage:  "synchronizes a cluster",
			Before: ConfigRead,
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.SyncStack,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "repair-cluster",
			Usage:  "repaires a cluster",
			Before: ConfigRead,
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.RepairStack,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "change-ambari-password",
			Usage:  "changes Ambari password",
			Before: ConfigRead,
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName, cb.FlOldPassword, cb.FlNewPassword, cb.FlAmbariUser).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.ChangeAmbariPassword,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName, cb.FlOldPassword, cb.FlNewPassword, cb.FlAmbariUser).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-clusters",
			Usage:  "lists the running clusters",
			Flags:  cb.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
			Before: ConfigRead,
			Action: cb.ListStacks,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "delete-cluster",
			Usage:  "deletes a cluster",
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName, cb.FlForce).AddAuthenticationFlags().Build(),
			Before: ConfigRead,
			Action: cb.DeleteStack,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName, cb.FlForce).AddAuthenticationFlags().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:  "create-recipe",
			Usage: "adds a new recipe from a file or from a URL",
			Subcommands: []cli.Command{
				{
					Name:   "from-url",
					Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlExecutionType, cb.FlURL).AddAuthenticationFlags().Build(),
					Before: ConfigRead,
					Action: cb.CreateRecipeFromUrl,
					Usage:  "creates a recipe by downloading it from a URL location",
					BashComplete: func(c *cli.Context) {
						for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlExecutionType, cb.FlURL).AddAuthenticationFlags().Build() {
							printFlagCompletion(f)
						}
					},
				},
				{
					Name:   "from-file",
					Flags:  cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlExecutionType, cb.FlFile).AddAuthenticationFlags().Build(),
					Before: ConfigRead,
					Action: cb.CreateRecipeFromFile,
					Usage:  "creates a recipe by reading it from a local file",
					BashComplete: func(c *cli.Context) {
						for _, f := range cb.NewFlagBuilder().AddResourceDefaultFlags().AddFlags(cb.FlExecutionType, cb.FlFile).AddAuthenticationFlags().Build() {
							printFlagCompletion(f)
						}
					},
				},
			},
		},
		{
			Name:   "describe-recipe",
			Usage:  "describes a recipe",
			Before: ConfigRead,
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build(),
			Action: cb.DescribeRecipe,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-recipes",
			Usage:  "lists the available recipes",
			Flags:  cb.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build(),
			Before: ConfigRead,
			Action: cb.ListRecipes,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddAuthenticationFlags().AddOutputFlag().Build() {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "delete-recipe",
			Usage:  "deletes a recipe",
			Flags:  cb.NewFlagBuilder().AddFlags(cb.FlName).AddOutputFlag().AddAuthenticationFlags().Build(),
			Before: ConfigRead,
			Action: cb.DeleteRecipe,
			BashComplete: func(c *cli.Context) {
				for _, f := range cb.NewFlagBuilder().AddFlags(cb.FlName).AddOutputFlag().AddAuthenticationFlags().Build() {
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
			Action: cb.ShowHiddenCommands,
		},
	}...)

	app.Run(os.Args)
}
