package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client/cluster"
	"github.com/hortonworks/hdc-cli/models"
	"time"
)

func (c *Cloudbreak) GetClusterConfig(id int64, inputs []*models.BlueprintParameter) []*models.BlueprintInput {
	defer timeTrack(time.Now(), "get cluster config by id")

	log.Infof("[GetClusterConfig] get cluster config for stack id: %d", id)
	resp, err := c.Cloudbreak.Cluster.GetConfigs(&cluster.GetConfigsParams{ID: id, Body: &models.ConfigsRequest{Requests: inputs}})

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[GetClusterConfig] found configs for stack id: %d", id)
	return resp.Payload.Inputs
}
