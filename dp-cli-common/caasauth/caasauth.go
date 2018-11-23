package caasauth

import (
	"bufio"
	"bytes"
	"crypto/tls"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/ernesto-jimenez/httplogger"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/runtime/client"
	"github.com/hortonworks/cb-cli/dp-cli-common/utils"
)

var PREFIX_TRIM = []string{"http://", "https://"}

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

type tokenRequest struct {
	GrantType string `json:"grant_type"`
	Code      string `json:"code"`
	ClientId  string `json:"client_id"`
}

type refreshRequest struct {
	GrantType    string `json:"grant_type"`
	RefreshToken string `json:"refresh_token"`
	ClientId     string `json:"client_id"`
}

func newTokenRequest(code string) *tokenRequest {
	return &tokenRequest{
		GrantType: "authorization_code",
		Code:      code,
		ClientId:  "6eda2bf3-95ce-499b-8e27-1c19c93bae12",
	}
}

func newRefreshTokenRequest(refresh string) *refreshRequest {
	return &refreshRequest{
		GrantType:    "refresh_token",
		RefreshToken: refresh,
		ClientId:     "6eda2bf3-95ce-499b-8e27-1c19c93bae12",
	}
}

type TokenResponse struct {
	AccessToken  string `json:"access_token"`
	TokenType    string `json:"token_type"`
	ExpiresIn    string `json:"expires_in"`
	RefreshToken string `json:"refresh_token"`
}

func RefreshAccessToken(address, baseApiPath, refreshToken string) (*Transport, *TokenResponse) {
	address, basePath := cutAndTrimAddress(address)
	tokenReq := newRefreshTokenRequest(refreshToken)

	tokens, err := getCaasToken("https://"+address+basePath+"/oidc/token", tokenReq)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	cbTransport := &Transport{client.New(address, basePath+baseApiPath, []string{"https"})}
	cbTransport.Runtime.DefaultAuthentication = client.BearerToken(tokens.AccessToken)
	cbTransport.Runtime.Transport = LoggedTransportConfig
	return cbTransport, tokens
}

func NewRefreshToken(address string) string {
	_, tokens := NewCaasTransport(address, "")
	return tokens.RefreshToken
}

func NewCaasTransport(address, baseApiPath string) (*Transport, *TokenResponse) {
	address, basePath := cutAndTrimAddress(address)

	caasPath := fmt.Sprintf("https://%[1]s/oidc/authorize?scope=openid dps offline_access&response_type=code&client_id=6eda2bf3-95ce-499b-8e27-1c19c93bae12&redirect_uri=https://%[1]s/caas/cli&state=random-state&nonce=random-nonce", address+basePath)
	reader := bufio.NewReader(os.Stdin)
	printLink(utils.ConvertToURLAndEncode(caasPath))
	deviceCode, _ := reader.ReadString('\n')
	deviceCode = strings.TrimSuffix(deviceCode, "\n")
	tokenReq := newTokenRequest(deviceCode)

	cbTransport := &Transport{client.New(address, basePath+baseApiPath, []string{"https"})}

	tokens, err := getCaasToken("https://"+address+basePath+"/oidc/token", tokenReq)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	cbTransport.Runtime.DefaultAuthentication = client.BearerToken(tokens.AccessToken)
	cbTransport.Runtime.Transport = LoggedTransportConfig
	return cbTransport, tokens
}

func printLink(url *url.URL) {
	fmt.Println()
	fmt.Println(url)
	fmt.Println()
	fmt.Print("Enter security code: ")
}

func getCaasToken(identityUrl string, tokenReq interface{}) (*TokenResponse, error) {
	reqBody, _ := json.Marshal(tokenReq)
	req, err := http.NewRequest("POST", identityUrl, bytes.NewReader(reqBody))
	if err != nil {
		return nil, err
	}
	req.Header.Add("Content-Type", "application/json")

	c := &http.Client{
		Transport: LoggedTransportConfig,
	}

	resp, err := c.Do(req)

	if resp == nil && err == nil {
		return nil, errors.New(fmt.Sprintf("Unknown error while connnecting to %s", identityUrl))
	}

	if resp == nil || resp.StatusCode >= 400 {
		if err != nil {
			return nil, err
		}
		return nil, errors.New(fmt.Sprintf("Error while connnecting to %s, please check your username and password or use flags for each command. (%s)", identityUrl, resp.Status))
	}

	var tokenResp TokenResponse
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal(body, &tokenResp)
	if err != nil {
		return nil, err
	}

	return &tokenResp, nil
}

func cutAndTrimAddress(address string) (string, string) {
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

type Transport struct {
	Runtime *client.Runtime
}

func (t *Transport) Submit(operation *runtime.ClientOperation) (interface{}, error) {
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
				return nil, &utils.RESTError{Response: string(body), Code: response.Code()}
			}
			return nil, &utils.RESTError{Response: errorMessage.String(), Code: response.Code()}
		}
	}
	return resp, err
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
