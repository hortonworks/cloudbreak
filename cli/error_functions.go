package cli

import (
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/urfave/cli"
	"os"
	"reflect"
	"runtime"
	"strings"
)

type RESTError struct {
	Response interface{}
	Code     int
}

func (e *RESTError) Error() string {
	return fmt.Sprintf("(status %d): %+v ", e.Code, e.Response)
}

func logMissingParameterAndExit(c *cli.Context, caller interface{}, message ...string) {
	StopSpinner()
	if len(message) == 0 {
		logErrorMessage(caller, "there are missing parameters\n")
	} else {
		logErrorMessage(caller, message[0])
	}
	cli.ShowSubcommandHelp(c)
	os.Exit(1)
}

func logErrorAndExit(caller interface{}, message string) {
	StopSpinner()
	logErrorMessage(caller, message)
	os.Exit(1)
}

func logErrorMessage(caller interface{}, message string) {
	log.Errorf("[%s] %s", getFunctionName(caller), message)
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
