package cli

import "github.com/urfave/cli"

func newExitError() error {
	return newExitErrorCode(1)
}

func newExitErrorCode(code int) error {
	return cli.NewExitError("", code)
}
