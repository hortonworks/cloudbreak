package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_cloudbreak/cluster"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"time"
)

func (c *Cloudbreak) GetClusterConfig(id int64, inputs []*models_cloudbreak.BlueprintParameter) []*models_cloudbreak.BlueprintInput {
	defer timeTrack(time.Now(), "get cluster config by id")

	log.Infof("[GetClusterConfig] get cluster config for stack id: %d", id)
	resp, err := c.Cloudbreak.Cluster.GetConfigs(&cluster.GetConfigsParams{ID: id, Body: &models_cloudbreak.ConfigsRequest{Requests: inputs}})

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[GetClusterConfig] found configs for stack id: %d", id)
	return resp.Payload.Inputs
}
