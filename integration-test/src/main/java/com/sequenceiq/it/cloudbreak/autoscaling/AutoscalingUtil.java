package com.sequenceiq.it.cloudbreak.autoscaling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.scaling.ScalingUtil;
import com.sequenceiq.periscope.api.endpoint.AlertEndpoint;
import com.sequenceiq.periscope.api.endpoint.ClusterEndpoint;
import com.sequenceiq.periscope.api.endpoint.ConfigurationEndpoint;
import com.sequenceiq.periscope.api.endpoint.PolicyEndpoint;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.AlertOperator;
import com.sequenceiq.periscope.api.model.AlertState;
import com.sequenceiq.periscope.api.model.ClusterAutoscaleState;
import com.sequenceiq.periscope.api.model.ClusterJson;
import com.sequenceiq.periscope.api.model.PrometheusAlertJson;
import com.sequenceiq.periscope.api.model.ScalingConfigurationJson;
import com.sequenceiq.periscope.api.model.ScalingPolicyJson;
import com.sequenceiq.periscope.client.AutoscaleClient;


public class AutoscalingUtil extends AbstractCloudbreakIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sequenceiq.it.cloudbreak.autoscaling.AutoscalingUtil.class);

    static void setAlertsToContext(IntegrationTestContext itContext, Long clusterId, Long alertId) {
        List<Long> alertList = new ArrayList();
        Map<Long, List<Long>> autoscalingAlerts = new HashMap<>();
        if (itContext.getContextParam(CloudbreakITContextConstants.AUTOSCALE_ALERTS, Map.class) != null) {
            alertList = autoscalingAlerts.get(clusterId);
        }
        alertList.add(alertId);
        autoscalingAlerts.put(clusterId, alertList);
        itContext.putContextParam(CloudbreakITContextConstants.AUTOSCALE_ALERTS, autoscalingAlerts);
    }

    static long getPeriscopeClusterId(AutoscaleClient autoscaleClient, String stackId) {
        Long clusterId = null;
        int retryCount = 0;
        ClusterEndpoint clusterEndpoint = autoscaleClient.clusterEndpoint();
        while (clusterId == null && retryCount < 30) {
            LOGGER.info("Waiting for having Prometheus cluster id ...");
            CloudbreakUtil.sleep();
            List<ClusterJson> clusterJson = clusterEndpoint.getClusters();
            for (ClusterJson elem : clusterJson) {
                if (String.valueOf(elem.getStackId()).equals(stackId)) {
                    clusterId = elem.getId();
                }
            }
            retryCount += 1;
        }
        Assert.assertNotNull(clusterId);
        return clusterId;
    }

    static void createPrometheusAlert(AutoscaleClient autoscaleClient, Long clusterId, String alertName, String alertOperator,
            String alertRuleName, int period, Double threshold) {
        PrometheusAlertJson prometheusAlertJson = new PrometheusAlertJson();
        prometheusAlertJson.setAlertName(alertName);
        if ("more".equals(alertOperator)) {

            prometheusAlertJson.setAlertOperator(AlertOperator.MORE_THAN);
        } else {
            prometheusAlertJson.setAlertOperator(AlertOperator.LESS_THAN);
        }
        prometheusAlertJson.setAlertRuleName(alertRuleName);
        prometheusAlertJson.setAlertState(AlertState.OK);
        prometheusAlertJson.setPeriod(period);
        prometheusAlertJson.setThreshold(threshold);
        AlertEndpoint alertEndpoint = autoscaleClient.alertEndpoint();
        alertEndpoint.createPrometheusAlert(clusterId, prometheusAlertJson);
    }

    static Long getAlertId(AutoscaleClient autoscaleClient, Long clusterId, String alertName) {
        Long alertId = null;
        AlertEndpoint alertEndpoint = autoscaleClient.alertEndpoint();
        List<PrometheusAlertJson> prometheusAlertJsons = alertEndpoint.getPrometheusAlerts(clusterId);

        for (PrometheusAlertJson entry : prometheusAlertJsons) {
            if (entry.getAlertName().equals(alertName)) {
                alertId = entry.getId();
            }
        }
        Assert.assertNotNull(alertId);
        return alertId;
    }

    static void createPolicy(AutoscaleClient autoscaleClient, String policyName, Long clusterId, Long alertId, String hostGroup,
            int scalingAdjustment) {
        Boolean policyCreated = Boolean.FALSE;
        ScalingPolicyJson scalingPolicyJson = new ScalingPolicyJson();
        scalingPolicyJson.setName(policyName);
        scalingPolicyJson.setAdjustmentType(AdjustmentType.NODE_COUNT);
        scalingPolicyJson.setAlertId(alertId);
        scalingPolicyJson.setScalingAdjustment(scalingAdjustment);
        scalingPolicyJson.setHostGroup(hostGroup);
        PolicyEndpoint policyEndpoint = autoscaleClient.policyEndpoint();
        policyEndpoint.addScaling(clusterId, scalingPolicyJson);

        List<ScalingPolicyJson> policies = policyEndpoint.getScaling(clusterId);
        for (ScalingPolicyJson policy : policies) {
            if (policy.getAlertId() == alertId) {
                policyCreated = Boolean.TRUE;
            }
        }
        Assert.assertTrue(policyCreated);
    }

    static void configureAutoScaling(AutoscaleClient autoscaleClient, Long clusterId, int cooldown, int clusterMinsize, int clusterMaxSize) {
        ConfigurationEndpoint configurationEndpoint = autoscaleClient.configurationEndpoint();
        ScalingConfigurationJson scalingConfigurationJson = new ScalingConfigurationJson();
        scalingConfigurationJson.setCoolDown(cooldown);
        scalingConfigurationJson.setMinSize(clusterMinsize);
        scalingConfigurationJson.setMaxSize(clusterMaxSize);
        configurationEndpoint.setScalingConfiguration(clusterId, scalingConfigurationJson);

        ScalingConfigurationJson scalingConfigurationTest = configurationEndpoint.getScalingConfiguration(clusterId);
        Assert.assertEquals(cooldown, scalingConfigurationTest.getCoolDown());
        Assert.assertEquals(clusterMinsize, scalingConfigurationTest.getMinSize());
        Assert.assertEquals(clusterMaxSize, scalingConfigurationTest.getMaxSize());
    }

    static void checkHistory(AutoscaleClient autoscaleClient, Long clusterId, Long currentTime) {
        CloudbreakUtil.waitForAutoScalingEvent(autoscaleClient, clusterId, currentTime);
    }

    static void checkScaling(IntegrationTestContext itContext, CloudbreakClient cloudbreakClient, int scalingAdjustment, String stackId,
            int expectedNodeCountStack, int expectedNodeCountCluster) {

        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);

        StackV1Endpoint stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackEndpoint();

        if (scalingAdjustment < 0) {
            CloudbreakUtil.waitAndCheckClusterStatus(cloudbreakClient, stackId, "AVAILABLE");
            CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackId, "AVAILABLE");

        } else {
            CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackId, "AVAILABLE");
            CloudbreakUtil.waitAndCheckClusterStatus(cloudbreakClient, stackId, "AVAILABLE");
        }

        ScalingUtil.checkStackScaled(stackV1Endpoint, stackId, expectedNodeCountStack);
        ScalingUtil.checkClusterScaled(stackV1Endpoint, ambariPort, stackId, ambariUser, ambariPassword, expectedNodeCountCluster, itContext);
        ScalingUtil.putInstanceCountToContext(itContext, stackId);
    }

    static void deletePrometheusAlert(AutoscaleClient autoscaleClient, Long clusterId, Long alertId) {
        AlertEndpoint alertEndpoint = autoscaleClient.alertEndpoint();
        alertEndpoint.deletePrometheusAlarm(clusterId, alertId);
    }

    static void switchAutoscaling(AutoscaleClient autoscaleClient, Long clusterId, boolean enableAutoscaling) {
        ClusterEndpoint clusterEndpoint = autoscaleClient.clusterEndpoint();
        ClusterAutoscaleState json = new ClusterAutoscaleState();
        json.setEnableAutoscaling(enableAutoscaling);
        clusterEndpoint.setAutoscaleState(clusterId, json);
    }
}
