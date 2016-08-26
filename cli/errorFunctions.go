package cli

import (
	"github.com/urfave/cli"
	"os"
)

func newExitReturnError() {
	os.Exit(1)
}

func newExitError() error {
	return newExitErrorCode(1)
}

func newExitErrorCode(code int) error {
	return cli.NewExitError("", code)
}
