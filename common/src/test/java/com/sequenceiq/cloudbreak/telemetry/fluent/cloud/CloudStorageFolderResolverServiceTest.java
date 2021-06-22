package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public class CloudStorageFolderResolverServiceTest {

    private CloudStorageFolderResolverService underTest;

    @Before
    public void setUp() {
        underTest = new CloudStorageFolderResolverService(new S3ConfigGenerator(),
                new AdlsGen2ConfigGenerator(), new GcsConfigGenerator());
    }

    @Test
    public void testUpdateStorageLocationS3() {
        // GIVEN
        Telemetry telemetry = createTelemetry();
        // WHEN
        underTest.updateStorageLocation(telemetry, FluentClusterType.DATAHUB.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("s3://mybucket/cluster-logs/datahub/mycluster_12345", telemetry.getLogging().getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationS3WithPrefix() {
        // GIVEN
        Telemetry telemetry = createTelemetry();
        telemetry.getLogging().setStorageLocation("s3://mybucket/prefix");
        // WHEN
        underTest.updateStorageLocation(telemetry, FluentClusterType.DATAHUB.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("s3://mybucket/prefix/cluster-logs/datahub/mycluster_12345", telemetry.getLogging().getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationAdlsGen2() {
        // GIVEN
        Telemetry telemetry = createTelemetry();
        telemetry.getLogging().setS3(null);
        telemetry.getLogging().setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        telemetry.getLogging().setStorageLocation("abfs://mycontainer");
        // WHEN
        underTest.updateStorageLocation(telemetry, FluentClusterType.DATAHUB.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("abfs://mycontainer@null.dfs.core.windows.net/cluster-logs/datahub/mycluster_12345", telemetry.getLogging().getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationAdlsGen2WithPrefix() {
        // GIVEN
        Telemetry telemetry = createTelemetry();
        telemetry.getLogging().setS3(null);
        telemetry.getLogging().setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        telemetry.getLogging().setStorageLocation("abfs://mycontainer/prefix");
        // WHEN
        underTest.updateStorageLocation(telemetry, FluentClusterType.DATAHUB.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("abfs://mycontainer@null.dfs.core.windows.net/prefix/cluster-logs/datahub/mycluster_12345", telemetry.getLogging().getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationAdlsGen2WithoutScheme() {
        // GIVEN
        Telemetry telemetry = createTelemetry();
        telemetry.getLogging().setS3(null);
        telemetry.getLogging().setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        telemetry.getLogging().setStorageLocation("abfs://mycontainer");
        // WHEN
        underTest.updateStorageLocation(telemetry, FluentClusterType.DATAHUB.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("abfs://mycontainer@null.dfs.core.windows.net/cluster-logs/datahub/mycluster_12345", telemetry.getLogging().getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationWithoutScheme() {
        // GIVEN
        Telemetry telemetry = createTelemetry();
        telemetry.getLogging().setStorageLocation("mybucket");
        // WHEN
        underTest.updateStorageLocation(telemetry, FluentClusterType.DATAHUB.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("s3://mybucket/cluster-logs/datahub/mycluster_12345", telemetry.getLogging().getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationWithoutTelemetry() {
        // GIVEN
        Telemetry telemetry = null;
        // WHEN
        underTest.updateStorageLocation(telemetry, null, null, null);
        // THEN
        assertNull(telemetry);
    }

    @Test(expected = CrnParseException.class)
    public void testUpdateStorageLocationWithInvalidCrn() {
        // GIVEN
        Telemetry telemetry = createTelemetry();
        // WHEN
        underTest.updateStorageLocation(telemetry, FluentClusterType.DATAHUB.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west:someone:stack:12345");
    }

    private Telemetry createTelemetry() {
        Telemetry telemetry = new Telemetry();
        Logging logging = new Logging();
        logging.setStorageLocation("s3://mybucket");
        logging.setS3(new S3CloudStorageV1Parameters());
        telemetry.setLogging(logging);
        return telemetry;
    }

}
