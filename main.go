package main

import (
	"fmt"
	"os"

	log "github.com/Sirupsen/logrus"
	hdc "github.com/hortonworks/hdc-cli/cli"
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
		formatter := &hdc.CBFormatter{}
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
			Name:  "add-autoscaling-policy",
			Usage: "add a new autoscaling policy to the cluster",
			Flags: []cli.Flag{hdc.FlClusterName, hdc.FlPolicyName, hdc.FlScalingAdjustment, hdc.FlScalingDefinition, hdc.FlOperator, hdc.FlThreshold,
				hdc.FlPeriod, hdc.FlNodeType, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.AddAutoscalingPolicy,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlPolicyName, hdc.FlScalingAdjustment, hdc.FlScalingDefinition, hdc.FlOperator, hdc.FlThreshold,
					hdc.FlPeriod, hdc.FlNodeType, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name: "configure",
			Description: fmt.Sprintf("it will save the provided server address and credential "+
				"to %s/%s/%s", hdc.GetHomeDirectory(), hdc.Hdc_dir, hdc.Config_file),
			Usage:  "configure the server address and credentials used to communicate with this server",
			Flags:  []cli.Flag{hdc.FlServerRequired, hdc.FlUsernameRequired, hdc.FlPassword, hdc.FlOutput},
			Action: hdc.Configure,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlServerRequired, hdc.FlUsernameRequired, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "configure-autoscaling",
			Usage:  "configure autoscaling (cooldown time, min/max cluster size)",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlCooldownTime, hdc.FlClusterMinSize, hdc.FlClusterMaxSize, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Action: hdc.ConfigureAutoscaling,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlCooldownTime, hdc.FlClusterMinSize,
					hdc.FlClusterMaxSize, hdc.FlServer, hdc.FlUsername, hdc.FlPassword} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "create-cluster",
			Usage:  "creates a new cluster",
			Flags:  []cli.Flag{hdc.FlInputJson, hdc.FlWait, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlAmbariPasswordOptional},
			Before: ConfigRead,
			Action: hdc.CreateCluster,
			BashComplete: func(c *cli.Context) {
				fmt.Println("generate-cli-skeleton")
				fmt.Println("validate-cli-skeleton")
				for _, f := range []cli.Flag{hdc.FlInputJson, hdc.FlWait, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlAmbariPasswordOptional} {
					printFlagCompletion(f)
				}
			},
			Subcommands: []cli.Command{
				{
					Name:      "generate-cli-skeleton",
					Action:    hdc.GenerateCreateClusterSkeleton,
					ArgsUsage: hdc.AWSCreateClusterSkeletonHelp,
					Usage:     "generate the cluster creation template",
				},
				{
					Name:   "validate-cli-skeleton",
					Flags:  []cli.Flag{hdc.FlInputJson, hdc.FlAmbariPasswordOptional},
					Action: hdc.ValidateCreateClusterSkeleton,
					Usage:  "validate the input json",
					BashComplete: func(c *cli.Context) {
						for _, f := range []cli.Flag{hdc.FlInputJson, hdc.FlAmbariPasswordOptional} {
							printFlagCompletion(f)
						}
					},
				},
			},
		},
		{
			Name:   "describe-cluster",
			Usage:  "get a detailed description of a cluster",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.DescribeCluster,
			BashComplete: func(c *cli.Context) {
				fmt.Println("instances")
				for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
			Subcommands: []cli.Command{
				{
					Name:   "instances",
					Usage:  "list the available instances in the cluster",
					Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
					Before: ConfigRead,
					Action: hdc.ListClusterNodes,
					BashComplete: func(c *cli.Context) {
						for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
							printFlagCompletion(f)
						}
					},
				},
			},
		},
		{
			Name:   "disable-autoscaling",
			Usage:  "disable autoscaling on a cluster",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Action: hdc.DisableAutoscalingPolicy,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "enable-autoscaling",
			Usage:  "enable autoscaling on a cluster",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Action: hdc.EnableAutoscalingPolicy,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-autoscaling-definitions",
			Usage:  "list the available autoscaling definitions",
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.ListDefinitions,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-cluster-types",
			Usage:  "list the available cluster types and HDP versions",
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.ListBlueprints,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-clusters",
			Usage:  "list the available clusters",
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.ListClusters,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-ldaps",
			Usage:  "list the available ldaps",
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.ListLdaps,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "list-metastores",
			Usage:  "list the available metastores",
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.ListRDSConfigs,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:  "register-ldap",
			Usage: "register a new LDAP",
			Flags: []cli.Flag{hdc.FlLdapName, hdc.FlLdapServer, hdc.FlLdapDomain, hdc.FlLdapBindDN, hdc.FlLdapBindPassword,
				hdc.FlLdapUserSearchBase, hdc.FlLdapUserSearchFilter, hdc.FlLdapUserSearchAttribute, hdc.FlLdapGroupSearchBase,
				hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Action: hdc.CreateLDAP,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlLdapName, hdc.FlLdapServer, hdc.FlLdapDomain, hdc.FlLdapBindDN, hdc.FlLdapBindPassword,
					hdc.FlLdapUserSearchBase, hdc.FlLdapUserSearchFilter, hdc.FlLdapUserSearchAttribute, hdc.FlLdapGroupSearchBase,
					hdc.FlServer, hdc.FlUsername, hdc.FlPassword} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:  "register-metastore",
			Usage: "register a new Hive or Druid metastore",
			Flags: []cli.Flag{hdc.FlRdsName, hdc.FlRdsUsername, hdc.FlRdsPassword, hdc.FlRdsUrl, hdc.FlRdsType, hdc.FlRdsDbType, hdc.FlHdpVersion,
				hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Action: hdc.CreateRDSConfig,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlRdsName, hdc.FlRdsUsername, hdc.FlRdsPassword, hdc.FlRdsUrl, hdc.FlRdsType, hdc.FlRdsDbType,
					hdc.FlHdpVersion, hdc.FlServer, hdc.FlUsername, hdc.FlPassword} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "remove-autoscaling-policy",
			Usage:  "remove an existing autoscale policy",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlPolicyName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Action: hdc.RemoveAutoscalingPolicy,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlPolicyName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "remove-ldap",
			Usage:  "remove an LDAP",
			Flags:  []cli.Flag{hdc.FlLdapName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Action: hdc.DeleteLdap,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlLdapName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "remove-metastore",
			Usage:  "remove a metastore",
			Flags:  []cli.Flag{hdc.FlRdsName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Action: hdc.DeleteRDSConfig,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlRdsName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "repair-cluster",
			Usage:  "remove or replace the faulty nodes",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlNodeType, hdc.FlRemoveOnly, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.RepairCluster,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlNodeType, hdc.FlRemoveOnly, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "resize-cluster",
			Usage:  "change the number of worker or compute nodes of an existing cluster",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlNodeType, hdc.FlScalingAdjustment, hdc.FlWait, hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Before: ConfigRead,
			Action: hdc.ResizeCluster,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlNodeType, hdc.FlScalingAdjustment, hdc.FlWait,
					hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput} {
					printFlagCompletion(f)
				}
			},
		},
		{
			Name:   "terminate-cluster",
			Usage:  "terminates a cluster",
			Flags:  []cli.Flag{hdc.FlClusterName, hdc.FlWait, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Action: hdc.TerminateCluster,
			BashComplete: func(c *cli.Context) {
				for _, f := range []cli.Flag{hdc.FlClusterName, hdc.FlWait, hdc.FlServer, hdc.FlUsername, hdc.FlPassword} {
					printFlagCompletion(f)
				}
			},
		},
	}

	// hidden commands
	app.Commands = append(app.Commands, []cli.Command{
		{
			Name:   "hidden",
			Usage:  "shows the hidden commands",
			Hidden: true,
			Action: hdc.ShowHiddenCommands,
		},
		{
			Name:   "cleanup",
			Usage:  "remove the unused resources",
			Before: ConfigRead,
			Hidden: true,
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Action: hdc.CleanupResources,
		},
		{
			Name:  "create-credential",
			Usage: "create a new credential",
			Flags: []cli.Flag{hdc.FlCredentialName, hdc.FlRoleARN, hdc.FlSSHKeyURL, hdc.FlSSHKeyPair,
				hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Hidden: true,
			Action: hdc.CreateCredential,
		},
		{
			Name:  "create-network",
			Usage: "create a new network",
			Flags: []cli.Flag{hdc.FlNetworkName, hdc.FlSubnet, hdc.FlVPC, hdc.FlIGW,
				hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Hidden: true,
			Action: hdc.CreateNetworkCommand,
		},
		{
			Name:   "delete-credential",
			Usage:  "delete a credential",
			Flags:  []cli.Flag{hdc.FlCredentialName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Hidden: true,
			Action: hdc.DeleteCredential,
		},
		{
			Name:   "delete-network",
			Usage:  "delete a network",
			Flags:  []cli.Flag{hdc.FlNetworkName, hdc.FlServer, hdc.FlUsername, hdc.FlPassword},
			Before: ConfigRead,
			Hidden: true,
			Action: hdc.DeleteNetwork,
		},
		{
			Name:   "list-credentials",
			Usage:  "list the credentials",
			Before: ConfigRead,
			Hidden: true,
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Action: hdc.ListPrivateCredentials,
		},
		{
			Name:   "list-networks",
			Usage:  "list the networks",
			Before: ConfigRead,
			Hidden: true,
			Flags:  []cli.Flag{hdc.FlServer, hdc.FlUsername, hdc.FlPassword, hdc.FlOutput},
			Action: hdc.ListPrivateNetworks,
		},
	}...)

	app.Run(os.Args)
}
