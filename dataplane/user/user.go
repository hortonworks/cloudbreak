package user

import (
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	log "github.com/Sirupsen/logrus"
	"github.com/go-openapi/swag"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/oidc"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/roles"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/users"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/model"
	"github.com/hortonworks/cb-cli/dataplane/role"
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

type userRolesOut struct {
	Role *model.RoleWithPermission
}

func (r *userRolesOut) DataAsStringArray() []string {
	return []string{
		r.Role.ID.String(),
		swag.StringValue(r.Role.Name),
		swag.StringValue(r.Role.DisplayName),
		r.Role.Service}
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

type oidcClient interface {
	LoggedInUserInfo(params *oidc.LoggedInUserInfoParams) (*oidc.LoggedInUserInfoOK, error)
}

type roleClient interface {
	GetRoles(params *roles.GetRolesParams) (*roles.GetRolesOK, error)
	GetRoleByID(params *roles.GetRoleByIDParams) (*roles.GetRoleByIDOK, error)
}

// ListUsers : lists user details
func ListUsers(c *cli.Context) {
	log.Infof("[ListUsers] List all users in a tenant")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	usersResponse := listUsersImpl(dpClient.Dataplane.Users)
	tableRows := []utils.Row{}
	for _, user := range usersResponse {
		tableRows = append(tableRows, &userListOut{user})
	}
	output.WriteList(userDetailsHeader, tableRows)
}

func listUsersImpl(client userClient) []*model.UserWithRoles {
	defer utils.TimeTrack(time.Now(), "List Users")
	log.Infof("[listUsersImpl] sending list users request")
	resp, err := client.GetAllUsers(users.NewGetAllUsersParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload.Users
}

// ListRoles : List roles for users
func ListRoles(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list roles")
	log.Infof("[ListUsers] List roles available for a user")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	userNameOption := c.String(fl.FlUserNameOptional.Name)
	var userName string
	// If name is provided : roles information for another user
	var userinfo *model.UserInfo
	userinfo = UserInfoImpl(dpClient.Dataplane.Oidc)
	if userNameOption == "" {
		userName = swag.StringValue(userinfo.Name)
	} else {
		userName = userNameOption
	}
	strategyID := userinfo.StrategyID.String()
	resp, err := dpClient.Dataplane.Users.GetAllUsers(users.NewGetAllUsersParams().WithSearchTerm(&userName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	var filteredRoles []*model.RoleWithPermission

	// Assumption : There cannot be 2 users in a strategy
	for _, user := range resp.Payload.Users {
		if user.StrategyID.String() == strategyID {
			filteredRoles = user.Roles
		}
	}
	// If the user is not found in same strategy try to find in SAML
	//Ideally get active strategy other than local
	if len(filteredRoles) < 1 {
		for _, user := range resp.Payload.Users {
			filteredRoles = user.Roles
		}
	}
	tableRows := []utils.Row{}
	for _, role := range filteredRoles {
		tableRows = append(tableRows, &userRolesOut{role})
	}

	output.WriteList(roleDetailsHeader, tableRows)
}

// Userinfo : Lists information for connected user
func Userinfo(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list user information")
	log.Infof("[ListUsers] List information for connected user")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	output.Write(userDetailsHeader, &UsersInfoOut{UserInfoImpl(dpClient.Dataplane.Oidc)})
}

// UserInfoImpl : gets User information for connected user
func UserInfoImpl(client oidcClient) *model.UserInfo {
	defer utils.TimeTrack(time.Now(), "user info")
	log.Infof("[UserInfoImpl] sending user info request")
	resp, err := client.LoggedInUserInfo(oidc.NewLoggedInUserInfoParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}

// AssignRoles : Assign roles to a user
func AssignRoles(c *cli.Context) {
	log.Infof("[AssignRoles] Adding roles to user")
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

//AssignRolesToUserByName : Assign roles to a user by role names
func AssignRolesToUserByName(c *cli.Context) {
	log.Infof("[AssignRolesToUserByName] Adding roles to user")
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	userID := getUserIDFromName(
		dpClient.Dataplane.Users,
		c.String(fl.FlCaasStrategyName.Name),
		c.String(fl.FlUserName.Name),
	)
	if userID == "" {
		utils.LogErrorMessageAndExit(fmt.Sprintf("Could not find user with Name %s ... exiting", c.String(fl.FlUserName.Name)))
	}
	roleIDs := getRoleIDsFromRoles(
		dpClient.Dataplane.Roles,
		c.String(fl.FlRoleNames.Name),
	)
	if len(roleIDs) != len(utils.DelimitedStringToArray(c.String(fl.FlRoleNames.Name), ",")) {
		utils.LogErrorMessageAndExit(fmt.Sprintf("Not able to find all the specified roles names in system.  %s ... exiting", c.String(fl.FlRoleNames.Name)))
	}
	assignRolesImpl(dpClient.Dataplane.Users, userID, roleIDs, output.WriteList)
}

// RevokeRoles : Revokes roles from a user
func RevokeRoles(c *cli.Context) {
	log.Infof("[CreateKubernetes] Adding roles to user")
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	revokeRolesImpl(
		dpClient.Dataplane.Users,
		c.String(fl.FlUserID.Name),
		utils.DelimitedStringToArray(c.String(fl.FlRolesIDs.Name), ","))
}

//RevokeRolesFromUserByName : revoke roles from a user by role names
func RevokeRolesFromUserByName(c *cli.Context) {
	log.Infof("[RevokeRolesFromUserByName] revoke roles to user")
	dpClient := oauth.NewDataplaneHTTPClientFromContext(c)
	userID := getUserIDFromName(
		dpClient.Dataplane.Users,
		c.String(fl.FlCaasStrategyName.Name),
		c.String(fl.FlUserName.Name),
	)
	if userID == "" {
		utils.LogErrorMessageAndExit(fmt.Sprintf("Could not find user with Name %s ... exiting", c.String(fl.FlUserName.Name)))
	}
	roleIDs := getRoleIDsFromRoles(
		dpClient.Dataplane.Roles,
		c.String(fl.FlRoleNames.Name),
	)
	if len(roleIDs) != len(utils.DelimitedStringToArray(c.String(fl.FlRoleNames.Name), ",")) {
		utils.LogErrorMessageAndExit(fmt.Sprintf("Not able to find all the specified roles names in system.  %s ... exiting", c.String(fl.FlRoleNames.Name)))
	}
	revokeRolesImpl(dpClient.Dataplane.Users, userID, roleIDs)
}

func revokeRolesImpl(client userClient, userid string, roleids []string) {
	defer utils.TimeTrack(time.Now(), "revoke roles from user")
	log.Infof("[revokeRolesImpl] sending revoke roles request")
	userRolesRequest := &model.RolesInput{
		RoleIds: roleids}

	resp, err := client.DeleteRolesFromUser(users.NewDeleteRolesFromUserParams().WithUserID(userid).WithRoleIds(userRolesRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[revokeRolesImpl] roles deleted : %s ", strconv.FormatBool(resp.Payload))
}

func getUserIDFromName(client userClient, strategy string, name string) string {
	usersResponse := listUsersImpl(client)
	//If startegy is local
	if strings.EqualFold(strategy, "local") {
		for _, user := range usersResponse {
			if name == swag.StringValue(user.Name) && swag.StringValue(user.StrategyName) == "local0" {
				return user.ID.String()
			}
		}
	} else {
		// There will only other strategy enabled at time and it will not be local
		for _, user := range usersResponse {
			if name == swag.StringValue(user.Name) && swag.StringValue(user.StrategyName) != "local0" {
				return user.ID.String()
			}
		}
	}
	return ""
}

func getRoleIDsFromRoles(client roleClient, names string) []string {
	roleNames := utils.DelimitedStringToArray(names, ",")
	var roleIDs []string
	rolesResponse := role.GetRoles(client)
	for _, role := range rolesResponse {
		if contains(roleNames, swag.StringValue(role.Name)) {
			roleIDs = append(roleIDs, role.ID.String())
		}
	}
	return roleIDs
}

func contains(a []string, x string) bool {
	for _, n := range a {
		if x == n {
			return true
		}
	}
	return false
}
