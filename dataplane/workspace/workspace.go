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
	CreateWorkspace(params *v4ws.CreateWorkspaceParams) (*v4ws.CreateWorkspaceOK, error)
	GetWorkspaces(params *v4ws.GetWorkspacesParams) (*v4ws.GetWorkspacesOK, error)
	GetWorkspaceByName(params *v4ws.GetWorkspaceByNameParams) (*v4ws.GetWorkspaceByNameOK, error)
}

func CreateWorkspace(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "create workspace")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceName := c.String(fl.FlName.Name)
	workspaceDesc := c.String(fl.FlDescriptionOptional.Name)
	createWorkspaceImpl(cbClient.Cloudbreak.V4workspaces, workspaceName, workspaceDesc)
}

func createWorkspaceImpl(client workspaceClient, name string, description string) *model.WorkspaceV4Response {
	log.Infof("[CreateWorkspace] Create a workspace into a tenant")
	req := model.WorkspaceV4Request{
		Name:        &name,
		Description: &description,
	}
	resp, err := client.CreateWorkspace(v4ws.NewCreateWorkspaceParams().WithBody(&req))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[CreateWorkspace] workspace created: %s", name)
	return resp.Payload
}

func DeleteWorkspace(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "delete workspace")
	log.Infof("[DeleteWorkspace] Delete a workspace from a tenant")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	workspaceName := c.String(fl.FlName.Name)
	_, err := cbClient.Cloudbreak.V4workspaces.DeleteWorkspaceByName(v4ws.NewDeleteWorkspaceByNameParams().WithName(workspaceName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[DeleteWorkspace] workspace deleted: %s", workspaceName)
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

func GetWorkspaceIdByName(c *cli.Context, workspaceName string) int64 {
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V4workspaces.GetWorkspaceByName(v4ws.NewGetWorkspaceByNameParams().WithName(workspaceName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	return resp.Payload.ID
}

func getWorkspaceListImpl(client workspaceClient) []*model.WorkspaceV4Response {
	resp, err := client.GetWorkspaces(v4ws.NewGetWorkspacesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload.Responses
}

func RemoveUser(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "remove user from workspace")
	log.Infof("[RemoveUser] Remove user from workspace")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	userID := c.String(fl.FlUserID.Name)
	workspaceName := c.String(fl.FlName.Name)
	_, err := cbClient.Cloudbreak.V4workspaces.RemoveWorkspaceUsers(v4ws.NewRemoveWorkspaceUsersParams().WithName(workspaceName).WithBody(&model.UserIds{UserIds: []string{userID}}))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[RemoveUser] user removed from workspace: %s", workspaceName)
}

func AddReadUser(c *cli.Context) {
	addUser(c, []string{"ALL:READ"})
}

func AddReadWriteUser(c *cli.Context) {
	addUser(c, []string{"ALL:READ", "ALL:WRITE"})
}

func AddManageUser(c *cli.Context) {
	addUser(c, []string{"WORKSPACE:MANAGE"})
}

func AddReadWriteManageUser(c *cli.Context) {
	addUser(c, []string{"ALL:READ", "ALL:WRITE", "WORKSPACE:MANAGE"})
}

func addUser(c *cli.Context, roles []string) {
	defer utils.TimeTrack(time.Now(), "add user to workspace")
	log.Infof("[AddUser] add user to workspace")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	userID := c.String(fl.FlUserID.Name)
	workspaceName := c.String(fl.FlName.Name)

	changeUsersJSON := []*model.ChangeWorkspaceUsersV4Request{
		{
			UserID: &userID,
			Roles:  roles,
		},
	}

	_, err := cbClient.Cloudbreak.V4workspaces.AddWorkspaceUsers(v4ws.NewAddWorkspaceUsersParams().WithName(workspaceName).WithBody(&model.ChangeWorkspaceUsersV4Requests{Users: changeUsersJSON}))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[AddUser] user added to workspace: %s", workspaceName)
}
