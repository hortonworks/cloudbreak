package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;

class GCSBackupConfigGeneratorTest {

    @Test
    void testSimpleCaseLocation() {
        GCSBackupConfigGenerator gcsBackupConfigGenerator = new GCSBackupConfigGenerator();
        String location = gcsBackupConfigGenerator.generateBackupLocation(
                "gs://mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("gs://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    void testSuppliedFolderLocation() {
        GCSBackupConfigGenerator gcsBackupConfigGenerator = new GCSBackupConfigGenerator();
        String location = gcsBackupConfigGenerator.generateBackupLocation(
                "gs://mybucket/something",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("gs://mybucket/something/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    void testNonPrefixedLocation() {
        GCSBackupConfigGenerator gcsBackupConfigGenerator = new GCSBackupConfigGenerator();
        String location = gcsBackupConfigGenerator.generateBackupLocation(
                "mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("gs://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    void testMultiFolderLocation() {
        GCSBackupConfigGenerator gcsBackupConfigGenerator = new GCSBackupConfigGenerator();
        String location = gcsBackupConfigGenerator.generateBackupLocation(
                "mybucket/something/deeper",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("gs://mybucket/something/deeper/cluster-backups/freeipa/mycluster_12345", location);
    }
}
