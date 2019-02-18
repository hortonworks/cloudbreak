package rds

import (
	"fmt"
	"github.com/hortonworks/cb-cli/dataplane/oauth"
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	v4db "github.com/hortonworks/cb-cli/dataplane/api/client/v4_workspace_id_databases"
	"github.com/hortonworks/cb-cli/dataplane/api/model"
	fl "github.com/hortonworks/cb-cli/dataplane/flags"
	"github.com/hortonworks/cb-cli/dataplane/types"
	"github.com/hortonworks/dp-cli-common/utils"
	"github.com/urfave/cli"
	"strings"
)

var rdsHeader = []string{"Name", "Description", "ConnectionURL", "DatabaseEngine", "Type", "Driver", "Environments"}

type rds struct {
	Name           string `json:"Name" yaml:"Name"`
	Description    string `json:"Description" yaml:"Description"`
	URL            string `json:"ConnectionURL" yaml:"ConnectionURL"`
	DatabaseEngine string `json:"DatabaseEngine" yaml:"DatabaseEngine"`
	Type           string `json:"Type" yaml:"Type"`
	Driver         string `json:"Driver" yaml:"Driver"`
	Environments   []string
}

type rdsOutDescribe struct {
	*rds
	ID string `json:"ID" yaml:"ID"`
}

func (r *rds) DataAsStringArray() []string {
	return []string{r.Name, r.Description, r.URL, r.DatabaseEngine, r.Type, r.Driver, strings.Join(r.Environments, ",")}
}

func (r *rdsOutDescribe) DataAsStringArray() []string {
	return append(r.rds.DataAsStringArray(), r.ID)
}

type rdsClient interface {
	ListDatabasesByWorkspace(params *v4db.ListDatabasesByWorkspaceParams) (*v4db.ListDatabasesByWorkspaceOK, error)
	CreateDatabaseInWorkspace(params *v4db.CreateDatabaseInWorkspaceParams) (*v4db.CreateDatabaseInWorkspaceOK, error)
	TestDatabaseConnectionInWorkspace(params *v4db.TestDatabaseConnectionInWorkspaceParams) (*v4db.TestDatabaseConnectionInWorkspaceOK, error)
}

func TestRdsByName(c *cli.Context) {
	log.Infof("[TestRdsByParams] test a database configuration")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	testRdsByNameImpl(
		cbClient.Cloudbreak.V4WorkspaceIDDatabases,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name))
}

func TestRdsByParams(c *cli.Context) {
	log.Infof("[TestRdsByParams] test a database configuration")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	testRdsByParamsImpl(
		cbClient.Cloudbreak.V4WorkspaceIDDatabases,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlRdsUserName.Name),
		c.String(fl.FlRdsPassword.Name),
		c.String(fl.FlRdsURL.Name),
		c.String(fl.FlRdsConnectorJarURLOptional.Name))
}

