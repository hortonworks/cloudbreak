package utils

import (
	"io/ioutil"
	"strings"
)

func SafeInt32Convert(value *int32) int32 {
	if value == nil {
		return 0
	}
	return *value
}

func SafeStringConvert(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}

func EscapeStringToJson(input string) string {
	return strings.Replace(strings.Replace(input, "\\", "\\\\", -1), "\"", "\\\"", -1)
}

func ReadFile(fileLocation string) []byte {
	bp, err := ioutil.ReadFile(fileLocation)
	if err != nil {
		LogErrorAndExit(err)
	}
	return bp
}
