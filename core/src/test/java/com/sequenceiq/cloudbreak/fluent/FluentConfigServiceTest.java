package com.sequenceiq.cloudbreak.fluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.common.api.cloudstorage.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class FluentConfigServiceTest {

    private FluentConfigService underTest;

    @Before
    public void setUp() {
        underTest = new FluentConfigService();
    }

    @Test
    public void testCreateFluentConfig() {
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("mybucket");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);

        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/distrox/cl1", result.getS3LogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithDatalake() {
        Stack stack = createStack();
        stack.setType(StackType.DATALAKE);
        Logging logging = new Logging();
        logging.setStorageLocation("mybucket");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);

        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/sdx/cl1", result.getS3LogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithCustomPath() {
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("mybucket/cluster-logs/custom");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);

        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/custom/distrox/cl1", result.getS3LogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithS3Path() {
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("s3://mybucket");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);

        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/distrox/cl1", result.getS3LogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithS3APath() {
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("s3a://mybucket");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);

        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/distrox/cl1", result.getS3LogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    private Stack createStack() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setName("cl1");
        stack.setCluster(cluster);
        stack.setCloudPlatform("AWS");
        stack.setType(StackType.WORKLOAD);
        return stack;
    }
}
