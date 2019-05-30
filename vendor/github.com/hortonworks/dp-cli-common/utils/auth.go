package utils

import (
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"strings"
	"time"

	"github.com/ernesto-jimenez/httplogger"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/runtime/client"
	log "github.com/sirupsen/logrus"
)

// This is nearly identical with http.DefaultTransport
var TransportConfig = &http.Transport{
	Proxy:           http.ProxyFromEnvironment,
	TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	Dial: (&net.Dialer{
		Timeout:   30 * time.Second,
		KeepAlive: 30 * time.Second,
	}).Dial,
	TLSHandshakeTimeout:   10 * time.Second,
	ExpectContinueTimeout: 1 * time.Second,
}

var LoggedTransportConfig = httplogger.NewLoggedTransport(TransportConfig, newLogger())

type httpLogger struct {
}

func newLogger() *httpLogger {
	return &httpLogger{}
}

func (l *httpLogger) LogRequest(req *http.Request) {
	log.Debugf(
		"Request %s %s",
		req.Method,
		req.URL.String(),
	)
}

func (l *httpLogger) LogResponse(req *http.Request, res *http.Response, err error, duration time.Duration) {
	duration /= time.Millisecond
	if err != nil {
		log.Error(err)
	} else {
		log.Debugf(
			"Response method:%s status:%d duration:%dms req_url:%s",
			req.Method,
			res.StatusCode,
			duration,
			req.URL.String(),
		)
	}
}

var PREFIX_TRIM = []string{"http://", "https://"}

func CutAndTrimAddress(address string) (string, string) {
	for _, v := range PREFIX_TRIM {
		address = strings.TrimPrefix(address, v)
	}
	address = strings.TrimRight(address, "/ ")
	basePath := ""
	slashIndex := strings.Index(address, "/")
	if slashIndex != -1 {
		basePath = address[slashIndex:]
		address = address[0:slashIndex]
	}
	return address, basePath
}

type noContentSafeResponseReader struct {
	OriginalReader runtime.ClientResponseReader
}

type ErrorMessage struct {
	Message         string            `json:"message"`
	ValidationError map[string]string `json:"validationErrors"`
}

func (e *ErrorMessage) String() string {
	var result string = ""
	if len(e.Message) > 0 {
		result = e.Message
	} else if len(e.ValidationError) > 0 {
		var validationErrors []string
		for k, v := range e.ValidationError {
			message := fmt.Sprintf("'%v' - %v", strings.TrimPrefix(k, "postPrivate.arg0."), v)
			validationErrors = append(validationErrors, message)
		}
		result = strings.Join(validationErrors, ",")
	}
	return result
}

func (r *noContentSafeResponseReader) ReadResponse(response runtime.ClientResponse, consumer runtime.Consumer) (interface{}, error) {
	resp, err := r.OriginalReader.ReadResponse(response, consumer)
	if err != nil {
		switch response.Code() {
		case 200:
		case 202:
		case 204:
			return nil, nil
		default:
			body, _ := ioutil.ReadAll(response.Body())
			var errorMessage ErrorMessage
			err := json.Unmarshal(body, &errorMessage)
			if err != nil || (len(errorMessage.Message) == 0 && len(errorMessage.ValidationError) == 0) {
				return nil, &RESTError{Response: string(body), Code: response.Code()}
			}
			return nil, &RESTError{Response: errorMessage.String(), Code: response.Code()}
		}
	}
	return resp, err
}

type Transport struct {
	Runtime *client.Runtime
}

func (t *Transport) Submit(operation *runtime.ClientOperation) (interface{}, error) {
	operation.Reader = &noContentSafeResponseReader{OriginalReader: operation.Reader}
	response, err := t.Runtime.Submit(operation)
	return response, err
}
