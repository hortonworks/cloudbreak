package utils

import (
	"io/ioutil"
	"strings"
	"net/http"
	"fmt"

	log "github.com/Sirupsen/logrus"
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
	log.Infof("[readFile] read content from file: %s", fileLocation)
	content, err := ioutil.ReadFile(fileLocation)
	if err != nil {
		LogErrorAndExit(err)
	}
	return content
}

func ReadContentFromURL(urlLocation string, client *http.Client) []byte {
	log.Infof("[readFile] read content from URL: %s", urlLocation)
	resp, err := client.Get(urlLocation)
	if err != nil {
		LogErrorAndExit(err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		LogErrorMessageAndExit(fmt.Sprintf("Couldn't download content from URL, response code is %d, expected: 200.", resp.StatusCode))
	}
	content, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		LogErrorAndExit(err)
	}
	return content
}
