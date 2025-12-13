package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import static com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType.FREEIPA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.freeipa.api.model.Backup;

@ExtendWith(MockitoExtension.class)
public class CloudBackupFolderResolverServiceTest {

    @Spy
    private S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();

    @Spy
    private GCSBackupConfigGenerator gcsBackupConfigGenerator = new GCSBackupConfigGenerator();

    @Spy
    private AdlsGen2BackupConfigGenerator adlsGen2BackupConfigGenerator = new AdlsGen2BackupConfigGenerator();

    @InjectMocks
    private CloudBackupFolderResolverService underTest;

    @Test
    public void testUpdateStorageLocationS3() {
        // GIVEN
        Backup backup = createBackup();
        // WHEN
        underTest.updateStorageLocation(
                backup,
                FREEIPA.value(),
                "mycluster",
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
        underTest.updateStorageLocation(
                backup,
                FREEIPA.value(),
                "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("https://someaccount.dfs.core.windows.net/mycontainer/cluster-backups/freeipa/mycluster_12345", backup.getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationGcs() {
        // GIVEN
        Backup backup = createBackup();
        backup.setS3(null);
        backup.setGcs(new GcsCloudStorageV1Parameters());
        backup.setStorageLocation("gs://mybucket");
        // WHEN
        underTest.updateStorageLocation(
                backup,
                FREEIPA.value(),
                "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("gs://mybucket/cluster-backups/freeipa/mycluster_12345", backup.getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationAdlsGen2WithoutScheme() {
        // GIVEN
        Backup backup = createBackup();
        backup.setS3(null);
        backup.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        backup.setStorageLocation("mycontainer@someaccount");
        // WHEN
        underTest.updateStorageLocation(
                backup,
                FREEIPA.value(),
                "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("https://someaccount.dfs.core.windows.net/mycontainer/cluster-backups/freeipa/mycluster_12345", backup.getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationWithoutScheme() {
        // GIVEN
        Backup backup = createBackup();
        backup.setStorageLocation("mybucket");
        // WHEN
        underTest.updateStorageLocation(
                backup,
                FREEIPA.value(),
                "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertEquals("s3://mybucket/cluster-backups/freeipa/mycluster_12345", backup.getStorageLocation());
    }

    @Test
    public void testUpdateStorageLocationWithoutBackup() {
        // GIVEN
        Backup backup = null;
        // WHEN
        underTest.updateStorageLocation(
                backup,
                FREEIPA.value(),
                "mycluster",
                "crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        // THEN
        assertNull(backup);
    }

    @Test
    public void testUpdateStorageLocationWithInvalidCrn() {
        // GIVEN
        Backup backup = createBackup();
        // WHEN
        assertThrows(CrnParseException.class, () -> underTest.updateStorageLocation(
                backup,
                FREEIPA.value(),
                "mycluster",
                "crn:cdp:cloudbreak:us-west:someone:stack:12345"));
    }

    private Backup createBackup() {
        Backup backup = new Backup();
        backup.setS3(new S3CloudStorageV1Parameters());
        backup.setStorageLocation("s3://mybucket");
        return backup;
    }

}
