package cli

import (
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
}

func CreateRds(c *cli.Context) {
	checkRequiredFlagsAndArguments(c)
	log.Infof("[CreateRds] creating an rds configuration")
	cbClient := NewCloudbreakHTTPClientFromContext(c)
	createRdsImpl(
		cbClient.Cloudbreak.V1rdsconfigs,
		c.String(FlName.Name),
		c.String(FlRdsUserName.Name),
		c.String(FlRdsPassword.Name),
		c.String(FlRdsURL.Name),
		c.String(FlRdsDatabaseEngine.Name),
		c.String(FlRdsDriver.Name),
		c.String(FlRdsType.Name),
		c.Bool(FlRdsValidatedOptional.Name),
		c.Bool(FlPublicOptional.Name))
}

func createRdsImpl(client rdsClient, name string, username string, password string, URL string, databaseEngine string, driver string, rdsType string, notValidated bool, public bool) {
	defer utils.TimeTrack(time.Now(), "create rds")
	rdsRequest := &models_cloudbreak.RdsConfig{
		Name:               &name,
		ConnectionUserName: &username,
		ConnectionPassword: &password,
		ConnectionURL:      &URL,
		ConnectionDriver:   &driver,
		DatabaseEngine:     &databaseEngine,
		Type:               &rdsType,
	}
	if notValidated {
		rdsRequest.Validated = &(&types.B{B: false}).B
	}
	var rdsResponse *models_cloudbreak.RDSConfigResponse
	if public {
		log.Infof("[createRdsImpl] sending create public rds request")
		resp, err := client.PostPublicRds(v1rdsconfigs.NewPostPublicRdsParams().WithBody(rdsRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		rdsResponse = resp.Payload
	} else {
		log.Infof("[createRdsImpl] sending create private rds request")
		resp, err := client.PostPrivateRds(v1rdsconfigs.NewPostPrivateRdsParams().WithBody(rdsRequest))
		if err != nil {
			utils.LogErrorAndExit(err)
		}
		rdsResponse = resp.Payload
	}
	log.Infof("[createRdsImpl] rds created: %s (id: %d)", rdsResponse.Name, rdsResponse.ID)
}

func DeleteRds(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "delete an rds")

	rdsName := c.String(FlName.Name)
	log.Infof("[DeleteRds] delete rds config by name: %s", rdsName)
	cbClient := NewCloudbreakHTTPClientFromContext(c)

	if err := cbClient.Cloudbreak.V1rdsconfigs.DeletePublicRds(v1rdsconfigs.NewDeletePublicRdsParams().WithName(rdsName)); err != nil {
		utils.LogErrorAndExit(err)
	}
	log.Infof("[DeleteRds] rds config deleted: %s", rdsName)
	return nil
}

func ListAllRds(c *cli.Context) error {
	checkRequiredFlagsAndArguments(c)
	defer utils.TimeTrack(time.Now(), "list rds configs")

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
