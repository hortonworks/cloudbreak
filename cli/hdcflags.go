package cli

import "github.com/urfave/cli"

var (
	FlCBServer = cli.StringFlag{
		Name:   "server",
		Usage:  "server address",
		EnvVar: "CB_SERVER_ADDRESS",
		Value:  "192.168.99.100",
	}
	FlCBUsername = cli.StringFlag{
		Name:   "username",
		Usage:  "user name (e-mail address)",
		EnvVar: "CB_USER_NAME",
		Value:  "admin@example.com",
	}
	FlCBPassword = cli.StringFlag{
		Name:   "password",
		Usage:  "password",
		EnvVar: "CB_PASSWORD",
		Value:  "cloudbreak",
	}
)
