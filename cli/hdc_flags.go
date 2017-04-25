package cli

import (
	"reflect"
	"strconv"

	"github.com/urfave/cli"
)

var REQUIRED RequiredFlag = RequiredFlag{true}
var OPTIONAL RequiredFlag = RequiredFlag{false}

var (
	FlDebug = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:   "debug",
			Usage:  "debug mode",
			EnvVar: "DEBUG",
		},
	}
	FlServer = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "server",
			Usage:  "server address",
			EnvVar: "CB_SERVER_ADDRESS",
		},
	}
	FlServerRequired = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:   "server",
			Usage:  "server address",
			EnvVar: "CB_SERVER_ADDRESS",
		},
	}
	FlUsername = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "username",
			Usage:  "user name (e-mail address)",
			EnvVar: "CB_USER_NAME",
		},
	}
	FlUsernameRequired = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:   "username",
			Usage:  "user name (e-mail address)",
			EnvVar: "CB_USER_NAME",
		},
	}
	FlPassword = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "password",
			Usage:  "password",
			EnvVar: "CB_PASSWORD",
		},
	}
	FlInputJson = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "cli-input-json",
			Usage: "user provided file with json content",
		},
	}
	FlClusterName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "cluster-name",
			Usage: "name of a cluster",
		},
	}
	FlClusterNameOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "cluster-name",
			Usage: "name of a cluster",
		},
	}
	FlAmbariPasswordOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "input-json-param-ClusterAndAmbariPassword",
			Usage: "password of the cluster and ambari",
		},
	}
	FlClusterNameParamOptional = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "input-json-param-ClusterName",
			Usage: "name of the cluster",
		},
	}
	FlWait = BoolFlag{
		RequiredFlag: OPTIONAL,
		BoolFlag: cli.BoolFlag{
			Name:  "wait",
			Usage: "wait for the operation to finish, no argument required",
		},
	}
	FlRemoveOnly = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "remove-only",
			Usage: "set to true if you want the instances to be removed only, otherwise they will be replaced",
		},
	}
	FlOutput = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "output",
			Usage:  "supported formats: json, yaml, table (default: \"json\")",
			EnvVar: "CB_OUT_FORMAT",
		},
	}
	FlScalingAdjustment = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "scaling-adjustment",
			Usage: "change the number of worker or compute nodes, positive number for add more nodes, e.g: 1, negative for take down nodes, e.g: -1",
		},
	}
	FlNodeType = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "node-type",
			Usage: "type of the nodes. [worker, compute]",
		},
	}
	FlCredentialName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "credential-name",
		},
	}
	FlRoleARN = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "role-arn",
		},
	}
	FlSSHKeyURL = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "ssh-key-url",
		},
	}
	FlSSHKeyPair = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "existing-ssh-key-pair",
		},
	}
	FlClusterType = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "cluster-type",
			Usage: "type of the cluster",
		},
	}
	FlNetworkName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "network-name",
		},
	}
	FlSubnet = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "subnet-cidr",
		},
	}
	FlVPC = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "vpc",
		},
	}
	FlIGW = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "igw",
		},
	}
	FlRdsName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "rds-name",
			Usage: "name of the RDS",
		},
	}
	FlRdsUsername = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "rds-username",
			Usage: "username of the RDS",
		},
	}
	FlRdsPassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "rds-password",
			Usage: "password of the RDS",
		},
	}
	FlRdsUrl = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "rds-url",
			Usage: "URL of the RDS, <Endpoint>/<DB Name>",
		},
	}
	FlRdsType = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "rds-type",
			Usage: "type of the RDS (HIVE/DRUID)",
		},
	}
	FlRdsDbType = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "rds-database-type",
			Usage: "database type of the RDS",
			Value: POSTGRES,
		},
	}
	FlHdpVersion = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "hdp-version",
			Usage: "HDP version",
			Value: strconv.FormatFloat(SUPPORTED_HDP_VERSIONS[0], 'f', 6, 64)[0:3],
		},
	}
	FlPolicyName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "policy-name",
			Usage: "name of the autoscaling policy",
		},
	}
	FlScalingDefinition = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "scaling-definition",
			Usage: "name of the autoscaling definition",
		},
	}
	FlOperator = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "operator",
			Usage: "operater ('>', '<')",
		},
	}
	FlThreshold = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "threshold",
			Usage: "threshold value",
		},
	}
	FlPeriod = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "period",
			Usage: "period in minutes",
		},
	}
	FlCooldownTime = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "cooldown-time",
			Usage: "cooldown time in minutes",
		},
	}
	FlClusterMinSize = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "min-cluster-size",
			Usage: "minimum size of the cluster",
		},
	}
	FlClusterMaxSize = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "max-cluster-size",
			Usage: "maximum size of the cluster",
		},
	}
	FlLdapName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-name",
			Usage: "name of the ldap",
		},
	}
	FlLdapServer = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-server",
			Usage: "address of the ldap server (e.g: ldap://10.0.0.1:384)",
		},
	}
	FlLdapDomain = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-domain",
			Usage: "ldap domain (e.g: ad.hdc.com)",
		},
	}
	FlLdapBindDN = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-bind-dn",
			Usage: "ldap bind dn (e.g: CN=Administrator,CN=Users,DC=ad,DC=hdc,DC=com)",
		},
	}
	FlLdapBindPassword = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-bind-password",
			Usage: "ldap bind password",
		},
	}
	FlLdapUserSearchBase = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-search-base",
			Usage: "ldap user search base (e.g: CN=Users,DC=ad,DC=hdc,DC=com)",
		},
	}
	FlLdapUserSearchFilter = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-search-filter",
			Usage: "ldap user search filter (e.g: CN)",
		},
	}
	FlLdapUserSearchAttribute = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-user-search-attribute",
			Usage: "ldap user search attribute",
		},
	}
	FlLdapGroupSearchBase = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "ldap-group-search-base",
			Usage: "ldap group search base (e.g: OU=scopes,DC=ad,DC=hdc,DC=com)",
		},
	}
	FlSmartSenseSubscription = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "subscription",
			Usage: "SmartSense subscription id",
		},
	}
	FlSmartSenseSubscriptionID = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "subscription-id",
			Usage: "id of the SmartSense subscription object in Cloudbreak",
		},
	}
	FlFlexSubscription = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "subscription",
			Usage: "id of the Flex subscription object in Cloudbreak",
		},
	}
	FlFlexSubscriptionName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "subscription-name",
			Usage: "name of the Flex stored subscription",
		},
	}
)

