package cli

import (
	"github.com/urfave/cli"
	"reflect"
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
	FlPasswordRequired = StringFlag{
		RequiredFlag: REQUIRED,
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
	FlWait = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "wait",
			Usage: "wait for the operation to finish",
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
			Usage: "change the number of worker nodes, positive number for add more nodes, e.g: 1, negative for take down nodes, e.g: -1",
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
		if !isRequiredVisible(flag) {
			required = append(required, flag)
		}
	}
	return required
}

func checkRequiredFlags(c *cli.Context, caller interface{}) {
	for _, f := range c.Command.Flags {
		if isRequired(f) && len(c.String(f.GetName())) == 0 {
			logMissingParameterAndExit(c, caller)
		}
	}
}

func isRequiredVisible(flag cli.Flag) bool {
	if flag.GetName() == "help, h" {
		return false
	}
	hidden := flagValue(flag).FieldByName("Hidden").Bool()
	required := flagValue(flag).FieldByName("Required").Bool()
	return !hidden && required
}

func isRequired(flag cli.Flag) bool {
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
