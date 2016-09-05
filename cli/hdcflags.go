package cli

import "github.com/urfave/cli"

var (
	FlDebug = cli.BoolFlag{
		Name:   "debug",
		Usage:  "debug mode",
		EnvVar: "DEBUG",
	}
	FlCBServer = cli.StringFlag{
		Name:   "server",
		Usage:  "server address",
		EnvVar: "CB_SERVER_ADDRESS",
	}
	FlCBUsername = cli.StringFlag{
		Name:   "username",
		Usage:  "user name (e-mail address)",
		EnvVar: "CB_USER_NAME",
	}
	FlCBPassword = cli.StringFlag{
		Name:   "password",
		Usage:  "password",
		EnvVar: "CB_PASSWORD",
	}
	FlCBInputJson = cli.StringFlag{
		Name:  "cli-input-json",
		Usage: "user provided file with json content",
	}
	FlCBClusterName = cli.StringFlag{
		Name:  "cluster-name",
		Usage: "name of a cluster",
	}
	FlCBWait = cli.StringFlag{
		Name:  "wait",
		Usage: "wait for the operation to finish",
	}
	FlCBOutput = cli.StringFlag{
		Name:   "output",
		Usage:  "supported formats: json, yaml, table",
		Value:  "json",
		EnvVar: "CB_OUT_FORMAT",
	}
)
