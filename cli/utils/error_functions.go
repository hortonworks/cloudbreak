package utils

import (
	"fmt"
	"reflect"
	"runtime"
	"strings"

	log "github.com/Sirupsen/logrus"
	"github.com/urfave/cli"
)

type RESTError struct {
	Response interface{}
	Code     int
}

func (e *RESTError) Error() string {
	return fmt.Sprintf("status code: %d, message: %+v ", e.Code, e.Response)
}

func LogMissingParameterMessageAndExit(c *cli.Context, message string) {
	LogMissingParameterAndExit(c, nil, message)
}

func LogMissingParameterAndExit(c *cli.Context, missingFlags []string, message ...string) {
	if len(message) == 0 {
		if missingFlags != nil && len(missingFlags) > 0 {
			LogErrorMessage(fmt.Sprintf("the following parameters are missing: %v\n", strings.Join(missingFlags, ", ")))
		} else {
			LogErrorMessage("there are missing parameters\n")
		}
	} else {
		LogErrorMessage(message[0])
	}
	if err := cli.ShowSubcommandHelp(c); err != nil {
		LogErrorAndExit(err)
	}
	panic("missing")
}

func LogErrorAndExit(err error) {
	LogErrorMessage(err.Error())
	panic(err.Error())
}

func LogErrorMessage(message string) {
	log.Errorf(message)
}

func LogErrorMessageAndExit(message string) {
	log.Errorf(message)
	panic(message)
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
