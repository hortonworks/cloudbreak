package cli

import (
	"fmt"
	"os"
)

type RESTError struct {
	Response interface{}
	Code     int
}

func (e *RESTError) Error() string {
	return fmt.Sprintf("(status %d): %+v ", e.Code, e.Response)
}

func newExitReturnError() {
	os.Exit(1)
}
