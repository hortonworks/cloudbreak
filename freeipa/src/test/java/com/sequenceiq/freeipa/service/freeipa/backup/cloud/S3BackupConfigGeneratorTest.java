package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;

/**
 *
 */
public class S3BackupConfigGeneratorTest {

    @Test
    public void testSimpleCaseLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "s3://mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testSuppliedFolderLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "s3://mybucket/something",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/something/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testS3aLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "s3a://mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testNonPrefixedLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testMultiFolderLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "mybucket/something/deeper",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/something/deeper/cluster-backups/freeipa/mycluster_12345", location);
    }
}
