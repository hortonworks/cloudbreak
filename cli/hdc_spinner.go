package cli

import (
	"fmt"
	"github.com/briandowns/spinner"
	"time"
)

var Spinner *spinner.Spinner

func init() {
	spinner.CharSets[37] = []string{"H", "o", "r", "t", "o", "n", "w", "o", "r", "k", "s"}
	Spinner = spinner.New(spinner.CharSets[9], 200*time.Millisecond)
}

func StartSpinner() {
	if Spinner != nil {
		Spinner.Start()
	}
}

func StopSpinner() {
	if Spinner != nil {
		Spinner.Stop()
		fmt.Print("\r")
	}
}
