package cli

import (
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_autoscale/alerts"
	"github.com/hortonworks/hdc-cli/models_autoscale"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
	"github.com/urfave/cli"
	"time"
)

var AlertListHeader = []string{"Name", "Label"}

type AlertListElement struct {
	Name  string `json:"name" yaml:"name"`
	Label string `json:"label" yaml:"label"`
}

func (c *AlertListElement) DataAsStringArray() []string {
	return []string{c.Name, c.Label}
}

func ListDefinitions(c *cli.Context) error {
	defer timeTrack(time.Now(), "list scaling definitions")

	cbClient, asClient := NewOAuth2HTTPClients(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := Output{Format: c.String(FlOutput.Name)}

	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, []string{FlClusterName.Name})
	}

	return listDefinitionsImpl(clusterName, asClient.getAlertDefinitions, cbClient.GetClusterByName, output.WriteList)
}

func listDefinitionsImpl(clusterName string,
	getAlertDefinitions func(string, func(name string) *models_cloudbreak.StackResponse) []*models_autoscale.AlertRuleDefinitionEntry,
	getCluster func(name string) *models_cloudbreak.StackResponse,
	writer func([]string, []Row)) error {

	alertDefinitions := getAlertDefinitions(clusterName, getCluster)

	tableRows := make([]Row, len(alertDefinitions))
	for i, a := range alertDefinitions {
		tableRows[i] = &AlertListElement{
			Name:  SafeStringConvert(a.Name),
			Label: SafeStringConvert(a.Label),
		}
	}
	writer(AlertListHeader, tableRows)

	return nil
}

func (autosScaling *Autoscaling) getAlertDefinitions(clusterName string,
	getCluster func(name string) *models_cloudbreak.StackResponse) []*models_autoscale.AlertRuleDefinitionEntry {

	log.Infof("[getAlertDefinitions] get cluster by name: %s", clusterName)
	clusterId := getCluster(clusterName).ID
	log.Infof("[getAlertDefinitions] found cluster, id: %d", *clusterId)

	alertDefinitions, err := autosScaling.AutoScaling.Alerts.GetPrometheusDefinitions(&alerts.GetPrometheusDefinitionsParams{ClusterID: *clusterId})
	if err != nil {
		logErrorAndExit(err)
	}

	return alertDefinitions.Payload
}
