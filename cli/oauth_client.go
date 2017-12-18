package cli

import (
	"crypto/tls"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"net/url"
	"regexp"
	"strings"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/ernesto-jimenez/httplogger"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/runtime/client"
	"github.com/go-openapi/strfmt"
	"github.com/hortonworks/cb-cli/cli/utils"
	apiclient "github.com/hortonworks/cb-cli/client_cloudbreak"
	"github.com/urfave/cli"
)

const (
	OAUTH2 = "oauth2"
	BASIC  = "basic"
)

var PREFIX_TRIM = []string{"http://", "https://"}

type Cloudbreak struct {
	Cloudbreak *apiclient.Cloudbreak
}

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

func NewCloudbreakHTTPClientFromContext(c *cli.Context) *Cloudbreak {
	return NewCloudbreakHTTPClient(c.String(FlServerOptional.Name), c.String(FlUsername.Name), c.String(FlPassword.Name), c.String(FlAuthTypeOptional.Name))
}

func NewCloudbreakHTTPClient(address, username, password, authType string) *Cloudbreak {
	for _, v := range PREFIX_TRIM {
		address = strings.TrimPrefix(address, v)
	}
	basePath := "/cb/api"
	slashIndex := strings.Index(address, "/")
	if slashIndex != -1 {
		basePath = address[slashIndex:] + "/cb/api"
		address = address[0:slashIndex]
	}

	cbTransport := &transport{client.New(address, basePath, []string{"https"})}

	switch authType {
	case OAUTH2:
		token, err := getOAuth2Token("https://"+address+"/identity/oauth/authorize", username, password, "cloudbreak_shell")
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		cbTransport.Runtime.DefaultAuthentication = client.BearerToken(token)
		break
	case BASIC:
		cbTransport.Runtime.DefaultAuthentication = client.BasicAuth(username, password)
		break
	default:
		utils.LogErrorAndExit(errors.New(fmt.Sprintf("invalid authentication type, accepted values: [%s, %s]", OAUTH2, BASIC)))
	}

	cbTransport.Runtime.Transport = LoggedTransportConfig
	return &Cloudbreak{Cloudbreak: apiclient.New(cbTransport, strfmt.Default)}
}

func getOAuth2Token(identityUrl string, username string, password string, clientId string) (string, error) {
	form := url.Values{"credentials": {fmt.Sprintf(`{"username":"%s","password":"%s"}`, username, utils.EscapeStringToJson(password))}}
	req, err := http.NewRequest("POST", fmt.Sprintf("%s?response_type=token&client_id=%s", identityUrl, clientId), strings.NewReader(form.Encode()))
	if err != nil {
		return "", err
	}
	req.Header.Add("Accept", "application/x-www-form-urlencoded")
	req.Header.Add("Content-Type", "application/x-www-form-urlencoded")

	c := &http.Client{
		Transport: LoggedTransportConfig,
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			return errors.New("Don't redirect!")
		},
	}

	resp, err := c.Do(req)

	if resp == nil && err == nil {
		return "", errors.New(fmt.Sprintf("Unkown error while connnecting to %s as user: %s", identityUrl, username))
	}

	if resp == nil || resp.StatusCode >= 400 {
		if err != nil {
			return "", err
		}
		return "", errors.New(fmt.Sprintf("Error while connnecting to %s as user: %s, please check your username and password in %s or use flags for each command. (%s)", identityUrl, username, Config_dir+"/"+Config_file, resp.Status))
	}

	location := resp.Header.Get("Location")
	reg := regexp.MustCompile("access_token=(.*)&expires_in")
	tokenBytes := reg.Find([]byte(location))
	tokenString := string(tokenBytes)
	token := tokenString[13 : len(tokenString)-11]
	return token, nil
}

type transport struct {
	Runtime *client.Runtime
}

func (t *transport) Submit(operation *runtime.ClientOperation) (interface{}, error) {
	operation.Reader = &noContentSafeResponseReader{OriginalReader: operation.Reader}
	response, err := t.Runtime.Submit(operation)
	return response, err
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
		for _, v := range e.ValidationError {
			validationErrors = append(validationErrors, v)
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
				return nil, &utils.RESTError{Response: string(body), Code: response.Code()}
			}
			return nil, &utils.RESTError{Response: errorMessage.String(), Code: response.Code()}
		}
	}
	return resp, nil
}

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
