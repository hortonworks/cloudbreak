package utils

import (
	"fmt"
	"io/ioutil"
	"math/rand"
	"net/http"
	"net/url"
	"strings"
	"time"

	"strconv"

	log "github.com/Sirupsen/logrus"
)

var r *rand.Rand

const randbytes = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

func init() {
	r = rand.New(rand.NewSource(time.Now().UnixNano()))
}

func RandStr(n int) string {
	b := make([]byte, n)
	for i := range b {
		b[i] = randbytes[rand.Intn(len(randbytes))]
	}
	return string(b)
}

func ConvertToURLAndEncode(urlString string) *url.URL {
	u, err := url.Parse(urlString)
	if err != nil {
		LogErrorAndExit(err)
	}
	u.RawQuery = u.Query().Encode()
	return u
}

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
	defer func() {
		if err = resp.Body.Close(); err != nil {
			LogErrorAndExit(err)
		}
	}()
	if resp.StatusCode != 200 {
		LogErrorMessageAndExit(fmt.Sprintf("Couldn't download content from URL, response code is %d, expected: 200.", resp.StatusCode))
	}
	content, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		LogErrorAndExit(err)
	}
	return content
}

// Returns a slice of strings. If the string is empty it will return an empty slice.
func DelimitedStringToArray(text, delimiter string) []string {
	if len(text) == 0 {
		return make([]string, 0)
	}
	return strings.Split(text, delimiter)
}

func CopyToByTargets(sourceParameters map[string]interface{}, parameters ...string) map[string]interface{} {
	destinationParameters := map[string]interface{}{}
	for _, actual := range parameters {
		destinationParameters[actual] = sourceParameters[actual]
	}
	return destinationParameters
}

func SafeStringTypeAssert(value interface{}) string {
	strValue, ok := value.(string)
	if ok {
		return strValue
	}
	return ""
}

func FloatToString(float float64) string {
	return strconv.FormatFloat(float, 'f', -1, 64)
}
