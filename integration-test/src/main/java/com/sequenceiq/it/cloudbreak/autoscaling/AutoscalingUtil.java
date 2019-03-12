package com.sequenceiq.it.cloudbreak.autoscaling;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.scaling.ScalingUtil;
import com.sequenceiq.periscope.api.endpoint.v1.AlertEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.AutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.endpoint.v1.ConfigurationEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.PolicyEndpoint;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.AlertOperator;
import com.sequenceiq.periscope.api.model.AlertState;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.AutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.PrometheusAlertRequest;
import com.sequenceiq.periscope.api.model.PrometheusAlertResponse;
import com.sequenceiq.periscope.api.model.ScalingConfigurationRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyResponse;
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
        AutoScaleClusterV1Endpoint autoScaleClusterV1Endpoint = autoscaleClient.clusterEndpoint();
        while (clusterId == null && retryCount < 30) {
            LOGGER.info("Waiting for having Prometheus cluster id ...");
            CloudbreakUtil.sleep();
            List<AutoscaleClusterResponse> autoscaleClusterResponse = autoScaleClusterV1Endpoint.getClusters();
            for (AutoscaleClusterResponse elem : autoscaleClusterResponse) {
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
        PrometheusAlertRequest prometheusAlertRequest = new PrometheusAlertRequest();
        prometheusAlertRequest.setAlertName(alertName);
        if ("more".equals(alertOperator)) {

            prometheusAlertRequest.setAlertOperator(AlertOperator.MORE_THAN);
        } else {
            prometheusAlertRequest.setAlertOperator(AlertOperator.LESS_THAN);
        }
        prometheusAlertRequest.setAlertRuleName(alertRuleName);
        prometheusAlertRequest.setAlertState(AlertState.OK);
        prometheusAlertRequest.setPeriod(period);
        prometheusAlertRequest.setThreshold(threshold);
        AlertEndpoint alertEndpoint = autoscaleClient.alertEndpoint();
        alertEndpoint.createPrometheusAlert(clusterId, prometheusAlertRequest);
    }

    static Long getAlertId(AutoscaleClient autoscaleClient, Long clusterId, String alertName) {
        Long alertId = null;
        AlertEndpoint alertEndpoint = autoscaleClient.alertEndpoint();
        List<PrometheusAlertResponse> prometheusAlertResponses = alertEndpoint.getPrometheusAlerts(clusterId);

        for (PrometheusAlertResponse entry : prometheusAlertResponses) {
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
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setName(policyName);
        scalingPolicyRequest.setAdjustmentType(AdjustmentType.NODE_COUNT);
        scalingPolicyRequest.setAlertId(alertId);
        scalingPolicyRequest.setScalingAdjustment(scalingAdjustment);
        scalingPolicyRequest.setHostGroup(hostGroup);
        PolicyEndpoint policyEndpoint = autoscaleClient.policyEndpoint();
        policyEndpoint.addScalingPolicy(clusterId, scalingPolicyRequest);

        List<ScalingPolicyResponse> policies = policyEndpoint.getScalingPolicies(clusterId);
        for (ScalingPolicyResponse policy : policies) {
            if (policy.getAlertId() == alertId) {
                policyCreated = Boolean.TRUE;
            }
        }
        Assert.assertTrue(policyCreated);
    }

    static void configureAutoScaling(AutoscaleClient autoscaleClient, Long clusterId, int cooldown, int clusterMinsize, int clusterMaxSize) {
        ConfigurationEndpoint configurationEndpoint = autoscaleClient.configurationEndpoint();
        ScalingConfigurationRequest scalingConfigurationRequest = new ScalingConfigurationRequest();
        scalingConfigurationRequest.setCoolDown(cooldown);
        scalingConfigurationRequest.setMinSize(clusterMinsize);
        scalingConfigurationRequest.setMaxSize(clusterMaxSize);
        configurationEndpoint.setScalingConfiguration(clusterId, scalingConfigurationRequest);

        ScalingConfigurationRequest scalingConfigurationTest = configurationEndpoint.getScalingConfiguration(clusterId);
        Assert.assertEquals(cooldown, scalingConfigurationTest.getCoolDown());
        Assert.assertEquals(clusterMinsize, scalingConfigurationTest.getMinSize());
        Assert.assertEquals(clusterMaxSize, scalingConfigurationTest.getMaxSize());
    }

    static void checkHistory(AutoscaleClient autoscaleClient, Long clusterId, Long currentTime) {
        CloudbreakUtil.waitForAutoScalingEvent(autoscaleClient, clusterId, currentTime);
    }

    static void checkScaling(IntegrationTestContext itContext, CloudbreakClient cloudbreakClient, Long workspaceId, int scalingAdjustment, String stackName,
            int expectedNodeCountStack, int expectedNodeCountCluster) throws IOException, URISyntaxException {

        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);

        var stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackV4Endpoint();

        if (scalingAdjustment < 0) {
            CloudbreakUtil.waitAndCheckClusterStatus(cloudbreakClient, workspaceId, stackName, "AVAILABLE");
            CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, workspaceId, stackName, "AVAILABLE");

        } else {
            CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, workspaceId, stackName, "AVAILABLE");
            CloudbreakUtil.waitAndCheckClusterStatus(cloudbreakClient, workspaceId, stackName, "AVAILABLE");
        }

        ScalingUtil.checkClusterScaled(stackV1Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, expectedNodeCountCluster, itContext);
        ScalingUtil.putInstanceCountToContext(itContext, workspaceId, stackName);
    }

    static void deletePrometheusAlert(AutoscaleClient autoscaleClient, Long clusterId, Long alertId) {
        AlertEndpoint alertEndpoint = autoscaleClient.alertEndpoint();
        alertEndpoint.deletePrometheusAlarm(clusterId, alertId);
    }

    static void switchAutoscaling(AutoscaleClient autoscaleClient, Long clusterId, boolean enableAutoscaling) {
        AutoScaleClusterV1Endpoint autoScaleClusterV1Endpoint = autoscaleClient.clusterEndpoint();
        AutoscaleClusterState json = new AutoscaleClusterState();
        json.setEnableAutoscaling(enableAutoscaling);
        autoScaleClusterV1Endpoint.setAutoscaleState(clusterId, json);
    }
}
