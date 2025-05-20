package com.sequenceiq.it.cloudbreak.testcase.mock;

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.client.ApiKeyRequestFilter;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.config.server.ServerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.util.ResourceUtil;

public class MetricsTest extends AbstractMockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsTest.class);

    private static final String EXPECTED_CLOUDBREAK_METRICS_PATH = "classpath:/metrics/cloudbreak_metrics.txt";

    private static final String EXPECTED_DATALAKE_METRICS_PATH = "classpath:/metrics/datalake_metrics.txt";

    private static final String EXPECTED_ENVIRONMENT_METRICS_PATH = "classpath:/metrics/environment_metrics.txt";

    private static final String EXPECTED_FREE_IPA_METRICS_PATH = "classpath:/metrics/freeipa_metrics.txt";

    private static final String EXPECTED_PERISCOPE_METRICS_PATH = "classpath:/metrics/periscope_metrics.txt";

    private static final String EXPECTED_REDBEAMS_METRICS_PATH = "classpath:/metrics/redbeams_metrics.txt";

    private static final String EXPECTED_REMOTE_ENVIRONMENT_METRICS_PATH = "classpath:/metrics/remote_environment_metrics.txt";

    private static final String EXPECTED_EXTERNALIZED_COMPUTE_CLUSTER_METRICS_PATH = "classpath:/metrics/externalized_compute_cluster_metrics.txt";

    @Inject
    private ServerProperties serverProperties;

    @Override
    protected void setupTest(TestContext testContext) {

    }

    @Ignore("This test case should be re-enabled in case of InternalSDXDistroXTest has been removed")
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "cloudbreak service is running",
            when = "call the metrics endpoint",
            then = "metrics response contains a predefined set of metrics")
    public void testCloudbreakMetrics(TestContext testContext) throws Exception {
        Set<String> actualMetricNames = collectMetrics(testContext, serverProperties.getCloudbreakAddress());
        Set<String> expectedMetricNames = collectExpectedMetrics(testContext, EXPECTED_CLOUDBREAK_METRICS_PATH);

        showMissingMetrics(expectedMetricNames, actualMetricNames);
        assertTrue(actualMetricNames.containsAll(expectedMetricNames));
    }

    @Ignore("This test case should be re-enabled in case of InternalSDXDistroXTest has been removed")
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment service is running",
            when = "call the metrics endpoint",
            then = "metrics response contains a predefined set of metrics")
    public void testEnvironmentMetrics(TestContext testContext) throws IOException {
        Set<String> actualMetricNames = collectMetrics(testContext, serverProperties.getEnvironmentAddress());
        Set<String> expectedMetricNames = collectExpectedMetrics(testContext, EXPECTED_ENVIRONMENT_METRICS_PATH);

        showMissingMetrics(expectedMetricNames, actualMetricNames);
        assertTrue(actualMetricNames.containsAll(expectedMetricNames));
    }

    @Ignore("This test case should be re-enabled in case of InternalSDXDistroXTest has been removed")
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "datalake service is running",
            when = "call the metrics endpoint",
            then = "metrics response contains a predefined set of metrics")
    public void testDatalakeMetrics(TestContext testContext) throws IOException {
        Set<String> actualMetricNames = collectMetrics(testContext, serverProperties.getSdxAddress());
        Set<String> expectedMetricNames = collectExpectedMetrics(testContext, EXPECTED_DATALAKE_METRICS_PATH);

        showMissingMetrics(expectedMetricNames, actualMetricNames);
        assertTrue(actualMetricNames.containsAll(expectedMetricNames));
    }

    @Ignore("This test case should be re-enabled in case of InternalSDXDistroXTest has been removed")
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "free IPA service is running",
            when = "call the metrics endpoint",
            then = "metrics response contains a predefined set of metrics")
    public void testFreeIpaMetrics(TestContext testContext) throws IOException {
        Set<String> actualMetricNames = collectMetrics(testContext, serverProperties.getFreeipaAddress());
        Set<String> expectedMetricNames = collectExpectedMetrics(testContext, EXPECTED_FREE_IPA_METRICS_PATH);

        showMissingMetrics(expectedMetricNames, actualMetricNames);
        assertTrue(actualMetricNames.containsAll(expectedMetricNames));
    }

    @Ignore("This test case should be re-enabled in case of InternalSDXDistroXTest has been removed")
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "redbeams service is running",
            when = "call the metrics endpoint",
            then = "metrics response contains a predefined set of metrics")
    public void testRedbeamsMetrics(TestContext testContext) throws IOException {
        Set<String> actualMetricNames = collectMetrics(testContext, serverProperties.getRedbeamsAddress());
        Set<String> expectedMetricNames = collectExpectedMetrics(testContext, EXPECTED_REDBEAMS_METRICS_PATH);

        showMissingMetrics(expectedMetricNames, actualMetricNames);
        assertTrue(actualMetricNames.containsAll(expectedMetricNames));
    }

    @Ignore("This test case should be re-enabled in case of InternalSDXDistroXTest has been removed")
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "periscope service is running",
            when = "call the metrics endpoint",
            then = "metrics response contains a predefined set of metrics")
    public void testPeriscopeMetrics(TestContext testContext) throws IOException {
        Set<String> actualMetricNames = collectMetrics(testContext, serverProperties.getPeriscopeAddress());
        Set<String> expectedMetricNames = collectExpectedMetrics(testContext, EXPECTED_PERISCOPE_METRICS_PATH);

        showMissingMetrics(expectedMetricNames, actualMetricNames);
        assertTrue(actualMetricNames.containsAll(expectedMetricNames));
    }

    @Ignore("This test case should be re-enabled in case of InternalSDXDistroXTest has been removed")
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "remote environment service is running",
            when = "call the metrics endpoint",
            then = "metrics response contains a predefined set of metrics")
    public void testRemoteEnvironmentMetrics(TestContext testContext) throws IOException {
        Set<String> actualMetricNames = collectMetrics(testContext, serverProperties.getRemoteEnvironmentAddress());
        Set<String> expectedMetricNames = collectExpectedMetrics(testContext, EXPECTED_REMOTE_ENVIRONMENT_METRICS_PATH);

        showMissingMetrics(expectedMetricNames, actualMetricNames);
        assertTrue(actualMetricNames.containsAll(expectedMetricNames));
    }

    @Ignore("This test case should be re-enabled in case of InternalSDXDistroXTest has been removed")
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "externalized compute cluster service is running",
            when = "call the metrics endpoint",
            then = "metrics response contains a predefined set of metrics")
    public void testExternalizedComputeClusterMetrics(TestContext testContext) throws IOException {
        Set<String> actualMetricNames = collectMetrics(testContext, serverProperties.getExternalizedComputeAddress());
        Set<String> expectedMetricNames = collectExpectedMetrics(testContext, EXPECTED_EXTERNALIZED_COMPUTE_CLUSTER_METRICS_PATH);

        showMissingMetrics(expectedMetricNames, actualMetricNames);
        assertTrue(actualMetricNames.containsAll(expectedMetricNames));
    }

    private void showMissingMetrics(Set<String> expectedMetricNames, Set<String> actualMetricNames) {
        Set<String> missingMetrics = Sets.difference(expectedMetricNames, actualMetricNames);
        if (!missingMetrics.isEmpty()) {
            LOGGER.info("Missing metrics: {}", String.join(", ", missingMetrics));
        }
    }

    private Set<String> collectMetrics(TestContext testContext, String address) {
        Set<String> metrics = new HashSet<>();
        String responseBody = getMetricsResponse(testContext, address);
        for (String x : responseBody.split("\n")) {
            if (!x.startsWith("#")) {
                String[] keyValuePair = x.split(x.contains("{") ? "\\{" : "\\s+");
                metrics.add(keyValuePair[0]);
            }
        }

        return metrics;
    }

    private String getMetricsResponse(TestContext testContext, String address) {
        Client client = RestClientUtil.get();
        WebTarget webTarget = client.target(address).path("/metrics");
        webTarget.register(new ApiKeyRequestFilter(testContext.getActingUserAccessKey(), testContext.getActingUser().getSecretKey()));
        return webTarget.request().get().readEntity(String.class);
    }

    private Set<String> collectExpectedMetrics(TestContext testContext, String path) throws IOException {
        String fileContent = ResourceUtil.readResourceAsString(testContext.getApplicationContext(), path);
        return new HashSet<>(Arrays.asList(fileContent.split(System.lineSeparator())));
    }
}