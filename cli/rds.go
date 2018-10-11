package cli

import (
	"fmt"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/cb-cli/cli/types"
	"github.com/hortonworks/cb-cli/cli/utils"
	"github.com/hortonworks/cb-cli/client_cloudbreak/v1rdsconfigs"
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

func (r *rds) DataAsStringArray() []string {
	return []string{r.Name, r.URL, r.DatabaseEngine, r.Type, r.Driver}
}

type rdsClient interface {
	GetPublicsRds(params *v1rdsconfigs.GetPublicsRdsParams) (*v1rdsconfigs.GetPublicsRdsOK, error)
	PostPrivateRds(params *v1rdsconfigs.PostPrivateRdsParams) (*v1rdsconfigs.PostPrivateRdsOK, error)
	PostPublicRds(params *v1rdsconfigs.PostPublicRdsParams) (*v1rdsconfigs.PostPublicRdsOK, error)
	TestRdsConnection(params *v1rdsconfigs.TestRdsConnectionParams) (*v1rdsconfigs.TestRdsConnectionOK, error)
}

func TestRdsByName(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[TestRdsByParams] test a database configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	testRdsByNameImpl(
		cbClient.Cloudbreak.V1rdsconfigs,
		c.String(FlName.Name))
}

func TestRdsByParams(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[TestRdsByParams] test a database configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	testRdsByParamsImpl(
		cbClient.Cloudbreak.V1rdsconfigs,
		c.String(FlRdsUserName.Name),
		c.String(FlRdsPassword.Name),
		c.String(FlRdsURL.Name),
		c.String(FlRdsType.Name))
}

func testRdsByNameImpl(client rdsClient, name string) {
	defer utils.TimeTrack(time.Now(), "test database configuration by name")
	rdsRequest := &models_cloudbreak.RdsTestRequest{
		Name: name,
	}
	log.Infof("[testRdsByParamsImpl] sending test database configuration by parameters request")
	resp, err := client.TestRdsConnection(v1rdsconfigs.NewTestRdsConnectionParams().WithBody(rdsRequest))
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	if responseText := getEmptyIfNil(resp.Payload.ConnectionResult); responseText != "connected" {
		utils.LogErrorMessageAndExit(fmt.Sprintf("database configuration test result: %s", responseText))
	}
}

func testRdsByParamsImpl(client rdsClient, username string, password string, URL string, rdsType string) {
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
	resp, err := client.TestRdsConnection(v1rdsconfigs.NewTestRdsConnectionParams().WithBody(rdsRequest))
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
		cbClient.Cloudbreak.V1rdsconfigs,
		c.String(FlName.Name),
		c.String(FlRdsUserName.Name),
		c.String(FlRdsPassword.Name),
		c.String(FlRdsURL.Name),
		c.String(FlRdsType.Name),
		c.String(FlRdsConnectorJarURLOptional.Name),
		c.Bool(FlPublicOptional.Name),
		&models_cloudbreak.Oracle{
			Version: &(&types.S{S: "11"}).S,
		})
}

func CreateRdsOracle12(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[CreateRdsOracle12] creating a database configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	createRdsImpl(
		cbClient.Cloudbreak.V1rdsconfigs,
		c.String(FlName.Name),
		c.String(FlRdsUserName.Name),
		c.String(FlRdsPassword.Name),
		c.String(FlRdsURL.Name),
		c.String(FlRdsType.Name),
		c.String(FlRdsConnectorJarURLOptional.Name),
		c.Bool(FlPublicOptional.Name),
		&models_cloudbreak.Oracle{
			Version: &(&types.S{S: "12"}).S,
		})
}

func CreateRds(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[CreateRds] creating a database configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	createRdsImpl(
		cbClient.Cloudbreak.V1rdsconfigs,
		c.String(FlName.Name),
		c.String(FlRdsUserName.Name),
		c.String(FlRdsPassword.Name),
		c.String(FlRdsURL.Name),
		c.String(FlRdsType.Name),
		c.String(FlRdsConnectorJarURLOptional.Name),
		c.Bool(FlPublicOptional.Name),
		nil)
}

func createRdsImpl(client rdsClient, name string, username string, password string, URL string, rdsType string, jarURL string, public bool, oracle *models_cloudbreak.Oracle) {
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
	if public {
		log.Infof("[createRdsImpl] sending create public database request")
		resp, err := client.PostPublicRds(v1rdsconfigs.NewPostPublicRdsParams().WithBody(rdsRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		rdsResponse = resp.Payload
	} else {
		log.Infof("[createRdsImpl] sending create private database request")
		resp, err := client.PostPrivateRds(v1rdsconfigs.NewPostPrivateRdsParams().WithBody(rdsRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		rdsResponse = resp.Payload
	}
	log.Infof("[createRdsImpl] rds created: %s (id: %d)", *rdsResponse.Name, rdsResponse.ID)
}

func DeleteRds(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete a database configuration")

	rdsName := c.String(FlName.Name)
	log.Infof("[DeleteRds] delete database configuration by name: %s", rdsName)
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	if err := cbClient.Cloudbreak.V1rdsconfigs.DeletePublicRds(v1rdsconfigs.NewDeletePublicRdsParams().WithName(rdsName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteRds] database configuration deleted: %s", rdsName)
	return nil
}

func ListAllRds(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list database configurations")

	cbClient := NewCloudbreakHTTPClientFromContext(c)

	output := utils.Output{Format: c.String(FlOutputOptional.Name)}
	return listAllRdsImpl(cbClient.Cloudbreak.V1rdsconfigs, output.WriteList)
}

func listAllRdsImpl(rdsClient rdsClient, writer func([]string, []utils.Row)) error {
	resp, err := rdsClient.GetPublicsRds(v1rdsconfigs.NewGetPublicsRdsParams())
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
