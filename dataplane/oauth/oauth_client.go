package oauth

import (
	"crypto/tls"
	"net"
	"net/http"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/ernesto-jimenez/httplogger"
	"github.com/go-openapi/strfmt"
	apiclient "github.com/hortonworks/cb-cli/dataplane/api/client"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	authapiclient "github.com/hortonworks/cb-cli/dataplane/oauthapi/client"
	u "github.com/hortonworks/cb-cli/dataplane/utils"
	"github.com/hortonworks/dp-cli-common/caasauth"
	"github.com/urfave/cli"
)

var PREFIX_TRIM = []string{"http://", "https://"}

type Cloudbreak struct {
	Cloudbreak *apiclient.Cloudbreak
}
type Dataplane struct {
	Dataplane *authapiclient.Dataplane
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
	return NewCloudbreakHTTPClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlRefreshTokenOptional.Name))
}

func NewCloudbreakHTTPClient(address string, refreshToken string) *Cloudbreak {
	u.CheckServerAddress(address)
	var transport *caasauth.Transport
	baseAPIPath := "/cb/api"
	if len(refreshToken) == 0 {
		transport, _ = caasauth.NewCaasTransport(address, baseAPIPath)
	} else {
		transport, _ = caasauth.RefreshAccessToken(address, baseAPIPath, refreshToken)
	}
	return &Cloudbreak{Cloudbreak: apiclient.New(transport, strfmt.Default)}
}

// NewDataplaneHTTPClientFromContext : Initialize Dataplane client.
func NewDataplaneHTTPClientFromContext(c *cli.Context) *Dataplane {
	return NewDataplaneHTTPClient(c.String(fl.FlServerOptional.Name), c.String(fl.FlRefreshTokenOptional.Name))
}

// NewDataplaneHTTPClient : Creates new Dataplane client
func NewDataplaneHTTPClient(address string, refreshToken string) *Dataplane {
	u.CheckServerAddress(address)
	var transport *caasauth.Transport
	const baseAPIPath string = "/"
	if len(refreshToken) == 0 {
		transport, _ = caasauth.NewCaasTransport(address, baseAPIPath)
	} else {
		transport, _ = caasauth.RefreshAccessToken(address, baseAPIPath, refreshToken)
	}
	return &Dataplane{Dataplane: authapiclient.New(transport, strfmt.Default)}
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
