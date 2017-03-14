package cli

import (
	"errors"
	"fmt"
	log "github.com/Sirupsen/logrus"
	"github.com/hortonworks/hdc-cli/client_autoscale/alerts"
	"github.com/hortonworks/hdc-cli/client_autoscale/clusters"
	"github.com/hortonworks/hdc-cli/client_autoscale/configurations"
	"github.com/hortonworks/hdc-cli/client_autoscale/policies"
	"github.com/hortonworks/hdc-cli/models_autoscale"
	"github.com/urfave/cli"
	"time"
)

var AlertListHeader = []string{"Name", "Label"}

type AlertListElement struct {
	Name  string `json:"Name" yaml:"Name"`
	Label string `json:"Label" yaml:"Label"`
}

func (c *AlertListElement) DataAsStringArray() []string {
	return []string{c.Name, c.Label}
}

func AddAutoscalingPolicy(c *cli.Context) error {
	defer timeTrack(time.Now(), "add autoscaling policy")
	checkRequiredFlags(c)

	period := int32(c.Int(FlPeriod.Name))
	if period <= 0 {
		logErrorAndExit(errors.New("the period must be greater than 0"))
	}
	scalingAdjustment := int32(c.Int(FlScalingAdjustment.Name))
	if scalingAdjustment == 0 {
		logErrorAndExit(errors.New("the scalingAdjustment must be greater than or less than 0"))
	}
	nodeType := c.String(FlNodeType.Name)
	if nodeType != WORKER && nodeType != COMPUTE {
		logErrorAndExit(errors.New("the nodeType must be must be one of [worker, compute]"))
	}
	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, []string{FlClusterName.Name})
	}
	definitionName := c.String(FlScalingDefinition.Name)
	if len(definitionName) == 0 {
		logMissingParameterAndExit(c, []string{FlScalingDefinition.Name})
	}
	policyName := c.String(FlPolicyName.Name)
	if len(policyName) == 0 {
		logMissingParameterAndExit(c, []string{FlPolicyName.Name})
	}

	cbClient, asClient := NewOAuth2HTTPClients(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	validDefinition := false
	for _, alertDef := range asClient.getAlertDefinitions() {
		if *alertDef.Name == definitionName {
			validDefinition = true
			break
		}
	}
	if !validDefinition {
		logErrorAndExit(errors.New(fmt.Sprintf("the provided scaling definition name '%s' is not valid", definitionName)))
	}

	clusterId := *cbClient.GetClusterByName(clusterName).ID
	asClusterId, err := asClient.getAutoscalingClusterByStackId(clusterName, clusterId)
	if err != nil {
		log.Warn(err)
		asClusterId = asClient.createBaseAutoscalingCluster(clusterId)
	}

	policy := AutoscalingPolicy{
		PolicyName:        policyName,
		NodeType:          nodeType,
		Period:            period,
		Operator:          c.String(FlOperator.Name),
		ScalingAdjustment: scalingAdjustment,
		ScalingDefinition: definitionName,
		Threshold:         c.Float64(FlThreshold.Name),
	}

	alertId := asClient.addPrometheusAlert(asClusterId, policy)
	asClient.addScalingPolicy(asClusterId, policy, alertId)
	return nil
}

func RemoveAutoscalingPolicy(c *cli.Context) error {
	defer timeTrack(time.Now(), "remove autoscaling policy")
	checkRequiredFlags(c)

	clusterName := c.String(FlClusterName.Name)
	if len(clusterName) == 0 {
		logMissingParameterAndExit(c, []string{FlClusterName.Name})
	}
	policyName := c.String(FlPolicyName.Name)
	if len(policyName) == 0 {
		logMissingParameterAndExit(c, []string{FlPolicyName.Name})
	}

	cbClient, asClient := NewOAuth2HTTPClients(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))

	clusterId := *cbClient.GetClusterByName(clusterName).ID
	asClusterId, err := asClient.getAutoscalingClusterByStackId(clusterName, clusterId)
	if err != nil {
		logErrorAndExit(err)
	}

	alert := asClient.getAlertByName(policyName, asClusterId)
	if alert != nil {
		log.Infof("[RemoveAutoscalingPolicy] remove autoscaling policy: %s", policyName)
		asClient.AutoScaling.Alerts.DeletePrometheusAlarm(&alerts.DeletePrometheusAlarmParams{ClusterID: asClusterId, AlertID: *alert.ID})
	} else {
		logErrorAndExit(errors.New(fmt.Sprintf("autoscaling policy '%s' not found", policyName)))
	}

	return nil
}

func (autosScaling *Autoscaling) getAlertByName(name string, asClusterId int64) *models_autoscale.PromhetheusAlert {
	defer timeTrack(time.Now(), "get autoscaling alert by name")
	log.Infof("[getAlertByName] get autoscaling alert by name: %s", name)

	resp, err := autosScaling.AutoScaling.Alerts.GetPrometheusAlerts(&alerts.GetPrometheusAlertsParams{ClusterID: asClusterId})
	if err != nil {
		logErrorAndExit(err)
	}

	for _, a := range resp.Payload {
		if a.AlertName != nil && *a.AlertName == name {
			log.Infof("[getAlertByName] found alert by name: %s", name)
			return a
		}
	}

	return nil
}

