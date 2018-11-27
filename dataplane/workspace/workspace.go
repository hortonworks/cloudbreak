package workspace

import (
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/dataplane/api/client/v3workspaces"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
	"gopkg.in/yaml.v2"
)

var workspaceListHeader = []string{"Name", "Description", "Permissions"}

type workspaceListOut struct {
	Workspace *model.WorkspaceResponse `json:"Workspace" yaml:"Workspace"`
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
	return []string{o.Workspace.Name, utils.SafeStringConvert(o.Workspace.Description), permissionString}
}

func (o *workspaceListOutDescribe) DataAsStringArray() []string {
	return append(o.workspaceListOut.DataAsStringArray(), o.ID)
}

type workspaceClient interface {
	CreateWorkspace(params *v3workspaces.CreateWorkspaceParams) (*v3workspaces.CreateWorkspaceOK, error)
	GetWorkspaces(params *v3workspaces.GetWorkspacesParams) (*v3workspaces.GetWorkspacesOK, error)
	GetWorkspaceByName(params *v3workspaces.GetWorkspaceByNameParams) (*v3workspaces.GetWorkspaceByNameOK, error)
}

func CreateWorkspace(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "create workspace")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	workspaceName := c.String(fl.FlName.Name)
	workspaceDesc := c.String(fl.FlDescriptionOptional.Name)
	createWorkspaceImpl(cbClient.Cloudbreak.V3workspaces, workspaceName, workspaceDesc)
}

func createWorkspaceImpl(client workspaceClient, name string, description string) *model.WorkspaceResponse {
	log.Infof("[CreateWorkspace] Create a workspace into a tenant")
	req := model.WorkspaceRequest{
		Name:        name,
		Description: &description,
	}
	resp, err := client.CreateWorkspace(v3workspaces.NewCreateWorkspaceParams().WithBody(&req))
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
	_, err := cbClient.Cloudbreak.V3workspaces.DeleteWorkspaceByName(v3workspaces.NewDeleteWorkspaceByNameParams().WithName(workspaceName))
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
	describeWorkspaceImpl(cbClient.Cloudbreak.V3workspaces, output.WriteList, workspaceName)
}

func describeWorkspaceImpl(client workspaceClient, writer func([]string, []utils.Row), workspaceName string) {
	resp, err := client.GetWorkspaceByName(v3workspaces.NewGetWorkspaceByNameParams().WithName(workspaceName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	tableRows := []utils.Row{}
	tableRows = append(tableRows, &workspaceListOutDescribe{&workspaceListOut{resp.Payload}, strconv.FormatInt(resp.Payload.ID, 10)})
	writer(append(workspaceListHeader, "ID"), tableRows)
}

func ListWorkspaces(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list workspaces")
	log.Infof("[ListWorkspaces] List all workspaces in a tenant")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	listWorkspacesImpl(cbClient.Cloudbreak.V3workspaces, output.WriteList)
}

func listWorkspacesImpl(client workspaceClient, writer func([]string, []utils.Row)) {
	tableRows := []utils.Row{}
	for _, workspace := range getWorkspaceListImpl(client) {
		tableRows = append(tableRows, &workspaceListOut{workspace})
	}
	writer(workspaceListHeader, tableRows)
}

func GetWorkspaceIdByName(c *cli.Context, workspaceName string) int64 {
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V3workspaces.GetWorkspaceByName(v3workspaces.NewGetWorkspaceByNameParams().WithName(workspaceName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	return resp.Payload.ID
}

func GetWorkspaceList(c *cli.Context) []*model.WorkspaceResponse {
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	return getWorkspaceListImpl(cbClient.Cloudbreak.V3workspaces)
}

func getWorkspaceListImpl(client workspaceClient) []*model.WorkspaceResponse {
	resp, err := client.GetWorkspaces(v3workspaces.NewGetWorkspacesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

func RemoveUser(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "remove user from workspace")
	log.Infof("[RemoveUser] Remove user from workspace")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	userID := c.String(fl.FlUserID.Name)
	workspaceName := c.String(fl.FlName.Name)
	_, err := cbClient.Cloudbreak.V3workspaces.RemoveWorkspaceUsers(v3workspaces.NewRemoveWorkspaceUsersParams().WithName(workspaceName).WithBody([]string{userID}))
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

func addUser(c *cli.Context, permissions []string) {
	defer utils.TimeTrack(time.Now(), "add user to workspace")
	log.Infof("[AddUser] add user to workspace")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	userID := c.String(fl.FlUserID.Name)
	workspaceName := c.String(fl.FlName.Name)

	changeUsersJSON := &model.ChangeWorkspaceUsersJSON{
		UserID:      userID,
		Permissions: permissions,
	}

	_, err := cbClient.Cloudbreak.V3workspaces.AddWorkspaceUsers(v3workspaces.NewAddWorkspaceUsersParams().WithName(workspaceName).WithBody([]*model.ChangeWorkspaceUsersJSON{changeUsersJSON}))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	log.Infof("[AddUser] user added to workspace: %s", workspaceName)
}
