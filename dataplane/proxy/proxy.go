package proxy

import (
	"strconv"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	v1Proxy "github.com/hortonworks/cb-cli/dataplane/api-environment/client/v1proxies"
	"github.com/hortonworks/cb-cli/dataplane/api-environment/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

type proxyClient interface {
	CreateProxyConfigV1(params *v1Proxy.CreateProxyConfigV1Params) (*v1Proxy.CreateProxyConfigV1OK, error)
	ListProxyConfigsV1(params *v1Proxy.ListProxyConfigsV1Params) (*v1Proxy.ListProxyConfigsV1OK, error)
}

var Header = []string{"Name", "Host", "Port", "Protocol", "Crn"}

type proxy struct {
	Name     string `json:"Name" yaml:"Name"`
	Host     string `json:"Host" yaml:"Host"`
	Port     string `json:"Port" yaml:"Port"`
	Protocol string `json:"Protocol" yaml:"Protocol"`
	Crn      string `json:"Crn" yaml:"Crn"`
}

func (p *proxy) DataAsStringArray() []string {
	return []string{p.Name, p.Host, p.Port, p.Protocol, p.Crn}
}

func CreateProxy(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "create proxy")

	name := c.String(fl.FlName.Name)
	host := c.String(fl.FlProxyHost.Name)
	port := c.String(fl.FlProxyPort.Name)
	protocol := c.String(fl.FlProxyProtocol.Name)
	user := c.String(fl.FlProxyUser.Name)
	password := c.String(fl.FlProxyPassword.Name)

	if protocol != "http" && protocol != "https" {
		utils.LogErrorMessageAndExit("Proxy protocol must be either http or https")
	}
	serverPort, _ := strconv.Atoi(port)

	envClient := oauth.NewEnvironmentClientFromContext(c)

	return createProxy(envClient.Environment.V1proxies, name, host, int32(serverPort), protocol, user, password)
}

func createProxy(proxyClient proxyClient, name, host string, port int32, protocol, user, password string) error {
	proxyRequest := &model.ProxyRequest{
		Name:     &name,
		Host:     &host,
		Port:     &port,
		Protocol: &protocol,
		UserName: user,
		Password: password,
	}

	log.Infof("[createProxy] create proxy with name: %s", name)
	var proxy *model.ProxyResponse
	resp, err := proxyClient.CreateProxyConfigV1(v1Proxy.NewCreateProxyConfigV1Params().WithBody(proxyRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	proxy = resp.Payload

	log.Infof("[createProxy] proxy created with name: %s, CRN: %s", name, proxy.Crn)
	return nil
}

func ListProxies(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list proxies")

	envClient := oauth.NewEnvironmentClientFromContext(c)

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	return listProxiesImpl(envClient.Environment.V1proxies, output.WriteList)
}

func listProxiesImpl(proxyClient proxyClient, writer func([]string, []utils.Row)) error {
	resp, err := proxyClient.ListProxyConfigsV1(v1Proxy.NewListProxyConfigsV1Params())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, p := range resp.Payload.Responses {
		row := &proxy{
			Name:     *p.Name,
			Host:     *p.Host,
			Port:     strconv.Itoa(int(*p.Port)),
			Protocol: *p.Protocol,
			Crn:      p.Crn,
		}
		tableRows = append(tableRows, row)
	}

	writer(Header, tableRows)
	return nil
}

func DeleteProxy(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete a proxy")

	proxyName := c.String(fl.FlName.Name)
	log.Infof("[DeleteProxy] delete proxy config by name: %s", proxyName)

	envClient := oauth.NewEnvironmentClientFromContext(c)

	if _, err := envClient.Environment.V1proxies.DeleteProxyConfigByNameV1(v1Proxy.NewDeleteProxyConfigByNameV1Params().WithName(proxyName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteProxy] proxy config deleted: %s", proxyName)
	return nil
}