func testRdsByNameImpl(client rdsClient, workspaceID int64, name string) {
	defer utils.TimeTrack(time.Now(), "test database configuration by name")
	rdsRequest := &model.DatabaseTestV4Request{
		ExistingDatabaseName: name,
	}
	log.Infof("[testRdsByParamsImpl] sending test database configuration by parameters request")
	resp, err := client.TestDatabaseConnectionInWorkspace(v4db.NewTestDatabaseConnectionInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(rdsRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	if responseText := getEmptyIfNil(resp.Payload.Result); responseText != "connected" {
		utils.LogErrorMessageAndExit(fmt.Sprintf("database configuration test result: %s", responseText))
	}
}

func testRdsByParamsImpl(client rdsClient, workspaceID int64, username string, password string, URL string, jarURL string) {
	defer utils.TimeTrack(time.Now(), "test database configuration by parameters")
	rdsRequest := &model.DatabaseTestV4Request{
		Database: &model.DatabaseV4Request{
			Name:               &(&types.S{S: "testconnection"}).S,
			ConnectionUserName: &username,
			ConnectionPassword: &password,
			ConnectionURL:      &URL,
			Type:               &(&types.S{S: "testtype"}).S,
			ConnectorJarURL:    jarURL,
		},
	}
	log.Infof("[testRdsByParamsImpl] sending test database configuration by parameters request")
	resp, err := client.TestDatabaseConnectionInWorkspace(v4db.NewTestDatabaseConnectionInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(rdsRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	if responseText := getEmptyIfNil(resp.Payload.Result); responseText != "connected" {
		utils.LogErrorMessageAndExit(fmt.Sprintf("database configuration test result: %s", responseText))
	}
}

func CreateRdsOracle11(c *cli.Context) {
	log.Infof("[CreateRdsOracle11] creating a database configuration")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	createRdsImpl(
		cbClient.Cloudbreak.V4WorkspaceIDDatabases,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		c.String(fl.FlRdsUserName.Name),
		c.String(fl.FlRdsPassword.Name),
		c.String(fl.FlRdsURL.Name),
		c.String(fl.FlRdsType.Name),
		c.String(fl.FlRdsConnectorJarURLOptional.Name),
		utils.DelimitedStringToArray(c.String(fl.FlEnvironmentsOptional.Name), ","),
		&model.OracleParameters{
			Version: &(&types.S{S: "11"}).S,
		})
}

func CreateRdsOracle12(c *cli.Context) {
	log.Infof("[CreateRdsOracle12] creating a database configuration")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	createRdsImpl(
		cbClient.Cloudbreak.V4WorkspaceIDDatabases,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		c.String(fl.FlRdsUserName.Name),
		c.String(fl.FlRdsPassword.Name),
		c.String(fl.FlRdsURL.Name),
		c.String(fl.FlRdsType.Name),
		c.String(fl.FlRdsConnectorJarURLOptional.Name),
		utils.DelimitedStringToArray(c.String(fl.FlEnvironmentsOptional.Name), ","),
		&model.OracleParameters{
			Version: &(&types.S{S: "12"}).S,
		})
}

func CreateRds(c *cli.Context) {
	log.Infof("[CreateRds] creating a database configuration")
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	createRdsImpl(
		cbClient.Cloudbreak.V4WorkspaceIDDatabases,
		c.Int64(fl.FlWorkspaceOptional.Name),
		c.String(fl.FlName.Name),
		c.String(fl.FlDescriptionOptional.Name),
		c.String(fl.FlRdsUserName.Name),
		c.String(fl.FlRdsPassword.Name),
		c.String(fl.FlRdsURL.Name),
		c.String(fl.FlRdsType.Name),
		c.String(fl.FlRdsConnectorJarURLOptional.Name),
		utils.DelimitedStringToArray(c.String(fl.FlEnvironmentsOptional.Name), ","),
		nil)
}

func createRdsImpl(client rdsClient, workspaceID int64, name string, description string, username string, password string, URL string, rdsType string, jarURL string, environments []string, oracle *model.OracleParameters) {
	defer utils.TimeTrack(time.Now(), "create database")
	rdsRequest := &model.DatabaseV4Request{
		Name:               &name,
		Description:        &description,
		ConnectionUserName: &username,
		ConnectionPassword: &password,
		ConnectionURL:      &URL,
		Type:               &rdsType,
		ConnectorJarURL:    jarURL,
		Environments:       environments,
	}
	if oracle != nil {
		rdsRequest.Oracle = oracle
	}
	var rdsResponse *model.DatabaseV4Response
	log.Infof("[createRdsImpl] sending create database request")
	resp, err := client.CreateDatabaseInWorkspace(v4db.NewCreateDatabaseInWorkspaceParams().WithWorkspaceID(workspaceID).WithBody(rdsRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	rdsResponse = resp.Payload

	log.Infof("[createRdsImpl] rds created: %s (id: %d)", *rdsResponse.Name, rdsResponse.ID)
}

func DeleteRds(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "delete a database configuration")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	rdsName := c.String(fl.FlName.Name)
	log.Infof("[DeleteRds] delete database configuration by name: %s", rdsName)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	if _, err := cbClient.Cloudbreak.V4WorkspaceIDDatabases.DeleteDatabaseInWorkspace(v4db.NewDeleteDatabaseInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(rdsName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteRds] database configuration deleted: %s", rdsName)
	return nil
}

func DescribeRds(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "describe a database configuration")
	log.Infof("[DescribeRds] Describes a database configuration")
	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	rdsName := c.String(fl.FlName.Name)
	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V4WorkspaceIDDatabases.GetDatabaseInWorkspace(v4db.NewGetDatabaseInWorkspaceParams().WithWorkspaceID(workspaceID).WithName(rdsName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	r := resp.Payload

	output.Write(append(rdsHeader, "ID"), &rdsOutDescribe{
		&rds{
			Name:           *r.Name,
			Description:    utils.SafeStringConvert(r.Description),
			URL:            *r.ConnectionURL,
			DatabaseEngine: *r.DatabaseEngine,
			Type:           getEmptyIfNil(r.Type),
			Driver:         getEmptyIfNil(r.ConnectionDriver),
			Environments:   r.Environments,
		}, strconv.FormatInt(r.ID, 10)})

}

func AttachRdsToEnvs(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "attach rds to environments")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	rdsName := c.String(fl.FlName.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironments.Name), ",")
	log.Infof("[AttachRdsToEnvs] attach rds config '%s' to environments: %s", rdsName, environments)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	attachRequest := v4db.NewAttachDatabaseToEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(rdsName).WithBody(&model.EnvironmentNames{EnvironmentNames: environments})
	response, err := cbClient.Cloudbreak.V4WorkspaceIDDatabases.AttachDatabaseToEnvironments(attachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	rds := response.Payload
	log.Infof("[AttachRdsToEnvs] rds config '%s' is now attached to the following environments: %s", *rds.Name, rds.Environments)
}

func DetachRdsFromEnvs(c *cli.Context) {
	defer utils.TimeTrack(time.Now(), "detach rds from environments")

	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	rdsName := c.String(fl.FlName.Name)
	environments := utils.DelimitedStringToArray(c.String(fl.FlEnvironments.Name), ",")
	log.Infof("[DetachRdsFromEnvs] detach rds config '%s' from environments: %s", rdsName, environments)

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)
	detachRequest := v4db.NewDetachDatabaseFromEnvironmentsParams().WithWorkspaceID(workspaceID).WithName(rdsName).WithBody(&model.EnvironmentNames{EnvironmentNames: environments})
	response, err := cbClient.Cloudbreak.V4WorkspaceIDDatabases.DetachDatabaseFromEnvironments(detachRequest)
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	rds := response.Payload
	log.Infof("[DetachRdsFromEnvs] rds config '%s' is now attached to the following environments: %s", *rds.Name, rds.Environments)
}

func ListAllRds(c *cli.Context) error {
	defer utils.TimeTrack(time.Now(), "list database configurations")

	cbClient := oauth.NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(fl.FlOutputOptional.Name)}
	workspaceID := c.Int64(fl.FlWorkspaceOptional.Name)
	return listAllRdsImpl(cbClient.Cloudbreak.V4WorkspaceIDDatabases, output.WriteList, workspaceID)
}

func listAllRdsImpl(rdsClient rdsClient, writer func([]string, []utils.Row), workspaceID int64) error {
	resp, err := rdsClient.ListDatabasesByWorkspace(v4db.NewListDatabasesByWorkspaceParams().WithWorkspaceID(workspaceID).WithAttachGlobal(&(&types.B{B: true}).B))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, r := range resp.Payload.Responses {
		row := &rds{
			Name:           *r.Name,
			Description:    utils.SafeStringConvert(r.Description),
			URL:            *r.ConnectionURL,
			DatabaseEngine: *r.DatabaseEngine,
			Type:           getEmptyIfNil(r.Type),
			Driver:         getEmptyIfNil(r.ConnectionDriver),
			Environments:   r.Environments,
		}
		tableRows = append(tableRows, row)
	}

	writer(rdsHeader, tableRows)
	return nil
}

func getEmptyIfNil(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}
