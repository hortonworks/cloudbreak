package redbeams

import (
	"time"

	"github.com/hortonworks/cb-cli/dataplane/api-redbeams/client/database_servers"
	"github.com/hortonworks/cb-cli/dataplane/api-redbeams/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	commonutils "github.com/hortonworks/dp-cli-common/utils"
	log "github.com/sirupsen/logrus"
	"github.com/urfave/cli"
)

type ClientRedbeams oauth.Redbeams

var serverListHeader = []string{"Name", "Description", "Crn", "EnvironmentCrn", "Status", "ResourceStatus", "DatabaseVendor", "Host", "Port"}

type serverDetails struct {
	Name           string `json:"Name" yaml:"Name"`
	Description    string `json:"Description" yaml:"Description"`
	CRN            string `json:"CRN" yaml:"CRN"`
	EnvironmentCrn string `json:"EnvironmentCrn" yaml:"EnvironmentCrn"`
	Status         string `json:"Status" yaml:"Status"`
	StatusReason   string `json:"StatusReason" yaml:"StatusReason"`
	ResourceStatus string `json:"ResourceStatus" yaml:"ResourceStatus"`
	DatabaseVendor string `json:"DatabaseVendor" yaml:"DatabaseVendor"`
	Host           string `json:"Host" yaml:"Host"`
	Port           int32  `json:"Port" yaml:"Port"`
	CreationDate   int64  `json:"CreationDate" yaml:"CreationDate"`
}

func (server *serverDetails) DataAsStringArray() []string {
	return []string{server.Name, server.Description, server.CRN, server.EnvironmentCrn, server.Status, server.DatabaseVendor, server.Host, string(server.Port)}
}

func NewDetailsFromResponse(r *model.DatabaseServerV4Response) *serverDetails {
	details := &serverDetails{
		Name:           *r.Name,
		CRN:            r.Crn,
		EnvironmentCrn: *r.EnvironmentCrn,
		Status:         r.Status,
		StatusReason:   r.StatusReason,
		ResourceStatus: r.ResourceStatus,
		CreationDate:   r.CreationDate,
	}

	// ternary operator, where art thou?
	if r.Description != nil {
		details.Description = *r.Description
	}
	if r.DatabaseVendor != nil {
		details.DatabaseVendor = *r.DatabaseVendor
	}
	if r.Host != nil {
		details.Host = *r.Host
	}
	if r.Port != nil {
		details.Port = *r.Port
	}

	return details
}

var statusListHeader = []string{"Name", "Crn", "EnvironmentCrn", "Status"}

type serverStatusDetails struct {
	Name           string `json:"Name" yaml:"Name"`
	CRN            string `json:"CRN" yaml:"CRN"`
	EnvironmentCrn string `json:"EnvironmentCrn" yaml:"EnvironmentCrn"`
	Status         string `json:"Status" yaml:"Status"`
}

func (status *serverStatusDetails) DataAsStringArray() []string {
	return []string{status.Name, status.CRN, status.EnvironmentCrn, status.Status}
}

func NewDetailsFromStatusResponse(r *model.DatabaseServerStatusV4Response) *serverStatusDetails {
	details := &serverStatusDetails{
		Name:           *r.Name,
		CRN:            *r.ResourceCrn,
		EnvironmentCrn: *r.EnvironmentCrn,
		Status:         *r.Status,
	}

	return details
}

func ListDatabaseServers(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "List database servers in environment")
	envCrn := c.String(fl.FlEnvironmentCrn.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	log.Infof("[ListDBServers] Listing database servers in environment: %s", envCrn)
	resp, err := redbeamsDbServerClient.ListDatabaseServers(database_servers.NewListDatabaseServersParams().WithEnvironmentCrn(envCrn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}

	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}

	var tableRows []commonutils.Row

	for _, response := range resp.Payload.Responses {
		row := NewDetailsFromResponse(response)
		tableRows = append(tableRows, row)
	}
	output.WriteList(serverListHeader, tableRows)
}

func GetDatabaseServer(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Get a database server by CRN")
	crn := c.String(fl.FlCrn.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	log.Infof("[GetDBServer] Getting database server with CRN: %s", crn)
	resp, err := redbeamsDbServerClient.GetDatabaseServerByCrn(database_servers.NewGetDatabaseServerByCrnParams().WithCrn(crn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}

	server := resp.Payload
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	row := NewDetailsFromResponse(server)
	output.Write(serverListHeader, row)
}

func GetDatabaseServerByName(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Get a database server by name")
	envCrn := c.String(fl.FlEnvironmentCrn.Name)
	name := c.String(fl.FlName.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	log.Infof("[GetDBServerByName] Getting database server with name: %s", name)
	resp, err := redbeamsDbServerClient.GetDatabaseServerByName(database_servers.NewGetDatabaseServerByNameParams().WithEnvironmentCrn(envCrn).WithName(name))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}

	server := resp.Payload
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	row := NewDetailsFromResponse(server)
	output.Write(serverListHeader, row)
}

func GetDatabaseServerStatus(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Get the status of a database server by CRN")
	crn := c.String(fl.FlCrn.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	log.Infof("[GetDBServerStatus] Getting status for database server with CRN: %s", crn)
	resp, err := redbeamsDbServerClient.GetDatabaseServerStatusByCrn(database_servers.NewGetDatabaseServerStatusByCrnParams().WithCrn(crn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}

	status := resp.Payload
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	row := NewDetailsFromStatusResponse(status)
	output.Write(statusListHeader, row)
}

func GetDatabaseServerStatusByName(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Get the status of a database server by name")
	envCrn := c.String(fl.FlEnvironmentCrn.Name)
	name := c.String(fl.FlName.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	log.Infof("[GetDBServerStatusByName] Getting status for database server with name: %s", name)
	resp, err := redbeamsDbServerClient.GetDatabaseServerStatusByName(database_servers.NewGetDatabaseServerStatusByNameParams().WithEnvironmentCrn(envCrn).WithName(name))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}

	status := resp.Payload
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	row := NewDetailsFromStatusResponse(status)
	output.Write(statusListHeader, row)
}

func TerminateManagedDatabaseServer(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Terminate a managed database server")
	crn := c.String(fl.FlCrn.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	log.Infof("[TerminateDBServer] Terminating database server with CRN: %s", crn)
	resp, err := redbeamsDbServerClient.TerminateManagedDatabaseServer(database_servers.NewTerminateManagedDatabaseServerParams().WithCrn(crn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}

	outcome := resp.Payload
	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	output.Write(statusListHeader, &serverStatusDetails{
		Name:           *outcome.Name,
		CRN:            *outcome.ResourceCrn,
		EnvironmentCrn: *outcome.EnvironmentCrn,
		Status:         outcome.Status,
	})
}

func DeleteDatabaseServer(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Delete a registered database server")
	crn := c.String(fl.FlCrn.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	log.Infof("[DeleteDBServer] Deleting database server with CRN: %s", crn)
	result, err := redbeamsDbServerClient.DeleteDatabaseServerByCrn(database_servers.NewDeleteDatabaseServerByCrnParams().WithCrn(crn))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteDBServer] Deleted database server with CRN: %s Details: %s", crn, result)
}
