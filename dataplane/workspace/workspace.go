package workspace

import (
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	v4ws "github.com/hortonworks/cb-cli/dataplane/api/client/v4workspaces"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
)

var workspaceListHeader = []string{"Name", "Description", "Permissions"}

type workspaceListOut struct {
	Workspace *model.WorkspaceV4Response `json:"Workspace" yaml:"Workspace"`
}

type workspaceListOutDescribe struct {
	*workspaceListOut
	ID string `json:"ID" yaml:"ID"`
}

func (o *workspaceListOut) DataAsStringArray() []string {
	permissionYAML, err := yaml.Marshal(o.Workspace.Users)
	var permissionString string
	if err != nil {
		permissionString = err.Error()
	} else {
		permissionString = string(permissionYAML)
	}
	return []string{utils.SafeStringConvert(o.Workspace.Name), utils.SafeStringConvert(o.Workspace.Description), permissionString}
}

func (o *workspaceListOutDescribe) DataAsStringArray() []string {
	return append(o.workspaceListOut.DataAsStringArray(), o.ID)
}

type workspaceClient interface {
	GetWorkspaces(params *v4ws.GetWorkspacesParams) (*v4ws.GetWorkspacesOK, error)
	GetWorkspaceByName(params *v4ws.GetWorkspaceByNameParams) (*v4ws.GetWorkspaceByNameOK, error)
}

func DescribeWorkspace(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe workspace")
	log.Infof("[DescribeWorkspace] Describes a workspaces in a tenant")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceName := c.String(fl.FlName.Name)
	describeWorkspaceImpl(cbClient.Cloudbreak.V4workspaces, output.WriteList, workspaceName)
}

func describeWorkspaceImpl(client workspaceClient, writer func([]string, []utils.Row), workspaceName string) {
	resp, err := client.GetWorkspaceByName(v4ws.NewGetWorkspaceByNameParams().WithName(workspaceName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var tableRows []utils.Row
	tableRows = append(tableRows, &workspaceListOutDescribe{&workspaceListOut{resp.Payload}, strconv.FormatInt(resp.Payload.ID, 10)})
	writer(append(workspaceListHeader, "ID"), tableRows)
}

func ListWorkspaces(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list workspaces")
	log.Infof("[ListWorkspaces] List all workspaces in a tenant")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	listWorkspacesImpl(cbClient.Cloudbreak.V4workspaces, output.WriteList)
}

func listWorkspacesImpl(client workspaceClient, writer func([]string, []utils.Row)) {
	var tableRows []utils.Row
	for _, workspace := range getWorkspaceListImpl(client) {
		tableRows = append(tableRows, &workspaceListOut{workspace})
	}
	writer(workspaceListHeader, tableRows)
}

func getWorkspaceListImpl(client workspaceClient) []*model.WorkspaceV4Response {
	resp, err := client.GetWorkspaces(v4ws.NewGetWorkspacesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload.Responses
}
