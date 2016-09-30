package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/rdsconfigs"
	"github.com/hortonworks/hdc-cli/models"
	"github.com/urfave/cli"
	"strings"
	"time"
)

var RdsHeader []string = []string{"Name", "Username", "URL", "DatabaseType", "HDP Version"}

type RdsConfig struct {
	MetaStore
	HDPVersion string `json:"HDPVersion" yaml:"HDPVersion"`
}

func (r *RdsConfig) DataAsStringArray() []string {
	return []string{r.Name, r.Username, r.URL, r.DatabaseType, r.HDPVersion}
}

func (c *Cloudbreak) GetRDSConfigByName(name string) models.RDSConfigResponse {
	defer timeTrack(time.Now(), "get rds config by name")
	log.Infof("[GetRDSConfigByName] get rds config by name: %s", name)

	resp, err := c.Cloudbreak.Rdsconfigs.GetRdsconfigsAccountName(&rdsconfigs.GetRdsconfigsAccountNameParams{Name: name})

	if err != nil {
		logErrorAndExit(c.GetRDSConfigByName, err.Error())
	}

	rdsConfig := *resp.Payload
	log.Infof("[GetRDSConfigByName] found rds config, name: %s", rdsConfig.Name)
	return rdsConfig
}

func (c *Cloudbreak) GetRDSConfigById(id int64) *models.RDSConfigResponse {
	defer timeTrack(time.Now(), "get rds config by id")
	log.Infof("[GetRDSConfigById] get rds config by id: %d", id)

	resp, err := c.Cloudbreak.Rdsconfigs.GetRdsconfigsID(&rdsconfigs.GetRdsconfigsIDParams{ID: id})

	if err != nil {
		logErrorAndExit(c.GetRDSConfigById, err.Error())
	}

	rdsConfig := resp.Payload
	log.Infof("[GetRDSConfigById] found rds config, name: %s", rdsConfig.Name)
	return rdsConfig
}

func ListRDSConfigs(c *cli.Context) error {
	defer timeTrack(time.Now(), "list rds configs")

	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	resp, err := oAuth2Client.Cloudbreak.Rdsconfigs.GetRdsconfigsAccount(&rdsconfigs.GetRdsconfigsAccountParams{})

	if err != nil {
		logErrorAndExit(ListRDSConfigs, err.Error())
	}

	var tableRows []Row
	for _, rds := range resp.Payload {
		row := &RdsConfig{
			HDPVersion: rds.HdpVersion,
			MetaStore: MetaStore{
				Name:         rds.Name,
				Username:     rds.ConnectionUserName,
				Password:     "",
				URL:          rawRdsUrl(rds.ConnectionURL),
				DatabaseType: rds.DatabaseType,
			},
		}
		tableRows = append(tableRows, row)
	}
	output := Output{Format: c.String(FlOutput.Name)}
	output.WriteList(RdsHeader, tableRows)
	return nil
}

func CreateRDSConfig(c *cli.Context) error {
	checkRequiredFlags(c, CreateCredential)
	defer timeTrack(time.Now(), "create rds config")

	log.Infof("[CreateRDSConfig] create RDS config with name: %s", c.String(FlRdsName.Name))
	oAuth2Client := NewOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	validate := false
	rdsConfig := models.RDSConfig{
		Name:               c.String(FlRdsName.Name),
		ConnectionUserName: c.String(FlRdsUsername.Name),
		ConnectionPassword: c.String(FlRdsPassword.Name),
		ConnectionURL:      extendRdsUrl(c.String(FlRdsUrl.Name), c.String(FlRdsDbType.Name)),
		DatabaseType:       c.String(FlRdsDbType.Name),
		Validated:          &validate,
		HdpVersion:         c.String(FlHdpVersion.Name),
	}

	resp, err := oAuth2Client.Cloudbreak.Rdsconfigs.PostRdsconfigsAccount(&rdsconfigs.PostRdsconfigsAccountParams{&rdsConfig})

	if err != nil {
		logErrorAndExit(CreateRDSConfig, err.Error())
	}

	log.Infof("[CreateRDSConfig] RDS config created, id: %d", resp.Payload.ID)
	return nil
}

func extendRdsUrl(url string, dbType string) string {
	if strings.Contains(url, "jdbc:") {
		return url
	}
	if dbType == POSTGRES {
		return "jdbc:postgresql://" + url
	} else {
		return "jdbc:mysql://" + url
	}
}

func rawRdsUrl(url string) string {
	if strings.Contains(url, "jdbc") {
		if strings.Contains(url, "postgresql") {
			return url[len("jdbc:postgresql://"):]
		}
		return url[len("jdbc:mysql://"):]
	}
	return url
}
