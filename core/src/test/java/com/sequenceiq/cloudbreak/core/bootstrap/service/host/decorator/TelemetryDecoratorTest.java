package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.fluent.FluentConfigService;
import com.sequenceiq.cloudbreak.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class TelemetryDecoratorTest {

    private TelemetryDecorator underTest;

    @Mock
    private FluentConfigService fluentConfigService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new TelemetryDecorator(fluentConfigService);
    }

    @Test
    public void testS3DecorateWithDefaultPath() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        FluentConfigView fluentConfigView = new FluentConfigView.Builder()
                .withPlatform("AWS")
                .withEnabled(true)
                .withS3LogArchiveBucketName("mybucket")
                .withLogFolderName("cluster-logs/datahub/cl1")
                .withProviderPrefix("s3")
                .build();
        given(fluentConfigService.createFluentConfigs(anyString(), anyString(), anyString(), any(Telemetry.class)))
                .willReturn(fluentConfigView);
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar,
                createStack(), new Telemetry(null, null));
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(result);
        assertEquals(results.get("providerPrefix"), "s3");
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("logFolderName"), "cluster-logs/datahub/cl1");
        assertEquals(results.get("enabled"), true);
        assertEquals(results.get("platform"), CloudPlatform.AWS.name());
        assertEquals(results.get("user"), "root");
    }

    @Test
    public void testS3DecorateWithOverrides() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("providerPrefix", "s3a");
        FluentConfigView fluentConfigView = new FluentConfigView.Builder()
                .withPlatform("AWS")
                .withEnabled(true)
                .withS3LogArchiveBucketName("mybucket")
                .withLogFolderName("cluster-logs/datahub/cl1")
                .withProviderPrefix("s3")
                .withOverrideAttributes(overrides)
                .build();
        given(fluentConfigService.createFluentConfigs(anyString(), anyString(), anyString(), any(Telemetry.class)))
                .willReturn(fluentConfigView);
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar,
                createStack(), new Telemetry(null, null));
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(result);
        assertEquals(results.get("providerPrefix"), "s3a");
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("logFolderName"), "cluster-logs/datahub/cl1");
        assertEquals(results.get("enabled"), true);
    }

    @Test
    public void testDecorateWithDisabledLogging() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        FluentConfigView fluentConfigView = new FluentConfigView.Builder()
                .build();
        given(fluentConfigService.createFluentConfigs(anyString(), anyString(), anyString(), any(Telemetry.class)))
                .willReturn(fluentConfigView);
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar,
                createStack(), new Telemetry(null, null));
        // THEN
        assertTrue(result.isEmpty());
    }

    private Map<String, Object> createMapFromFluentPillars(Map<String, SaltPillarProperties> servicePillar) {
        return (Map<String, Object>) servicePillar.get("fluent").getProperties().get("fluent");
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        stack.setCloudPlatform("AWS");
        Cluster cluster = new Cluster();
        cluster.setName("cl1");
        stack.setCluster(cluster);
        return stack;
    }

}
