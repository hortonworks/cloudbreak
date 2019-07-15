package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.fluent.FluentConfigView;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

public class TelemetryDecoratorTest {

    private final TelemetryDecorator underTest = new TelemetryDecorator();

    @Test
    public void testS3DecorateWithDefaultPath() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        FluentConfigView fluentConfigView = new FluentConfigView.Builder()
                .withPlatform("AWS")
                .withEnabled(true)
                .withS3LogArchiveBucketName("mybucket")
                .withS3LogFolderName("cluster-logs/distrox/cl1")
                .withProviderPrefix("s3")
                .build();
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar, fluentConfigView);
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(result);
        assertEquals(results.get("providerPrefix"), "s3");
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("s3LogFolderName"), "cluster-logs/distrox/cl1");
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
                .withS3LogFolderName("cluster-logs/distrox/cl1")
                .withProviderPrefix("s3")
                .withOverrideAttributes(overrides)
                .build();
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar, fluentConfigView);
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(result);
        assertEquals(results.get("providerPrefix"), "s3a");
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("s3LogFolderName"), "cluster-logs/distrox/cl1");
        assertEquals(results.get("enabled"), true);
    }

    @Test
    public void testDecorateWithDisabledLogging() {
        // GIVEN
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        FluentConfigView fluentConfigView = new FluentConfigView.Builder()
                .build();
        // WHEN
        Map<String, SaltPillarProperties> result = underTest.decoratePillar(servicePillar, fluentConfigView);
        // THEN
        assertTrue(result.isEmpty());
    }

    private Map<String, Object> createMapFromFluentPillars(Map<String, SaltPillarProperties> servicePillar) {
        return (Map<String, Object>) servicePillar.get("fluent").getProperties().get("fluent");
    }

}
