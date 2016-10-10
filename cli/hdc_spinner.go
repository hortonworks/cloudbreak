package cli

import (
	"os"
	"time"

	"fmt"
	"github.com/briandowns/spinner"
	"runtime"
)

var Spinner *spinner.Spinner

func init() {
	if runtime.GOOS != "windows" {
		Spinner = spinner.New(spinner.CharSets[9], 200*time.Millisecond)
		Spinner.Writer = os.Stderr
	}
}

func StartSpinner() {
	if Spinner != nil {
		Spinner.Start()
	}
}

func StopSpinner() {
	if Spinner != nil {
		Spinner.Stop()
		fmt.Fprint(os.Stderr, "\r")
	}
}