type RequiredFlag struct {
	Required bool
}

type StringFlag struct {
	cli.StringFlag
	RequiredFlag
}

type BoolFlag struct {
	cli.BoolFlag
	RequiredFlag
}

func RequiredFlags(flags []cli.Flag) []cli.Flag {
	required := []cli.Flag{}
	for _, flag := range flags {
		if isRequiredVisible(flag) {
			required = append(required, flag)
		}
	}
	return required
}

func OptionalFlags(flags []cli.Flag) []cli.Flag {
	required := []cli.Flag{}
	for _, flag := range flags {
		if flag.GetName() != "generate-bash-completion" {
			if !isRequiredVisible(flag) {
				required = append(required, flag)
			}
		}
	}
	return required
}

func checkRequiredFlags(c *cli.Context) {
	missingFlags := make([]string, 0)
	for _, f := range c.Command.Flags {
		if isRequired(f) && len(c.String(f.GetName())) == 0 {
			missingFlags = append(missingFlags, f.GetName())
		}
	}
	if len(missingFlags) > 0 {
		logMissingParameterAndExit(c, missingFlags)
	}
}

func isRequiredVisible(flag cli.Flag) bool {
	if flag.GetName() == "help, h" || flag.GetName() == "generate-bash-completion" {
		return false
	}
	hidden := flagValue(flag).FieldByName("Hidden").Bool()
	required := flagValue(flag).FieldByName("Required").Bool()
	return !hidden && required
}

func isRequired(flag cli.Flag) (required bool) {
	defer func() {
		if r := recover(); r != nil {
			required = false
		}
	}()
	if flag.GetName() == "help, h" {
		return false
	}
	return flagValue(flag).FieldByName("Required").Bool()
}

func flagValue(flag cli.Flag) reflect.Value {
	fv := reflect.ValueOf(flag)
	for fv.Kind() == reflect.Ptr {
		fv = reflect.Indirect(fv)
	}
	return fv
}
