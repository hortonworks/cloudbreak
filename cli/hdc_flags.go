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
	FlCBServer = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "server",
			Usage:  "server address",
			EnvVar: "CB_SERVER_ADDRESS",
		},
	}
	FlCBServerRequired = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:   "server",
			Usage:  "server address",
			EnvVar: "CB_SERVER_ADDRESS",
		},
	}
	FlCBUsername = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "username",
			Usage:  "user name (e-mail address)",
			EnvVar: "CB_USER_NAME",
		},
	}
	FlCBUsernameRequired = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:   "username",
			Usage:  "user name (e-mail address)",
			EnvVar: "CB_USER_NAME",
		},
	}
	FlCBPassword = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "password",
			Usage:  "password",
			EnvVar: "CB_PASSWORD",
		},
	}
	FlCBPasswordRequired = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:   "password",
			Usage:  "password",
			EnvVar: "CB_PASSWORD",
		},
	}
	FlCBInputJson = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "cli-input-json",
			Usage: "user provided file with json content",
		},
	}
	FlCBClusterName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "cluster-name",
			Usage: "name of a cluster",
		},
	}
	FlCBWait = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:  "wait",
			Usage: "wait for the operation to finish",
		},
	}
	FlCBOutput = StringFlag{
		RequiredFlag: OPTIONAL,
		StringFlag: cli.StringFlag{
			Name:   "output",
			Usage:  "supported formats: json, yaml, table (default: \"json\")",
			EnvVar: "CB_OUT_FORMAT",
		},
	}
	FlCBScalingAdjustment = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name:  "scaling-adjustment",
			Usage: "change the number of worker nodes, positive number for add more nodes, e.g: 1, negative for take down nodes, e.g: -1",
		},
	}
	FlCBCredentialName = StringFlag{
		RequiredFlag: REQUIRED,
		StringFlag: cli.StringFlag{
			Name: "credential-name",
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
		if isRequired(flag) {
			required = append(required, flag)
		}
	}
	return required
}

func OptionalFlags(flags []cli.Flag) []cli.Flag {
	required := []cli.Flag{}
	for _, flag := range flags {
		if !isRequired(flag) {
			required = append(required, flag)
		}
	}
	return required
}

func isRequired(flag cli.Flag) bool {
	if flag.GetName() == "help, h" {
		return false
	}
	hidden := flagValue(flag).FieldByName("Hidden").Bool()
	required := flagValue(flag).FieldByName("Required").Bool()
	return !hidden && required
}

func flagValue(flag cli.Flag) reflect.Value {
	fv := reflect.ValueOf(flag)
	for fv.Kind() == reflect.Ptr {
		fv = reflect.Indirect(fv)
	}
	return fv
}
