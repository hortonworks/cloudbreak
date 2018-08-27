package cli

import (
	"fmt"
	"strconv"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v3_organization_id_rdsconfigs"
	"github.com/hortonworks/cb-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var rdsHeader = []string{"Name", "ConnectionURL", "DatabaseEngine", "Type", "Driver"}

type rds struct {
	Name           string `json:"Name" yaml:"Name"`
	URL            string `json:"ConnectionURL" yaml:"ConnectionURL"`
	DatabaseEngine string `json:"DatabaseEngine" yaml:"DatabaseEngine"`
	Type           string `json:"Type" yaml:"Type"`
	Driver         string `json:"Driver" yaml:"Driver"`
}

type rdsOutDescribe struct {
	*rds
	ID string `json:"ID" yaml:"ID"`
}

func (r *rds) DataAsStringArray() []string {
	return []string{r.Name, r.URL, r.DatabaseEngine, r.Type, r.Driver}
}

func (r *rdsOutDescribe) DataAsStringArray() []string {
	return append(r.rds.DataAsStringArray(), r.ID)
}

type rdsClient interface {
	ListRdsConfigsByOrganization(params *v3_organization_id_rdsconfigs.ListRdsConfigsByOrganizationParams) (*v3_organization_id_rdsconfigs.ListRdsConfigsByOrganizationOK, error)
	CreateRdsConfigInOrganization(params *v3_organization_id_rdsconfigs.CreateRdsConfigInOrganizationParams) (*v3_organization_id_rdsconfigs.CreateRdsConfigInOrganizationOK, error)
	TestRdsConnectionInOrganization(params *v3_organization_id_rdsconfigs.TestRdsConnectionInOrganizationParams) (*v3_organization_id_rdsconfigs.TestRdsConnectionInOrganizationOK, error)
}

func TestRdsByName(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[TestRdsByParams] test a database configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	testRdsByNameImpl(
		cbClient.Cloudbreak.V3OrganizationIDRdsconfigs,
		c.Int64(FlOrganizationOptional.Name),
		c.String(FlName.Name))
}

func TestRdsByParams(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[TestRdsByParams] test a database configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	testRdsByParamsImpl(
		cbClient.Cloudbreak.V3OrganizationIDRdsconfigs,
		c.Int64(FlOrganizationOptional.Name),
		c.String(FlRdsUserName.Name),
		c.String(FlRdsPassword.Name),
		c.String(FlRdsURL.Name),
		c.String(FlRdsType.Name))
}

func testRdsByNameImpl(client rdsClient, orgID int64, name string) {
	defer utils.TimeTrack(time.Now(), "test database configuration by name")
	rdsRequest := &models_cloudbreak.RdsTestRequest{
		Name: name,
	}
	log.Infof("[testRdsByParamsImpl] sending test database configuration by parameters request")
	resp, err := client.TestRdsConnectionInOrganization(v3_organization_id_rdsconfigs.NewTestRdsConnectionInOrganizationParams().WithOrganizationID(orgID).WithBody(rdsRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	if responseText := getEmptyIfNil(resp.Payload.ConnectionResult); responseText != "connected" {
		utils.LogErrorMessageAndExit(fmt.Sprintf("database configuration test result: %s", responseText))
	}
}

func testRdsByParamsImpl(client rdsClient, orgID int64, username string, password string, URL string, rdsType string) {
	defer utils.TimeTrack(time.Now(), "test database configuration by parameters")
	rdsRequest := &models_cloudbreak.RdsTestRequest{
		RdsConfig: &models_cloudbreak.RdsConfig{
			Name:               &(&types.S{S: "testconnection"}).S,
			ConnectionUserName: &username,
			ConnectionPassword: &password,
			ConnectionURL:      &URL,
			Type:               &rdsType,
		},
	}
	log.Infof("[testRdsByParamsImpl] sending test database configuration by parameters request")
	resp, err := client.TestRdsConnectionInOrganization(v3_organization_id_rdsconfigs.NewTestRdsConnectionInOrganizationParams().WithOrganizationID(orgID).WithBody(rdsRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	if responseText := getEmptyIfNil(resp.Payload.ConnectionResult); responseText != "connected" {
		utils.LogErrorMessageAndExit(fmt.Sprintf("database configuration test result: %s", responseText))
	}
}

func CreateRdsOracle11(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[CreateRdsOracle11] creating a database configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	createRdsImpl(
		cbClient.Cloudbreak.V3OrganizationIDRdsconfigs,
		c.Int64(FlOrganizationOptional.Name),
		c.String(FlName.Name),
		c.String(FlRdsUserName.Name),
		c.String(FlRdsPassword.Name),
		c.String(FlRdsURL.Name),
		c.String(FlRdsType.Name),
		c.String(FlRdsConnectorJarURLOptional.Name),
		&models_cloudbreak.Oracle{
			Version: &(&types.S{S: "11"}).S,
		})
}

func CreateRdsOracle12(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[CreateRdsOracle12] creating a database configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	createRdsImpl(
		cbClient.Cloudbreak.V3OrganizationIDRdsconfigs,
		c.Int64(FlOrganizationOptional.Name),
		c.String(FlName.Name),
		c.String(FlRdsUserName.Name),
		c.String(FlRdsPassword.Name),
		c.String(FlRdsURL.Name),
		c.String(FlRdsType.Name),
		c.String(FlRdsConnectorJarURLOptional.Name),
		&models_cloudbreak.Oracle{
			Version: &(&types.S{S: "12"}).S,
		})
}

func CreateRds(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[CreateRds] creating a database configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	createRdsImpl(
		cbClient.Cloudbreak.V3OrganizationIDRdsconfigs,
		c.Int64(FlOrganizationOptional.Name),
		c.String(FlName.Name),
		c.String(FlRdsUserName.Name),
		c.String(FlRdsPassword.Name),
		c.String(FlRdsURL.Name),
		c.String(FlRdsType.Name),
		c.String(FlRdsConnectorJarURLOptional.Name),
		nil)
}

func createRdsImpl(client rdsClient, orgID int64, name string, username string, password string, URL string, rdsType string, jarURL string, oracle *models_cloudbreak.Oracle) {
	defer utils.TimeTrack(time.Now(), "create database")
	rdsRequest := &models_cloudbreak.RdsConfig{
		Name:               &name,
		ConnectionUserName: &username,
		ConnectionPassword: &password,
		ConnectionURL:      &URL,
		Type:               &rdsType,
		ConnectorJarURL:    jarURL,
	}
	if oracle != nil {
		rdsRequest.Oracle = oracle
	}
	var rdsResponse *models_cloudbreak.RDSConfigResponse
	log.Infof("[createRdsImpl] sending create database request")
	resp, err := client.CreateRdsConfigInOrganization(v3_organization_id_rdsconfigs.NewCreateRdsConfigInOrganizationParams().WithOrganizationID(orgID).WithBody(rdsRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	rdsResponse = resp.Payload

	log.Infof("[createRdsImpl] rds created: %s (id: %d)", *rdsResponse.Name, rdsResponse.ID)
}

func DeleteRds(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete a database configuration")

	orgID := c.Int64(FlOrganizationOptional.Name)
	rdsName := c.String(FlName.Name)
	log.Infof("[DeleteRds] delete database configuration by name: %s", rdsName)
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	if _, err := cbClient.Cloudbreak.V3OrganizationIDRdsconfigs.DeleteRdsConfigInOrganization(v3_organization_id_rdsconfigs.NewDeleteRdsConfigInOrganizationParams().WithOrganizationID(orgID).WithName(rdsName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteRds] database configuration deleted: %s", rdsName)
	return nil
}

func DescribeRds(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "describe a database configuration")
	log.Infof("[DescribeRds] Describes a database configuration")
	output := utils.Output{Format: c.String(FlOutputOptional.Name)}

	orgID := c.Int64(FlOrganizationOptional.Name)
	rdsName := c.String(FlName.Name)
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	resp, err := cbClient.Cloudbreak.V3OrganizationIDRdsconfigs.GetRdsConfigInOrganization(v3_organization_id_rdsconfigs.NewGetRdsConfigInOrganizationParams().WithOrganizationID(orgID).WithName(rdsName))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	r := resp.Payload

	output.Write(append(rdsHeader, "ID"), &rdsOutDescribe{
		&rds{
			Name:           *r.Name,
			URL:            *r.ConnectionURL,
			DatabaseEngine: *r.DatabaseEngine,
			Type:           getEmptyIfNil(r.Type),
			Driver:         getEmptyIfNil(r.ConnectionDriver),
		}, strconv.FormatInt(r.ID, 10)})

}

func ListAllRds(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list database configurations")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	orgID := c.Int64(FlOrganizationOptional.Name)
	return listAllRdsImpl(cbClient.Cloudbreak.V3OrganizationIDRdsconfigs, output.WriteList, orgID)
}

func listAllRdsImpl(rdsClient rdsClient, writer func([]string, []utils.Row), orgID int64) error {
	resp, err := rdsClient.ListRdsConfigsByOrganization(v3_organization_id_rdsconfigs.NewListRdsConfigsByOrganizationParams().WithOrganizationID(orgID))
	if err != nil {
		utils.LogErrorAndExit(err)
	}

	var tableRows []utils.Row
	for _, r := range resp.Payload {
		row := &rds{
			Name:           *r.Name,
			URL:            *r.ConnectionURL,
			DatabaseEngine: *r.DatabaseEngine,
			Type:           getEmptyIfNil(r.Type),
			Driver:         getEmptyIfNil(r.ConnectionDriver),
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
