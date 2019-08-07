package redbeams

import (
	"time"

	"github.com/hortonworks/cb-cli/dataplane/api-redbeams/client/database_servers"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"github.com/hortonworks/dp-cli-common/utils"
	commonutils "github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

type ClientRedbeams oauth.Redbeams

var listHeader = []string{"Name", "Crn", "EnvironmentCrn", "Status"}

type redbeamsDetails struct {
	Name           string `json:"Name" yaml:"Name"`
	CRN            string `json:"CRN" yaml:"CRN"`
	EnvironmentCrn string `json:"EnvironmentCrn" yaml:"EnvironmentCrn"`
	Status         string `json:"Status" yaml:"Status"`
}

func (redbeams *redbeamsDetails) DataAsStringArray() []string {
	return []string{redbeams.Name, redbeams.CRN, redbeams.EnvironmentCrn, redbeams.Status}
}

func ListRBDMSInstances(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "List Rbdms instances in environment")
	envCrn := c.String(fl.FlEnvironmentCrn.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	resp, err := redbeamsDbServerClient.ListDatabaseServers(database_servers.NewListDatabaseServersParams().WithEnvironmentCrn(envCrn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}

	log.Infof("[ListRBDMS] RBDMS list in environment: %s", envCrn)
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}

	var tableRows []commonutils.Row

	for _, response := range resp.Payload.Responses {
		row := &redbeamsDetails{
			Name:           *response.Name,
			CRN:            response.Crn,
			EnvironmentCrn: *response.EnvironmentCrn,
			Status:         response.Status,
		}
		tableRows = append(tableRows, row)
	}
	output.WriteList(listHeader, tableRows)
}

func DeleteRBDMSInstance(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "delete RBDMS instance")
	crn := c.String(fl.FlCrn.Name)

	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers
	result, err := redbeamsDbServerClient.DeleteDatabaseServerByCrn(database_servers.NewDeleteDatabaseServerByCrnParams().WithCrn(crn))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[deleteRBDMSInstance] RBDMS instance deleted in with name: %s Details: %s", crn, result)
}
