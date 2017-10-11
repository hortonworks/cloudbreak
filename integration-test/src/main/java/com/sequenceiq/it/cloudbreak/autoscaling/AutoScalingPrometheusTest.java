package com.sequenceiq.it.cloudbreak.autoscaling;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.recovery.RecoveryUtil;
import com.sequenceiq.it.cloudbreak.scaling.ScalingUtil;
import com.sequenceiq.periscope.client.AutoscaleClient;

public class AutoScalingPrometheusTest extends AbstractCloudbreakIntegrationTest {
    private IntegrationTestContext itContext;

    private AutoscaleClient autoscaleClient;

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull("Stack id is mandatory.", itContext.getContextParam(CloudbreakITContextConstants.STACK_ID));
        Assert.assertNotNull("Autoscale is mandatory.", itContext.getContextParam(CloudbreakITContextConstants.AUTOSCALE_CLIENT));
    }

    @Test
    @Parameters({ "cooldown", "clusterMinSize", "clusterMaxSize", "policyName", "operator", "alertRuleName", "period", "threshold", "hostGroup",
            "scalingAdjustment"})
    public void testAutoscaling(int cooldown, int clusterMinSize, int clusterMaxSize, String policyName, String operator, String alertRuleName, int period,
            Double threshold, String hostGroup, int scalingAdjustment) throws Exception {
        // GIVEN
        itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        StackV1Endpoint stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackEndpoint();
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        autoscaleClient = itContext.getContextParam(CloudbreakITContextConstants.AUTOSCALE_CLIENT, AutoscaleClient.class);
        Long clusterId = AutoscalingUtil.getPeriscopeClusterId(autoscaleClient, stackId);
        long currentTime = RecoveryUtil.getCurentTimeStamp();
        int expectedNodeCountStack = ScalingUtil.getNodeCountStack(stackV1Endpoint, stackId) + scalingAdjustment;
        int expectedNodeCountCluster = ScalingUtil.getNodeCountAmbari(stackV1Endpoint, ambariPort, stackId, ambariUser, ambariPassword, itContext)
                + scalingAdjustment;

        // WHEN
        AutoscalingUtil.configureAutoScaling(autoscaleClient, clusterId, cooldown, clusterMinSize, clusterMaxSize);
        AutoscalingUtil.switchAutoscaling(autoscaleClient, clusterId, true);
        AutoscalingUtil.createPrometheusAlert(autoscaleClient, clusterId, policyName, operator, alertRuleName, period, threshold);
        Long alertId = AutoscalingUtil.getAlertId(autoscaleClient, clusterId, policyName);
        AutoscalingUtil.setAlertsToContext(itContext, clusterId, alertId);
        AutoscalingUtil.createPolicy(autoscaleClient, policyName, clusterId, alertId, hostGroup, scalingAdjustment);
        // THEN
        AutoscalingUtil.checkHistory(autoscaleClient, clusterId, currentTime);
        AutoscalingUtil.checkScaling(itContext, getCloudbreakClient(), scalingAdjustment, stackId, expectedNodeCountStack, expectedNodeCountCluster);
    }

    @AfterTest
    public void cleanUpscaling() {
        Map<Long, List<Long>> autoscalingAlerts = itContext.getContextParam(CloudbreakITContextConstants.AUTOSCALE_ALERTS, Map.class);
        if (autoscalingAlerts != null) {
            for (Entry elem : autoscalingAlerts.entrySet()) {
                for (Object alertId : (Iterable<?>) (elem.getValue())) {
                    AutoscalingUtil.deletePrometheusAlert(autoscaleClient, (Long) elem.getKey(), (Long) alertId);
                }
            }
        }
        itContext.putContextParam(CloudbreakITContextConstants.AUTOSCALE_ALERTS, null);
    }
}
