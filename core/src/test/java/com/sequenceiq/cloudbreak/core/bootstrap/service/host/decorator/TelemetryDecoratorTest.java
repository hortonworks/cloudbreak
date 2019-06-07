package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.Logging;
import com.sequenceiq.cloudbreak.cloud.model.LoggingAttributesHolder;
import com.sequenceiq.cloudbreak.cloud.model.LoggingOutputType;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.cloud.model.logging.S3LoggingAttributes;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class TelemetryDecoratorTest {

    private TelemetryDecorator underTest;

    private Map<String, SaltPillarProperties> servicePillar;

    @Before
    public void setUp() {
        servicePillar = new HashMap<>();
        underTest = new TelemetryDecorator(servicePillar);
    }

    @Test
    public void testS3Decorate() {
        // GIVEN
        String clusterName = "cl1";
        LoggingAttributesHolder attributes = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(
                "mybucket", "cluster-logs/custom", 5);
        attributes.setS3Attributes(s3Attributes);
        Logging logging = new Logging(true, LoggingOutputType.S3, attributes);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        underTest.decoratePillar(telemetry, clusterName, StackType.WORKLOAD);
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(servicePillar);
        assertEquals(results.get("providerPrefix"), "s3");
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("s3LogFolderName"), "cluster-logs/custom/distrox/cl1");
        assertEquals(results.get("enabled"), true);
    }

    @Test
    public void testS3DecorateWithDatalake() {
        // GIVEN
        String clusterName = "cl1";
        LoggingAttributesHolder attributes = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(
                "mybucket", "cluster-logs/custom", 5);
        attributes.setS3Attributes(s3Attributes);
        Logging logging = new Logging(true, LoggingOutputType.S3, attributes);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        underTest.decoratePillar(telemetry, clusterName, StackType.DATALAKE);
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(servicePillar);
        assertEquals(results.get("providerPrefix"), "s3");
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("s3LogFolderName"), "cluster-logs/custom/sdx/cl1");
        assertEquals(results.get("enabled"), true);
    }

    @Test
    public void testS3DecorateWithDefaultPath() {
        // GIVEN
        String clusterName = "cl1";
        LoggingAttributesHolder attributes = new LoggingAttributesHolder();
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(
                "mybucket", null, 5);
        attributes.setS3Attributes(s3Attributes);
        Logging logging = new Logging(true, LoggingOutputType.S3, attributes);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        underTest.decoratePillar(telemetry, clusterName, StackType.WORKLOAD);
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(servicePillar);
        assertEquals(results.get("providerPrefix"), "s3");
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("s3LogFolderName"), "cluster-logs/distrox/cl1");
        assertEquals(results.get("enabled"), true);
    }

    @Test
    public void testS3DecorateWitoutS3Attributes() {
        // GIVEN
        String clusterName = "cl1";
        LoggingAttributesHolder attributes = new LoggingAttributesHolder();
        Logging logging = new Logging(true, LoggingOutputType.S3, attributes);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        underTest.decoratePillar(telemetry, clusterName, StackType.WORKLOAD);
        // THEN
        Map<String, Object> results = createMapFromFluentPillars(servicePillar);
        assertEquals(results.get("enabled"), false);
    }

    @Test
    public void testS3DecorateWithFullPathBucket() {
        // GIVEN
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(
                "s3://mybucket/cluster-logs/my/path", null, 5);
        Map<String, Object> results = new HashMap<>();
        // WHEN
        underTest.calculateS3BucketAndLogFolder("cl1", StackType.WORKLOAD, s3Attributes, results);
        // THEN
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("s3LogFolderName"), "cluster-logs/distrox/cl1");
    }

    @Test
    public void testS3DecorateWithFullBasePathOnly() {
        // GIVEN
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(
                null, "s3://mybucket/cluster-logs", 5);
        Map<String, Object> results = new HashMap<>();
        // WHEN
        underTest.calculateS3BucketAndLogFolder("cl1", StackType.WORKLOAD, s3Attributes, results);
        // THEN
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("s3LogFolderName"), "cluster-logs/distrox/cl1");
    }

    @Test
    public void testS3DecorateWithFullPathBucketWithS3A() {
        // GIVEN
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(
                null, "s3a://mybucket/cluster-logs", 5);
        Map<String, Object> results = new HashMap<>();
        // WHEN
        underTest.calculateS3BucketAndLogFolder("cl1", StackType.WORKLOAD, s3Attributes, results);
        // THEN
        assertEquals(results.get("s3LogArchiveBucketName"), "mybucket");
        assertEquals(results.get("s3LogFolderName"), "cluster-logs/distrox/cl1");
    }

    @Test(expected = CloudbreakServiceException.class)
    public void testS3DecorateWithoutProperAttributes() {
        // GIVEN
        S3LoggingAttributes s3Attributes = new S3LoggingAttributes(
                null, null, 5);
        Map<String, Object> results = new HashMap<>();
        // WHEN
        underTest.calculateS3BucketAndLogFolder("cl1", StackType.WORKLOAD, s3Attributes, results);
    }

    private Map<String, Object> createMapFromFluentPillars(Map<String, SaltPillarProperties> servicePillar) {
        return (Map<String, Object>) servicePillar.get("fluent").getProperties().get("fluent");
    }
}
