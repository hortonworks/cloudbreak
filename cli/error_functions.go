package cli

import (
	"fmt"
	"os"
	"reflect"
	"runtime"
	"strings"

	log "github.com/Sirupsen/logrus"
	"github.com/urfave/cli"
)

var exit func(code int) = os.Exit

type RESTError struct {
	Response interface{}
	Code     int
}

func (e *RESTError) Error() string {
	return fmt.Sprintf("status code: %d, message: %+v ", e.Code, e.Response)
}

func logMissingParameterMessageAndExit(c *cli.Context, message string) {
	logMissingParameterAndExit(c, nil, message)
}

func logMissingParameterAndExit(c *cli.Context, missingFlags []string, message ...string) {
	StopSpinner()
	if len(message) == 0 {
		if missingFlags != nil && len(missingFlags) > 0 {
			logErrorMessage(fmt.Sprintf("the following parameters are missing: %v\n", strings.Join(missingFlags, ", ")))
		} else {
			logErrorMessage("there are missing parameters\n")
		}
	} else {
		logErrorMessage(message[0])
	}
	cli.ShowSubcommandHelp(c)
	exit(1)
}

func logErrorAndExit(err error) {
	StopSpinner()
	logErrorMessage(err.Error())
	exit(1)
}

func logErrorMessage(message string) {
	log.Errorf(message)
}

func getFunctionName(caller interface{}) string {
	longFunctionName := runtime.FuncForPC(reflect.ValueOf(caller).Pointer()).Name()
	shortFunctionName := longFunctionName[strings.LastIndex(longFunctionName, ".")+1:]
	var endIndex int
	hyphenIndex := strings.Index(shortFunctionName, "-")
	if hyphenIndex == -1 {
		endIndex = len(shortFunctionName)
	} else {
		endIndex = hyphenIndex
	}
	return shortFunctionName[0:endIndex]
}
