package cli

import (
	"strings"
	"time"

	"errors"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/rdsconfigs"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
)

var RdsHeader []string = []string{"Name", "URL", "DatabaseType", "HDP Version", "Type"}

type RdsConfig struct {
	MetaStore
	Type       string `json:"Type" yaml:"Type"`
	HDPVersion string `json:"HDPVersion" yaml:"HDPVersion"`
}

func (r *RdsConfig) DataAsStringArray() []string {
	return []string{r.Name, r.URL, r.DatabaseType, r.HDPVersion, r.Type}
}

func (c *Cloudbreak) GetRDSConfigByName(name string) models_cloudbreak.RDSConfigResponse {
	defer timeTrack(time.Now(), "get rds config by name")
	log.Infof("[GetRDSConfigByName] get rds config by name: %s", name)

	resp, err := c.Cloudbreak.Rdsconfigs.GetPublicRds(&rdsconfigs.GetPublicRdsParams{Name: name})

	if err != nil {
		logErrorAndExit(err)
	}

	rdsConfig := *resp.Payload
	log.Infof("[GetRDSConfigByName] found rds config, name: %s", rdsConfig.Name)
	return rdsConfig
}

func (c *Cloudbreak) GetRDSConfigById(id int64) *models_cloudbreak.RDSConfigResponse {
	defer timeTrack(time.Now(), "get rds config by id")
	log.Infof("[GetRDSConfigById] get rds config by id: %d", id)

	resp, err := c.Cloudbreak.Rdsconfigs.GetRds(&rdsconfigs.GetRdsParams{ID: id})

	if err != nil {
		logErrorAndExit(err)
	}

	rdsConfig := resp.Payload
	log.Infof("[GetRDSConfigById] found rds config, name: %s", rdsConfig.Name)
	return rdsConfig
}

func ListRDSConfigs(c *cli.Context) error {
	defer timeTrack(time.Now(), "list rds configs")

	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	output := Output{Format: c.String(FlOutput.Name)}
	return listRDSConfigsImpl(oAuth2Client.Cloudbreak.Rdsconfigs.GetPublicsRds, output.WriteList)
}

func listRDSConfigsImpl(getConfigs func(*rdsconfigs.GetPublicsRdsParams) (*rdsconfigs.GetPublicsRdsOK, error), writer func([]string, []Row)) error {
	resp, err := getConfigs(&rdsconfigs.GetPublicsRdsParams{})

	if err != nil {
		logErrorAndExit(err)
	}

	var tableRows []Row
	for _, rds := range resp.Payload {
		row := &RdsConfig{
			HDPVersion: rds.HdpVersion,
			Type:       *rds.Type,
			MetaStore: MetaStore{
				Name:         rds.Name,
				URL:          rawRdsUrl(rds.ConnectionURL),
				DatabaseType: rds.DatabaseType,
			},
		}
		tableRows = append(tableRows, row)
	}

	writer(RdsHeader, tableRows)
	return nil
}

func CreateRDSConfig(c *cli.Context) error {
	checkRequiredFlags(c)
	defer timeTrack(time.Now(), "create rds config")

	if c.String(FlRdsDbType.Name) != POSTGRES {
		logErrorAndExit(errors.New("Invalid DB type. Accepted value: " + POSTGRES))
	}

	rdsType := strings.ToUpper(c.String(FlRdsType.Name))
	if len(rdsType) == 0 || (rdsType != HIVE_RDS && rdsType != DRUID_RDS) {
		logErrorAndExit(errors.New("Invalid RDS type, accepted values: HIVE,DRUID"))
	}

	if err := validateHDPVersion(c.String(FlHdpVersion.Name)); err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[CreateRDSConfig] create RDS config with name: %s", c.String(FlRdsName.Name))
	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	return createRDSConfigImpl(rdsType, c.String, oAuth2Client.Cloudbreak.Rdsconfigs.PostPublicRds)
}

func createRDSConfigImpl(rdsType string, finder func(string) string, postConfig func(*rdsconfigs.PostPublicRdsParams) (*rdsconfigs.PostPublicRdsOK, error)) error {
	validate := false
	rdsConfig := models_cloudbreak.RDSConfig{
		Name:               finder(FlRdsName.Name),
		ConnectionUserName: finder(FlRdsUsername.Name),
		ConnectionPassword: finder(FlRdsPassword.Name),
		ConnectionURL:      extendRdsUrl(finder(FlRdsUrl.Name)),
		DatabaseType:       finder(FlRdsDbType.Name),
		Validated:          &validate,
		HdpVersion:         finder(FlHdpVersion.Name),
		Type:               &rdsType,
	}

	resp, err := postConfig(&rdsconfigs.PostPublicRdsParams{Body: &rdsConfig})

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[CreateRDSConfig] RDS config created, id: %d", resp.Payload.ID)
	return nil
}

func DeleteRDSConfig(c *cli.Context) error {
	checkRequiredFlags(c)
	defer timeTrack(time.Now(), "delete rds config")

	log.Infof("[DeleteRDSConfig] delete RDS config by name: %s", c.String(FlRdsName.Name))
	oAuth2Client := NewCloudbreakOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	deleteRDSConfigImpl(c.String, oAuth2Client.Cloudbreak.Rdsconfigs.DeletePublicRds)
	return nil
}

func deleteRDSConfigImpl(finder func(string) string, deleteRDSConfig func(params *rdsconfigs.DeletePublicRdsParams) error) {
	rdsName := finder(FlRdsName.Name)

	if err := deleteRDSConfig(&rdsconfigs.DeletePublicRdsParams{Name: rdsName}); err != nil {
		logErrorAndExit(err)
	}
	log.Infof("[DeleteRDSConfig] RDS config deleted: %s", rdsName)
}

func createRDSRequest(metastore MetaStore, rdsType string, hdpVersion string, properties []*models_cloudbreak.RdsConfigProperty) *models_cloudbreak.RDSConfig {
	validate := false
	return &models_cloudbreak.RDSConfig{
		Name:               metastore.Name,
		ConnectionUserName: metastore.Username,
		ConnectionPassword: metastore.Password,
		ConnectionURL:      extendRdsUrl(metastore.URL),
		DatabaseType:       metastore.DatabaseType,
		HdpVersion:         hdpVersion,
		Validated:          &validate,
		Type:               &rdsType,
		Properties:         properties,
	}
}

func extendRdsUrl(url string) string {
	if strings.Contains(url, "jdbc:") {
		return url
	}
	return "jdbc:postgresql://" + url
}

func rawRdsUrl(url string) string {
	if strings.Contains(url, "jdbc") {
		if strings.Contains(url, "postgresql") {
			return url[len("jdbc:postgresql://"):]
		}
	}
	return url
}
