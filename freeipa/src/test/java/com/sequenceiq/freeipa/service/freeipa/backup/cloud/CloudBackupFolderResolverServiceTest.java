package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.freeipa.api.model.Backup;

public class CloudBackupFolderResolverServiceTest {

    private CloudBackupFolderResolverService underTest;

    @Before
    public void setUp() {
        underTest = new CloudBackupFolderResolverService(new S3BackupConfigGenerator(),
                new AdlsGen2BackupConfigGenerator());
    }

    @Test
    public void testUpdateStorageLocationS3() {
        // GIVEN
        Backup backup = createBackup();
        // WHEN
        underTest.updateStorageLocation(backup, FluentClusterType.FREEIPA.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("s3://mybucket/cluster-backups/freeipa/mycluster_12345", backup.getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationAdlsGen2() {
        // GIVEN
        Backup backup = createBackup();
        backup.setS3(null);
        backup.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        backup.setStorageLocation("abfs://mycontainer@someaccount.dfs.core.windows.net");
        // WHEN
        underTest.updateStorageLocation(backup, FluentClusterType.FREEIPA.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("https://someaccount.blob.core.windows.net/mycontainer/cluster-backups/freeipa/mycluster_12345", backup.getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationAdlsGen2WithoutScheme() {
        // GIVEN
        Backup backup = createBackup();
        backup.setS3(null);
        backup.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        backup.setStorageLocation("mycontainer@someaccount");
        // WHEN
        underTest.updateStorageLocation(backup, FluentClusterType.FREEIPA.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("https://someaccount.blob.core.windows.net/mycontainer/cluster-backups/freeipa/mycluster_12345", backup.getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationWithoutScheme() {
        // GIVEN
        Backup backup = createBackup();
        backup.setStorageLocation("mybucket");
        // WHEN
        underTest.updateStorageLocation(backup, FluentClusterType.FREEIPA.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("s3://mybucket/cluster-backups/freeipa/mycluster_12345", backup.getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationWithoutBackup() {
        // GIVEN
        Backup backup = null;
        // WHEN
        underTest.updateStorageLocation(backup, FluentClusterType.FREEIPA.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertNull(backup);
    }

    @Test(expected = CrnParseException.class)
    public void testUpdateStorageLocationWithInvalidCrn() {
        // GIVEN
        Backup backup = createBackup();
        // WHEN
        underTest.updateStorageLocation(backup, FluentClusterType.FREEIPA.value(), "mycluster",
                "crn:cdp:cloudbreak:us-west:someone:stack:12345");
    }

    private Backup createBackup() {
        Backup backup = new Backup();
        backup.setS3(new S3CloudStorageV1Parameters());
        backup.setStorageLocation("s3://mybucket");
        return backup;
    }

}
