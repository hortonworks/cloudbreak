package cmd

import (
	"context"
	"errors"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/go-openapi/runtime"
	cr "github.com/go-openapi/runtime/client"
	"github.com/go-openapi/strfmt"
	cf "github.com/hortonworks/cb-cli/dataplane/config"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	u "github.com/hortonworks/cb-cli/dataplane/utils"
	"github.com/hortonworks/dp-cli-common/apikeyauth"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

func init() {
	AppCommands = append(AppCommands, cli.Command{
		Name:   "service-versions",
		Usage:  "Print the version numbers of the service components",
		Before: cf.CheckConfigAndCommandFlagsDP,
		Action: printServiceVersions,
		Flags:  fl.NewFlagBuilder().AddAuthenticationFlagsWithoutWorkspace().AddOutputFlag().Build(),
		BashComplete: func(c *cli.Context) {
			for _, f := range fl.NewFlagBuilder().AddAuthenticationFlagsWithoutWorkspace().AddOutputFlag().Build() {
				fl.PrintFlagCompletion(f)
			}
		},
		Hidden: true,
	})
}

var serviceInfoHeader = []string{"Name", "Version"}

type serviceInfo struct {
	Name    string `json:"name"`
	Version string `json:"version"`
}

func (r *serviceInfo) DataAsStringArray() []string {
	return []string{r.Name, r.Version}
}

type serviceApp struct {
	App *serviceInfo `json:"app"`
	Err *string      `json:"error"`
}

type serviceResult struct {
	Versions []*serviceQueryResult `json:"versions"`
}

type serviceQueryResult struct {
	Response *serviceInfo
	Err      error
}

var servicePathNames = []string{"cb", "as", "environmentservice", "dl", "freeipa", "redbeams"}

func printServiceVersions(c *cli.Context) {
	t := newInfoHTTPTransportFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	results := []*serviceQueryResult{}
	for _, path := range servicePathNames {
		results = append(results, queryServiceInfo(t, path))
	}
	var tableRows []utils.Row
	for _, serviceResult := range results {
		if serviceResult.Err == nil {
			tableRows = append(tableRows, serviceResult.Response)
		}
	}

	output.WriteList(serviceInfoHeader, tableRows)
}

func queryServiceInfo(transport *utils.Transport, servicePath string) *serviceQueryResult {
	r, err := getVersionInfo(transport, servicePath)
	if err != nil {
		return &serviceQueryResult{nil, err}
	}
	if r.Payload.Err != nil {
		return &serviceQueryResult{nil, errors.New(*r.Payload.Err)}
	}
	return &serviceQueryResult{r.Payload.App, nil}
}

func newInfoHTTPTransportFromContext(c *cli.Context) *utils.Transport {
	return newInfoHTTPTransport(c.String(fl.FlServerOptional.Name), "/", c.String(fl.FlApiKeyIDOptional.Name), c.String(fl.FlPrivateKeyOptional.Name))
}

func newInfoHTTPTransport(address, baseAPIPath, apiKeyID, privateKey string) *utils.Transport {
	u.CheckServerAddress(address)
	return apikeyauth.GetAPIKeyAuthTransport(address, baseAPIPath, apiKeyID, privateKey)
}

func getVersionInfo(t *utils.Transport, basePath string) (*getInfo, error) {
	result, err := t.Submit(&runtime.ClientOperation{
		ID:                 "getInfo",
		Method:             "GET",
		PathPattern:        "/" + basePath + "/info",
		ProducesMediaTypes: []string{"application/json"},
		ConsumesMediaTypes: []string{"application/json"},
		Schemes:            []string{"http", "https"},
		Params:             newGetInfoParams(),
		Reader:             &getInfoReader{formats: strfmt.Default},
	})
	if err != nil {
		return nil, err
	}
	return result.(*getInfo), nil
}

type getInfoReader struct {
	formats strfmt.Registry
}

func (o *getInfoReader) ReadResponse(response runtime.ClientResponse, consumer runtime.Consumer) (interface{}, error) {
	switch response.Code() {

	case 200:
		result := newGetInfo()
		if err := result.readResponse(response, consumer, o.formats); err != nil {
			return nil, err
		}
		return result, nil

	default:
		return nil, runtime.NewAPIError("unknown error", response, response.Code())
	}
}

func newGetInfo() *getInfo {
	return &getInfo{}
}

//successful operation
type getInfo struct {
	Payload *serviceApp
}

func (o *getInfo) Error() string {
	return fmt.Sprintf("[GET /info getInfo  %+v", o.Payload)
}

func (o *getInfo) readResponse(response runtime.ClientResponse, consumer runtime.Consumer, formats strfmt.Registry) error {
	o.Payload = new(serviceApp)
	// response payload
	if err := consumer.Consume(response.Body(), o.Payload); err != nil && err != io.EOF {
		return err
	}
	return nil
}

func newGetInfoParams() *getInfoParams {
	return &getInfoParams{
		timeout: cr.DefaultTimeout,
	}
}

type getInfoParams struct {
	timeout    time.Duration
	Context    context.Context
	HTTPClient *http.Client
}

func (o *getInfoParams) WriteToRequest(r runtime.ClientRequest, reg strfmt.Registry) error {
	if err := r.SetTimeout(o.timeout); err != nil {
		return err
	}
	return nil
}
