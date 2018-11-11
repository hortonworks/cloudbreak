package proxy

import (
	"github.com/hortonworks/cb-cli/cloudbreak/oauth"
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	proxyConfig "github.com/hortonworks/cb-cli/cloudbreak/api/client/v3_workspace_id_proxyconfigs"
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/utils"
	"github.com/urfave/cli"
	"strings"
)

type proxyClient interface {
	CreateProxyconfigInWorkspace(params *proxyConfig.CreateProxyconfigInWorkspaceParams) (*proxyConfig.CreateProxyconfigInWorkspaceOK, error)
	ListProxyconfigsByWorkspace(params *proxyConfig.ListProxyconfigsByWorkspaceParams) (*proxyConfig.ListProxyconfigsByWorkspaceOK, error)
}

var ProxyHeader = []string{"Name", "Host", "Port", "Protocol", "Environments"}

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

	return createProxy(cbClient.Cloudbreak.V3WorkspaceIDProxyconfigs, workspaceID, name, host, int32(serverPort), protocol, user, password, environments)
}

func createProxy(proxyClient proxyClient, workspaceID int64, name, host string, port int32, protocol, user, password string, environments []string) error {
	proxyRequest := &model.ProxyConfigRequest{
		Name:         &name,
		ServerHost:   &host,
		ServerPort:   &port,
		Protocol:     &protocol,
		UserName:     user,
		Password:     password,
		Environments: environments,
	}

	log.Infof("[createProxy] create proxy with name: %s", name)
	var proxy *model.ProxyConfigResponse
	resp, err := proxyClient.CreateProxyconfigInWorkspace(proxyConfig.NewCreateProxyconfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(proxyRequest))
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
	attachRequest := proxyConfig.NewAttachProxyResourceToEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(proxyName).WithBody(environments)
	response, err := cbClient.Cloudbreak.V3WorkspaceIDProxyconfigs.AttachProxyResourceToEnvironments(attachRequest)
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
	detachRequest := proxyConfig.NewDetachProxyResourceFromEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(proxyName).WithBody(environments)
	response, err := cbClient.Cloudbreak.V3WorkspaceIDProxyconfigs.DetachProxyResourceFromEnvironments(detachRequest)
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
	return listProxiesImpl(cbClient.Cloudbreak.V3WorkspaceIDProxyconfigs, output.WriteList, workspaceID)
}

func listProxiesImpl(proxyClient proxyClient, writer func([]string, []utils.Row), workspaceID int64) error {
	resp, err := proxyClient.ListProxyconfigsByWorkspace(proxyConfig.NewListProxyconfigsByWorkspaceParams().WithWorkspaceID(workspaceID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, p := range resp.Payload {
		row := &proxy{
			Name:         *p.Name,
			Host:         *p.ServerHost,
			Port:         strconv.Itoa(int(*p.ServerPort)),
			Protocol:     *p.Protocol,
			Environments: p.Environments,
		}
		tableRows = append(tableRows, row)
	}

	writer(ProxyHeader, tableRows)
	return nil
}

func DeleteProxy(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete a proxy")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	proxyName := c.String(fl.FlName.Name)
	log.Infof("[DeleteProxy] delete proxy config by name: %s", proxyName)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	if _, err := cbClient.Cloudbreak.V3WorkspaceIDProxyconfigs.DeleteProxyconfigInWorkspace(proxyConfig.NewDeleteProxyconfigInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(proxyName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteProxy] proxy config deleted: %s", proxyName)
	return nil
}
