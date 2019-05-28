package proxy

import (
	"strconv"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	"strings"

	log "github.com/Sirupsen/logrus"
	v4Proxy "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_proxies"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

type proxyClient interface {
	CreateProxyConfigInWorkspace(params *v4Proxy.CreateProxyConfigInWorkspaceParams) (*v4Proxy.CreateProxyConfigInWorkspaceOK, error)
	ListProxyConfigsByWorkspace(params *v4Proxy.ListProxyConfigsByWorkspaceParams) (*v4Proxy.ListProxyConfigsByWorkspaceOK, error)
}

var Header = []string{"Name", "Host", "Port", "Protocol", "Environments"}

type proxy struct {
	Name         string `json:"Name" yaml:"Name"`
	Host         string `json:"Host" yaml:"Host"`
	Port         string `json:"Port" yaml:"Port"`
	Protocol     string `json:"Protocol" yaml:"Protocol"`
	Environments []string
}

func (p *proxy) DataAsStringArray() []string {
	return []string{p.Name, p.Host, p.Port, p.Protocol, strings.Join(p.Environments, ",")}
}

func CreateProxy(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "create proxy")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	name := c.String(fl.FlName.Name)
	host := c.String(fl.FlProxyHost.Name)
	port := c.String(fl.FlProxyPort.Name)
	protocol := c.String(fl.FlProxyProtocol.Name)
	user := c.String(fl.FlProxyUser.Name)
	password := c.String(fl.FlProxyPassword.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironmentsOptional.Name), ",")

	if protocol != "http" && protocol != "https" {
		utils.LogErrorMessageAndExit("Proxy protocol must be either http or https")
	}
	serverPort, _ := strconv.Atoi(port)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	return createProxy(cbClient.Cloudbreak.V4WorkspaceIDProxies, workspaceID, name, host, int32(serverPort), protocol, user, password, environments)
}

func createProxy(proxyClient proxyClient, workspaceID int64, name, host string, port int32, protocol, user, password string, environments []string) error {
	proxyRequest := &model.ProxyV4Request{
		Name:     &name,
		Host:     &host,
		Port:     &port,
		Protocol: &protocol,
		UserName: user,
		Password: password,
	}

	log.Infof("[createProxy] create proxy with name: %s", name)
	var proxy *model.ProxyV4Response
	resp, err := proxyClient.CreateProxyConfigInWorkspace(v4Proxy.NewCreateProxyConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(proxyRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	proxy = resp.Payload

	log.Infof("[createProxy] proxy created with name: %s, id: %d", name, proxy.ID)
	return nil
}

func ListProxies(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list proxies")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	return listProxiesImpl(cbClient.Cloudbreak.V4WorkspaceIDProxies, output.WriteList, workspaceID)
}

func listProxiesImpl(proxyClient proxyClient, writer func([]string, []utils.Row), workspaceID int64) error {
	resp, err := proxyClient.ListProxyConfigsByWorkspace(v4Proxy.NewListProxyConfigsByWorkspaceParams().WithWorkspaceID(workspaceID))
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
		}
		tableRows = append(tableRows, row)
	}

	writer(Header, tableRows)
	return nil
}

func DeleteProxy(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete a proxy")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	proxyName := c.String(fl.FlName.Name)
	log.Infof("[DeleteProxy] delete proxy config by name: %s", proxyName)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	if _, err := cbClient.Cloudbreak.V4WorkspaceIDProxies.DeleteProxyConfigInWorkspace(v4Proxy.NewDeleteProxyConfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(proxyName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteProxy] proxy config deleted: %s", proxyName)
	return nil
}
