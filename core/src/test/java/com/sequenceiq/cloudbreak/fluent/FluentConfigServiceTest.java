package com.sequenceiq.cloudbreak.fluent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.cloudbreak.fluent.cloud.WasbConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class FluentConfigServiceTest {

    private FluentConfigService underTest;

    @Before
    public void setUp() {
        underTest = new FluentConfigService(new S3ConfigGenerator(), new WasbConfigGenerator());
    }

    @Test
    public void testCreateFluentConfig() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("mybucket");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithDatalake() {
        // GIVEN
        Stack stack = createStack();
        stack.setType(StackType.DATALAKE);
        Logging logging = new Logging();
        logging.setStorageLocation("mybucket");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/datalake/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithCustomPath() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("mybucket/cluster-logs/custom");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/custom/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithS3Path() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("s3://mybucket");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithS3APath() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("s3a://mybucket");
        logging.setS3(new S3CloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mybucket", result.getS3LogArchiveBucketName());
    }

    @Test
    public void testCreateFluentConfigWithWasbPath() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("wasb://mycontainer@myaccount.blob.core.windows.net");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigWithFullWasbPath() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("wasb://mycontainer@myaccount.blob.core.windows.net/my/custom/path");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        wasbParams.setAccountName("myAccount");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertEquals("myaccount", result.getAzureStorageAccount());
        assertEquals("/my/custom/path/datahub/cl1", result.getLogFolderName());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigWithFullWasbPathWithContainer() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("wasb://mycontainer/my/custom/path@myaccount.blob.core.windows.net");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertEquals("/my/custom/path/datahub/cl1", result.getLogFolderName());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigWithoutScheme() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("mycontainer@myaccount.blob.core.windows.net");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test
    public void testCreateFluentConfigWithOnlyContainer() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("mycontainer");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        wasbParams.setAccountName("myAccount");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        FluentConfigView result = underTest.createFluentConfigs(stack, telemetry);
        // THEN
        assertTrue(result.isEnabled());
        assertEquals("myAccountKey", result.getAzureStorageAccessKey());
        assertEquals("myAccount", result.getAzureStorageAccount());
        assertEquals("cluster-logs/datahub/cl1", result.getLogFolderName());
        assertEquals("mycontainer", result.getAzureContainer());
    }

    @Test(expected = CloudbreakServiceException.class)
    public void testCreateFluentConfigWithoutLocation() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation(null);
        logging.setWasb(new WasbCloudStorageV1Parameters());
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        underTest.createFluentConfigs(stack, telemetry);
    }

    @Test(expected = CloudbreakServiceException.class)
    public void testCreateFluentConfigWithDoublePath() {
        // GIVEN
        Stack stack = createStack();
        Logging logging = new Logging();
        logging.setStorageLocation("wasb://mycontainer/my/custom/path@myaccount.blob.core.windows.net/my/custom/path");
        WasbCloudStorageV1Parameters wasbParams = new WasbCloudStorageV1Parameters();
        wasbParams.setAccountKey("myAccountKey");
        logging.setWasb(wasbParams);
        Telemetry telemetry = new Telemetry(logging, null);
        // WHEN
        underTest.createFluentConfigs(stack, telemetry);
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
