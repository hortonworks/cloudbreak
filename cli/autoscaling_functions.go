package cli

import (
	"errors"
	"fmt"
	"time"

	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/cli/utils"
	"github.com/hortonworks/hdc-cli/client_autoscale/clusters"
	"github.com/hortonworks/hdc-cli/models_autoscale"
)

func (as *Autoscaling) deleteCluster(name string, stackID int64) {
	defer utils.TimeTrack(time.Now(), "delete autoscaling cluster")

	log.Infof("[deleteCluster] delete autoscaling cluster, name: %s", name)
	if asClusterId, err := as.getAutoscalingClusterIdByStackId(name, stackID); err == nil {
		as.AutoScaling.Clusters.DeleteCluster(clusters.NewDeleteClusterParams().WithClusterID(asClusterId))
	} else {
		log.Infof("[deleteCluster] autoscaling is not enabled for cluster: %s", name)
	}
}

func (as *Autoscaling) getAutoscalingClusterIdByStackId(name string, stackID int64) (int64, error) {
	asCluster, err := as.getAutoscalingClusterByStackId(name, stackID)
	if err != nil {
		return 0, err
	}
	return asCluster.ID, nil
}

func (as *Autoscaling) getAutoscalingClusterByStackId(name string, stackID int64) (*models_autoscale.ClusterSummary, error) {
	log.Infof("[getAutoscalingClusterByStackId] retrieving autoscaling clusters, name:, %s", name)
	resp, err := as.AutoScaling.Clusters.GetClusters(clusters.NewGetClustersParams())
	if err != nil {
		utils.LogErrorAndExit(err)
	}
	for _, c := range resp.Payload {
		if c.StackID != nil && *c.StackID == stackID {
			return c, nil
		}
	}
	return nil, errors.New(fmt.Sprintf("no autoscaling cluster found for stack: %s", name))
}
