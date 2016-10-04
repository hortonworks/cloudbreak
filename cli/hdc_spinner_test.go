package cli

import (
	"fmt"
	"io/ioutil"
	"os"
	"testing"
)

func TestStopSpinner(t *testing.T) {
	originalStdout := os.Stdout
	r, w, _ := os.Pipe()
	os.Stdout = w

	StartSpinner()
	StopSpinner()

	fmt.Print("test message")

	w.Close()
	out, _ := ioutil.ReadAll(r)
	os.Stdout = originalStdout

	captured := string(out)
	if captured != "test message" {
		t.Error("theret are spinner elements in the output")
	}
}
