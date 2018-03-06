package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/utils"
	proxyConfig "github.com/hortonworks/cb-cli/client_cloudbreak/v1proxyconfigs"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
	"strconv"
	"time"
)

type proxyClient interface {
	PostPrivateProxyConfig(params *proxyConfig.PostPrivateProxyConfigParams) (*proxyConfig.PostPrivateProxyConfigOK, error)
	PostPublicProxyConfig(params *proxyConfig.PostPublicProxyConfigParams) (*proxyConfig.PostPublicProxyConfigOK, error)
	GetPublicsProxyConfig(params *proxyConfig.GetPublicsProxyConfigParams) (*proxyConfig.GetPublicsProxyConfigOK, error)
}

var ProxyHeader = []string{"Name", "Host", "Port", "Protocol", "User"}

type proxy struct {
	Name     string `json:"Name" yaml:"Name"`
	Host     string `json:"Host" yaml:"Host"`
	Port     string `json:"Port" yaml:"Port"`
	Protocol string `json:"Protocol" yaml:"Protocol"`
	User     string `json:"User" yaml:"User"`
}

func (p *proxy) DataAsStringArray() []string {
	return []string{p.Name, p.Host, p.Port, p.Protocol, p.User}
}

func CreateProxy(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "create proxy")

	name := c.String(FlName.Name)
	host := c.String(FlProxyHost.Name)
	port := c.String(FlProxyPort.Name)
	protocol := c.String(FlProxyProtocol.Name)
	user := c.String(FlProxyUser.Name)
	password := c.String(FlProxyPassword.Name)
	public := c.Bool(FlPublicOptional.Name)

	if protocol != "http" && protocol != "https" {
		utils.LogErrorMessageAndExit("Proxy protocol must be either http or https")
	}
	serverPort, _ := strconv.Atoi(port)

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	return createProxy(cbClient.Cloudbreak.V1proxyconfigs, name, host, int32(serverPort), protocol, user, password, public)
}

func createProxy(proxyClient proxyClient, name, host string, port int32, protocol, user, password string, public bool) error {
	proxyRequest := &models_cloudbreak.ProxyConfigRequest{
		Name:       &name,
		ServerHost: &host,
		ServerPort: &port,
		Protocol:   &protocol,
		UserName:   user,
		Password:   password,
	}

	log.Infof("[createProxy] create proxy with name: %s", name)
	var proxy *models_cloudbreak.ProxyConfigResponse
	if public {
		resp, err := proxyClient.PostPublicProxyConfig(proxyConfig.NewPostPublicProxyConfigParams().WithBody(proxyRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		proxy = resp.Payload
	} else {
		resp, err := proxyClient.PostPrivateProxyConfig(proxyConfig.NewPostPrivateProxyConfigParams().WithBody(proxyRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		proxy = resp.Payload
	}

	log.Infof("[createProxy] proxy created with name: %s, id: %d", name, proxy.ID)
	return nil
}

func ListProxies(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list proxies")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	return listProxiesImpl(cbClient.Cloudbreak.V1proxyconfigs, output.WriteList)
}

func listProxiesImpl(proxyClient proxyClient, writer func([]string, []utils.Row)) error {
	resp, err := proxyClient.GetPublicsProxyConfig(proxyConfig.NewGetPublicsProxyConfigParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, p := range resp.Payload {
		row := &proxy{
			Name:     *p.Name,
			Host:     *p.ServerHost,
			Port:     strconv.Itoa(int(*p.ServerPort)),
			Protocol: *p.Protocol,
			User:     p.UserName,
		}
		tableRows = append(tableRows, row)
	}

	writer(ProxyHeader, tableRows)
	return nil
}

func DeleteProxy(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete a proxy")

	proxyName := c.String(FlName.Name)
	log.Infof("[DeleteProxy] delete proxy config by name: %s", proxyName)

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	if err := cbClient.Cloudbreak.V1proxyconfigs.DeletePublicProxyConfig(proxyConfig.NewDeletePublicProxyConfigParams().WithName(proxyName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteProxy] proxy config deleted: %s", proxyName)
	return nil
}
