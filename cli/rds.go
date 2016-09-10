package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/rdsconfigs"
	"github.com/hortonworks/hdc-cli/models"
	"time"
)

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