func (autosScaling *Autoscaling) getAutoscalingClusterByStackId(clusterName string, stackId int64) (int64, error) {
	resp, err := autosScaling.AutoScaling.Clusters.GetClusters(&clusters.GetClustersParams{})
	if err != nil {
		logErrorAndExit(err)
	}
	for _, c := range resp.Payload {
		if c.StackID != nil && *c.StackID == stackId {
			return *c.ID, nil
		}
	}
	return 0, errors.New(fmt.Sprintf("no autoscaling cluster found for cluster: %s", clusterName))
}

func ListDefinitions(c *cli.Context) error {
	defer timeTrack(time.Now(), "list scaling definitions")

	asClient := NewAutoscalingOAuth2HTTPClient(c.String(FlServer.Name), c.String(FlUsername.Name), c.String(FlPassword.Name))
	output := Output{Format: c.String(FlOutput.Name)}

	return listDefinitionsImpl(asClient.getAlertDefinitions, output.WriteList)
}

func listDefinitionsImpl(getAlertDefinitions func() []*models_autoscale.AlertRuleDefinitionEntry,
	writer func([]string, []Row)) error {

	alertDefinitions := getAlertDefinitions()

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

func (autosScaling *Autoscaling) getAlertDefinitions() []*models_autoscale.AlertRuleDefinitionEntry {
	alertDefinitions, err := autosScaling.AutoScaling.Alerts.GetPrometheusDefinitions(&alerts.GetPrometheusDefinitionsParams{ClusterID: int64(0)})
	if err != nil {
		logErrorAndExit(err)
	}
	return alertDefinitions.Payload
}

func (autosScaling *Autoscaling) createBaseAutoscalingCluster(stackId int64) int64 {
	defer timeTrack(time.Now(), "create base autoscaling cluster")

	log.Infof("[createBaseAutoscalingCluster] add cluster to autoscaling, id: %d", stackId)
	state := "PENDING"
	resp, err := autosScaling.AutoScaling.Clusters.AddCluster(
		&clusters.AddClusterParams{
			Body: &models_autoscale.AmbariConnectionDetails{
				ClusterState: &state,
				StackID:      &stackId,
			}})

	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[createBaseAutoscalingCluster] base autoscale cluster created, id: %d", *resp.Payload.ID)
	return *resp.Payload.ID
}

func (autosScaling *Autoscaling) setConfiguration(asClusterId int64, config AutoscalingConfiguration) {
	defer timeTrack(time.Now(), "set autoscaling configuration")

	log.Infof("[setConfiguration] set autoscaling cluster configuration for cluster: %d, config: %v", asClusterId, config)
	_, err := autosScaling.AutoScaling.Configurations.SetScalingConfiguration(
		&configurations.SetScalingConfigurationParams{
			Body: &models_autoscale.ScalingConfiguration{
				Cooldown: &config.CooldownTime,
				MinSize:  &config.ClusterMinSize,
				MaxSize:  &config.ClusterMaxSize,
			},
			ClusterID: asClusterId,
		})

	if err != nil {
		logErrorAndExit(err)
	}
}

func (autosScaling *Autoscaling) addPrometheusAlert(asClusterId int64, policy AutoscalingPolicy) int64 {
	defer timeTrack(time.Now(), "add prometheus alert")

	log.Infof("[addPrometheusAlert] add prometheus alert to cluster: %d, name: %v", asClusterId, policy)
	operatorString := getOperatorName(policy.Operator)
	resp, err := autosScaling.AutoScaling.Alerts.CreatePrometheusAlert(
		&alerts.CreatePrometheusAlertParams{
			Body: &models_autoscale.PromhetheusAlert{
				AlertName:     &policy.PolicyName,
				AlertOperator: &operatorString,
				AlertRuleName: &policy.ScalingDefinition,
				Period:        &policy.Period,
				Threshold:     &policy.Threshold,
			},
			ClusterID: asClusterId,
		})
	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[addPrometheusAlert] prometheus alert added, id: %d", *resp.Payload.ID)
	return *resp.Payload.ID
}

func (autosScaling *Autoscaling) addScalingPolicy(asClusterId int64, policy AutoscalingPolicy, alertId int64) int64 {
	defer timeTrack(time.Now(), "add scaling policy")

	log.Infof("[addPolicy] add scaling policy: %v to alert id: %d", policy, alertId)

	adjustmentType := "NODE_COUNT"
	resp, err := autosScaling.AutoScaling.Policies.AddScaling(
		&policies.AddScalingParams{
			Body: &models_autoscale.ScalingPolicy{
				AdjustmentType:    &adjustmentType,
				AlertID:           &alertId,
				HostGroup:         &policy.NodeType,
				Name:              &policy.PolicyName,
				ScalingAdjustment: &policy.ScalingAdjustment,
			},
			ClusterID: asClusterId,
		})
	if err != nil {
		logErrorAndExit(err)
	}

	log.Infof("[addScalingPolicy] scaling policy added, id: %d", *resp.Payload.ID)
	return *resp.Payload.ID
}

func getOperatorName(operator string) string {
	switch operator {
	case ">":
		return "MORE_THAN"
	case "<":
		return "LESS_THAN"
	default:
		logErrorAndExit(errors.New("Unrecognised operator: " + operator))
	}
	return ""
}
