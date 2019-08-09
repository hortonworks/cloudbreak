package redbeams

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/hortonworks/cb-cli/dataplane/api-redbeams/client/database_servers"
	"github.com/hortonworks/cb-cli/dataplane/api-redbeams/client/databases"
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
	defer commonutils.TimeTrack(time.Now(), "Get a database server")
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	var server *model.DatabaseServerV4Response
	crn := c.String(fl.FlCrn.Name)
	if len(crn) != 0 {
		log.Infof("[GetDBServer] Getting database server with CRN: %s", crn)
		resp, err := redbeamsDbServerClient.GetDatabaseServerByCrn(database_servers.NewGetDatabaseServerByCrnParams().WithCrn(crn))
		if err != nil {
			commonutils.LogErrorAndExit(err)
		}
		server = resp.Payload
	} else {
		envCrn := c.String(fl.FlEnvironmentCrn.Name)
		name := c.String(fl.FlName.Name)
		log.Infof("[GetDBServer] Getting database server with name: %s", name)
		resp, err := redbeamsDbServerClient.GetDatabaseServerByName(database_servers.NewGetDatabaseServerByNameParams().WithEnvironmentCrn(envCrn).WithName(name))
		if err != nil {
			commonutils.LogErrorAndExit(err)
		}
		server = resp.Payload
	}

	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	row := NewDetailsFromResponse(server)
	output.Write(serverListHeader, row)
}

func GetDatabaseServerStatus(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Get the status of a database server")
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers

	var status *model.DatabaseServerStatusV4Response
	crn := c.String(fl.FlCrn.Name)
	if len(crn) != 0 {
		log.Infof("[GetDBServerStatus] Getting status for database server with CRN: %s", crn)
		resp, err := redbeamsDbServerClient.GetDatabaseServerStatusByCrn(database_servers.NewGetDatabaseServerStatusByCrnParams().WithCrn(crn))
		if err != nil {
			commonutils.LogErrorAndExit(err)
		}
		status = resp.Payload
	} else {
		envCrn := c.String(fl.FlEnvironmentCrn.Name)
		name := c.String(fl.FlName.Name)
		log.Infof("[GetDBServerStatus] Getting status for database server with name: %s", name)
		resp, err := redbeamsDbServerClient.GetDatabaseServerStatusByName(database_servers.NewGetDatabaseServerStatusByNameParams().WithEnvironmentCrn(envCrn).WithName(name))
		if err != nil {
			commonutils.LogErrorAndExit(err)
		}
		status = resp.Payload
	}

	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	row := NewDetailsFromStatusResponse(status)
	output.Write(statusListHeader, row)
}

func CreateManagedDatabaseServer(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Create a managed database server")
	fileLocation := c.String(fl.FlDatabaseServerCreationFile.Name)

	log.Infof("[CreateManagedDBServer] Creating database server from file: %s", fileLocation)
	content := commonutils.ReadFile(fileLocation)
	var req model.AllocateDatabaseServerV4Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid JSON: %s`, err.Error())
		commonutils.LogErrorMessageAndExit(msg)
	}

	log.Infof("[CreateManagedDBServer] JSON read, creating database server with name: %s", req.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers
	resp, err := redbeamsDbServerClient.CreateDatabaseServer(database_servers.NewCreateDatabaseServerParams().WithBody(&req))
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

func RegisterDatabaseServer(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Register database server")
	fileLocation := c.String(fl.FlDatabaseServerRegistrationFile.Name)

	log.Infof("[RegisterDBServer] Registrating database server from file: %s", fileLocation)
	content := commonutils.ReadFile(fileLocation)
	var req model.DatabaseServerV4Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid JSON: %s`, err.Error())
		commonutils.LogErrorMessageAndExit(msg)
	}

	log.Infof("[RegisterDBServer] JSON read, registering database server with name: %s", *req.Name)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers
	resp, err := redbeamsDbServerClient.RegisterDatabaseServer(database_servers.NewRegisterDatabaseServerParams().WithBody(&req))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	server := resp.Payload

	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	row := NewDetailsFromResponse(server)
	output.Write(statusListHeader, row)
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

var dbListHeader = []string{"Name", "Description", "Crn", "EnvironmentCrn", "DatabaseVendor", "ConnectionURL"}

type dbDetails struct {
	Name           string `json:"Name" yaml:"Name"`
	Description    string `json:"Description" yaml:"Description"`
	CRN            string `json:"CRN" yaml:"CRN"`
	EnvironmentCrn string `json:"EnvironmentCrn" yaml:"EnvironmentCrn"`
	DatabaseEngine string `json:"DatabaseEngine" yaml:"DatabaseEngine"`
	ConnectionURL  string `json:"ConnectionURL" yaml:"ConnectionURL"`
	CreationDate   int64  `json:"CreationDate" yaml:"CreationDate"`
}

