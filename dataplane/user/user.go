package user

import (
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/go-openapi/swag"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/oidc"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/roles"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/users"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/model"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var userDetailsHeader = []string{"ID", "PreferredUsername", "Name", "Email"}
var roleDetailsHeader = []string{"ID", "Name", "DisplayName", "Service"}
var roleAssignedHeader = []string{"ID", "RoleID", "UserID"}

type UsersInfoOut struct {
	User *model.UserInfo
}

func (u *UsersInfoOut) DataAsStringArray() []string {
	return []string{
		u.User.ID.String(),
		swag.StringValue(u.User.PreferredUsername),
		swag.StringValue(u.User.Name),
		u.User.Email}
}

type userListOut struct {
	User *model.UserWithRoles
}

func (u *userListOut) DataAsStringArray() []string {
	return []string{
		u.User.ID.String(),
		swag.StringValue(u.User.PreferredUsername),
		swag.StringValue(u.User.Name),
		u.User.Email}
}

type roleDetailsOut struct {
	Role *model.RoleWithPermissionDetails
}

func (r *roleDetailsOut) DataAsStringArray() []string {
	return []string{
		r.Role.ID.String(),
		swag.StringValue(r.Role.Name),
		swag.StringValue(r.Role.DisplayName),
		swag.StringValue(r.Role.Service.Name)}
}

type roleAssigned struct {
	Role *model.UserRole
}

func (r *roleAssigned) DataAsStringArray() []string {
	return []string{
		r.Role.ID.String(),
		r.Role.RoleID.String(),
		r.Role.UserID.String()}
}

type userClient interface {
	GetAllUsers(params *users.GetAllUsersParams) (*users.GetAllUsersOK, error)
	AssignRolesToUser(params *users.AssignRolesToUserParams) (*users.AssignRolesToUserOK, error)
	DeleteRolesFromUser(params *users.DeleteRolesFromUserParams) (*users.DeleteRolesFromUserOK, error)
}

// ListUsers : lists user details
func ListUsers(c *cli.Context) {
	log.Infof("[ListUsers] List all users in a tenant")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	listUsersImpl(dpClient.Dataplane.Users, output.WriteList)
}

func listUsersImpl(client userClient, writer func([]string, []utils.Row)) {
	defer utils.TimeTrack(time.Now(), "List Users")
	log.Infof("[listUsersImpl] sending list users request")
	resp, err := client.GetAllUsers(users.NewGetAllUsersParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	tableRows := []utils.Row{}
	for _, user := range resp.Payload {
		tableRows = append(tableRows, &userListOut{user})
	}
	writer(userDetailsHeader, tableRows)
}

// ListRoles : List roles for users
func ListRoles(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list roles")
	log.Infof("[ListUsers] List all roles of the user")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	resp, err := dpClient.Dataplane.Roles.GetRoles(roles.NewGetRolesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	tableRows := []utils.Row{}
	for _, role := range resp.Payload {
		tableRows = append(tableRows, &roleDetailsOut{role})
	}
	output.WriteList(roleDetailsHeader, tableRows)
}

// Userinfo : Lists information for connected user
func Userinfo(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list user information")
	log.Infof("[ListUsers] List information for connected user")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	resp, err := dpClient.Dataplane.Oidc.LoggedInUserInfo(oidc.NewLoggedInUserInfoParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	output.Write(userDetailsHeader, &UsersInfoOut{resp.Payload})
}

// AssignRoles : Assign roles to a user
func AssignRoles(c *cli.Context) {
	log.Infof("[CreateKubernetes] Adding roles to user")
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	assignRolesImpl(
		dpClient.Dataplane.Users,
		c.String(fl.FlUserID.Name),
		utils.DelimitedStringToArray(c.String(fl.FlRolesIDs.Name), ","),
		output.WriteList)
}

func assignRolesImpl(client userClient, userid string, roleids []string, writer func([]string, []utils.Row)) {
	defer utils.TimeTrack(time.Now(), "assign roles to user")
	log.Infof("[assignRolesImpl] sending assign roles request")
	userRolesRequest := &model.RolesInput{
		RoleIds: roleids}

	resp, err := client.AssignRolesToUser(users.NewAssignRolesToUserParams().WithUserID(userid).WithRoleIds(userRolesRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	tableRows := []utils.Row{}
	for _, res := range resp.Payload {
		tableRows = append(tableRows, &roleAssigned{res})
	}
	writer(roleAssignedHeader, tableRows)
}

// RevokeRoles : Revokes roles from a user
func RevokeRoles(c *cli.Context) {
	log.Infof("[CreateKubernetes] Adding roles to user")
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	revokeRolesImpl(
		dpClient.Dataplane.Users,
		c.String(fl.FlUserID.Name),
		utils.DelimitedStringToArray(c.String(fl.FlRolesIDs.Name), ","),
		output.Write)
}

func revokeRolesImpl(client userClient, userid string, roleids []string, writer func([]string, utils.Row)) {
	defer utils.TimeTrack(time.Now(), "assign roles to user")
	log.Infof("[assignRolesImpl] sending assign roles request")
	userRolesRequest := &model.RolesInput{
		RoleIds: roleids}

	resp, err := client.DeleteRolesFromUser(users.NewDeleteRolesFromUserParams().WithUserID(userid).WithRoleIds(userRolesRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[revokeRolesImpl] roles deleted : %s ", strconv.FormatBool(resp.Payload))
}
