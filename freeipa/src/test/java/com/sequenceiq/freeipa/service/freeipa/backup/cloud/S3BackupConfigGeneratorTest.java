package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;

class S3BackupConfigGeneratorTest {

    @Test
    void testSimpleCaseLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "s3://mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    void testSuppliedFolderLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "s3://mybucket/something",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/something/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    void testS3aLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "s3a://mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    void testNonPrefixedLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    void testMultiFolderLocation() {
        S3BackupConfigGenerator s3BackupConfigGenerator = new S3BackupConfigGenerator();
        String location = s3BackupConfigGenerator.generateBackupLocation(
                "mybucket/something/deeper",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("s3://mybucket/something/deeper/cluster-backups/freeipa/mycluster_12345", location);
    }
}