func (db *dbDetails) DataAsStringArray() []string {
	return []string{db.Name, db.Description, db.CRN, db.EnvironmentCrn, db.DatabaseEngine, db.ConnectionURL, string(db.CreationDate)}
}

func NewDetailsFromDbResponse(r *model.DatabaseV4Response) *dbDetails {
	details := &dbDetails{
		Name:           *r.Name,
		CRN:            r.Crn,
		EnvironmentCrn: r.EnvironmentCrn,
		ConnectionURL:  *r.ConnectionURL,
		CreationDate:   r.CreationDate,
	}

	if r.Description != nil {
		details.Description = *r.Description
	}
	if r.DatabaseEngine != nil {
		details.DatabaseEngine = *r.DatabaseEngine
	}

	return details
}

func ListDatabases(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "List databases in environment")
	envCrn := c.String(fl.FlEnvironmentCrn.Name)
	redbeamsDbClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.Databases

	log.Infof("[ListDBs] Listing databases in environment: %s", envCrn)
	resp, err := redbeamsDbClient.ListDatabases(databases.NewListDatabasesParams().WithEnvironmentCrn(envCrn), nil)
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}

	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}

	var tableRows []commonutils.Row

	for _, response := range resp.Payload.Responses {
		row := NewDetailsFromDbResponse(response)
		tableRows = append(tableRows, row)
	}
	output.WriteList(dbListHeader, tableRows)
}

func GetDatabase(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Get a database")
	envCrn := c.String(fl.FlEnvironmentCrn.Name)
	name := c.String(fl.FlName.Name)
	redbeamsDbClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.Databases

	log.Infof("[GetDB] Getting database with name: %s", name)
	resp, err := redbeamsDbClient.GetDatabase(databases.NewGetDatabaseParams().WithEnvironmentCrn(envCrn).WithName(name), nil)
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	db := resp.Payload

	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	row := NewDetailsFromDbResponse(db)
	output.Write(serverListHeader, row)
}

func CreateDatabase(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Create a database")
	fileLocation := c.String(fl.FlDatabaseCreationFile.Name)

	log.Infof("[CreateDB] Creating database from file: %s", fileLocation)
	content := commonutils.ReadFile(fileLocation)
	var req model.CreateDatabaseV4Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid JSON: %s`, err.Error())
		commonutils.LogErrorMessageAndExit(msg)
	}

	log.Infof("[CreateDB] JSON read, creating database with name: %s", *req.DatabaseName)
	redbeamsDbServerClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.DatabaseServers
	result, err := redbeamsDbServerClient.CreateDatabaseOnServer(database_servers.NewCreateDatabaseOnServerParams().WithBody(&req))
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}

	log.Infof("[DeleteDBServer] Created database with name: %s Details: %s", *req.DatabaseName, result)
}

func RegisterDatabase(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Register database")
	fileLocation := c.String(fl.FlDatabaseRegistrationFile.Name)

	log.Infof("[RegisterDB] Registering database server from file: %s", fileLocation)
	content := commonutils.ReadFile(fileLocation)
	var req model.DatabaseV4Request
	err := json.Unmarshal(content, &req)
	if err != nil {
		msg := fmt.Sprintf(`Invalid JSON: %s`, err.Error())
		commonutils.LogErrorMessageAndExit(msg)
	}

	log.Infof("[RegisterDB] JSON read, registering database with name: %s", *req.Name)
	redbeamsDbClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.Databases
	resp, err := redbeamsDbClient.RegisterDatabase(databases.NewRegisterDatabaseParams().WithBody(&req), nil)
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	db := resp.Payload

	output := commonutils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	row := NewDetailsFromDbResponse(db)
	output.Write(statusListHeader, row)
}

func DeleteDatabase(c *cli.Context) {
	defer commonutils.TimeTrack(time.Now(), "Delete a registered database")
	envCrn := c.String(fl.FlEnvironmentCrn.Name)
	name := c.String(fl.FlName.Name)
	redbeamsDbClient := ClientRedbeams(*oauth.NewRedbeamsClientFromContext(c)).Redbeams.Databases

	log.Infof("[DeleteDB] Deleting database with name: %s", name)
	result, err := redbeamsDbClient.DeleteDatabase(databases.NewDeleteDatabaseParams().WithEnvironmentCrn(envCrn).WithName(name), nil)
	if err != nil {
		commonutils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteDB] Deleted database with name: %s Details: %s", name, result)
}
