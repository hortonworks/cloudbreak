package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;

/**
 *
 */
public class AdlsGen2BackupConfigGeneratorTest {

    @Test
    public void testSimpleCaseLocation() {
        AdlsGen2BackupConfigGenerator generator = new AdlsGen2BackupConfigGenerator();
        String location = generator.generateBackupLocation(
                "abfs://mycontainer@someaccount.dfs.core.windows.net",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("https://someaccount.blob.core.windows.net/mycontainer/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testSuppliedFolderLocation() {
        AdlsGen2BackupConfigGenerator generator = new AdlsGen2BackupConfigGenerator();
        String location = generator.generateBackupLocation(
                "abfs://mycontainer/someplace@someaccount.dfs.core.windows.net",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("https://someaccount.blob.core.windows.net/mycontainer/someplace/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testAbfssLocation() {
        AdlsGen2BackupConfigGenerator generator = new AdlsGen2BackupConfigGenerator();
        String location = generator.generateBackupLocation(
                "abfss://mycontainer@someaccount.dfs.core.windows.net",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("https://someaccount.blob.core.windows.net/mycontainer/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testNonPrefixedLocation() {
        AdlsGen2BackupConfigGenerator generator = new AdlsGen2BackupConfigGenerator();
        String location = generator.generateBackupLocation(
                "mycontainer@someaccount.dfs.core.windows.net",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("https://someaccount.blob.core.windows.net/mycontainer/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testMultiFolderLocation() {
        AdlsGen2BackupConfigGenerator generator = new AdlsGen2BackupConfigGenerator();
        String location = generator.generateBackupLocation(
                "abfs://mycontainer/someplace/deeper@someaccount.dfs.core.windows.net",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("https://someaccount.blob.core.windows.net/mycontainer/someplace/deeper/cluster-backups/freeipa/mycluster_12345", location);
    }
}
