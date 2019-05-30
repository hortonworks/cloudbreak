package role

import (
	"time"

	"github.com/hortonworks/cb-cli/dataplane/oauth"

	"github.com/go-openapi/swag"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/client/roles"
	"github.com/hortonworks/cb-cli/dataplane/oauthapi/model"
	"github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

var roleDetailsHeader = []string{"ID", "Name", "DisplayName", "Service"}

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

type roleClient interface {
	GetRoles(params *roles.GetRolesParams) (*roles.GetRolesOK, error)
	GetRoleByID(params *roles.GetRoleByIDParams) (*roles.GetRoleByIDOK, error)
}

// ListRoles : List roles
func ListRoles(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list roles")
	log.Infof("[ListUsers] List all roles")
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

//GetRoles : return all roles present in system
func GetRoles(client roleClient) []*model.RoleWithPermissionDetails {
	log.Infof("[listUsersImpl] sending list roles request")
	resp, err := client.GetRoles(roles.NewGetRolesParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	return resp.Payload
}
