package user

import (
	"github.com/hortonworks/cb-cli/cloudbreak/oauth"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cloudbreak/api/client/v1users"
	"github.com/hortonworks/cb-cli/cloudbreak/api/model"
	fl "github.com/hortonworks/cb-cli/cloudbreak/flags"
	"github.com/hortonworks/cb-cli/dp-cli-common/utils"
	"github.com/urfave/cli"
)

var userListHeader = []string{"User ID", "Name"}

type userListOut struct {
	User *model.UserResponseJSON
}

func (u *userListOut) DataAsStringArray() []string {
	return []string{u.User.UserID, u.User.Username}
}

func ListUsers(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "list users")
	log.Infof("[ListUsers] List all users in a tenant")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V1users.GetAllUsers(v1users.NewGetAllUsersParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	tableRows := []utils.Row{}
	for _, user := range resp.Payload {
		tableRows = append(tableRows, &userListOut{user})
	}
	output.WriteList(userListHeader, tableRows)
}
