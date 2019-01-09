package proxy

import (
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	v4Proxy "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_proxies"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
	"strings"
)

type proxyClient interface {
	CreateProxyconfigInWorkspace(params *v4Proxy.CreateProxyconfigInWorkspaceParams) (*v4Proxy.CreateProxyconfigInWorkspaceOK, error)
	ListProxyconfigsByWorkspace(params *v4Proxy.ListProxyconfigsByWorkspaceParams) (*v4Proxy.ListProxyconfigsByWorkspaceOK, error)
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
		Name:         &name,
		Host:         &host,
		Port:         &port,
		Protocol:     &protocol,
		UserName:     user,
		Password:     password,
		Environments: environments,
	}

	log.Infof("[createProxy] create proxy with name: %s", name)
	var proxy *model.ProxyV4Response
	resp, err := proxyClient.CreateProxyconfigInWorkspace(v4Proxy.NewCreateProxyconfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(proxyRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	proxy = resp.Payload

	log.Infof("[createProxy] proxy created with name: %s, id: %d", name, proxy.ID)
	return nil
}

func AttachProxyToEnvs(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "attach proxy to environments")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	proxyName := c.String(fl.FlName.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironments.Name), ",")
	log.Infof("[AttachProxyToEnvs] attach proxy config '%s' to environments: %s", proxyName, environments)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	attachRequest := v4Proxy.NewAttachProxyResourceToEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(proxyName).WithBody(&model.EnvironmentNames{EnvironmentNames: environments})
	response, err := cbClient.Cloudbreak.V4WorkspaceIDProxies.AttachProxyResourceToEnvironments(attachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	proxy := response.Payload
	log.Infof("[AttachProxyToEnvs] proxy config '%s' is now attached to the following environments: %s", *proxy.Name, proxy.Environments)
}

func DetachProxyFromEnvs(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "detach proxy from environments")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	proxyName := c.String(fl.FlName.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironments.Name), ",")
	log.Infof("[DetachProxyFromEnvs] detach proxy config '%s' from environments: %s", proxyName, environments)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	detachRequest := v4Proxy.NewDetachProxyResourceFromEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(proxyName).WithBody(&model.EnvironmentNames{EnvironmentNames: environments})
	response, err := cbClient.Cloudbreak.V4WorkspaceIDProxies.DetachProxyResourceFromEnvironments(detachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	proxy := response.Payload
	log.Infof("[DetachProxyFromEnvs] proxy config '%s' is now attached to the following environments: %s", *proxy.Name, proxy.Environments)
}

func ListProxies(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list proxies")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	return listProxiesImpl(cbClient.Cloudbreak.V4WorkspaceIDProxies, output.WriteList, workspaceID)
}

func listProxiesImpl(proxyClient proxyClient, writer func([]string, []utils.Row), workspaceID int64) error {
	resp, err := proxyClient.ListProxyconfigsByWorkspace(v4Proxy.NewListProxyconfigsByWorkspaceParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, p := range resp.Payload.Responses {
		row := &proxy{
			Name:         *p.Name,
			Host:         *p.Host,
			Port:         strconv.Itoa(int(*p.Port)),
			Protocol:     *p.Protocol,
			Environments: p.Environments,
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

	if _, err := cbClient.Cloudbreak.V4WorkspaceIDProxies.DeleteProxyconfigInWorkspace(v4Proxy.NewDeleteProxyconfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(proxyName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteProxy] proxy config deleted: %s", proxyName)
	return nil
}
