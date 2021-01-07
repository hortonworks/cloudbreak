package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import com.sequenceiq.cloudbreak.telemetry.fluent.FluentClusterType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class GCSBackupConfigGeneratorTest {

    @Test
    public void testSimpleCaseLocation() {
        GCSBackupConfigGenerator gcsBackupConfigGenerator = new GCSBackupConfigGenerator();
        String location = gcsBackupConfigGenerator.generateBackupLocation(
                "gs://mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("gs://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testSuppliedFolderLocation() {
        GCSBackupConfigGenerator gcsBackupConfigGenerator = new GCSBackupConfigGenerator();
        String location = gcsBackupConfigGenerator.generateBackupLocation(
                "gs://mybucket/something",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("gs://mybucket/something/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testNonPrefixedLocation() {
        GCSBackupConfigGenerator gcsBackupConfigGenerator = new GCSBackupConfigGenerator();
        String location = gcsBackupConfigGenerator.generateBackupLocation(
                "mybucket",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("gs://mybucket/cluster-backups/freeipa/mycluster_12345", location);
    }

    @Test
    public void testMultiFolderLocation() {
        GCSBackupConfigGenerator gcsBackupConfigGenerator = new GCSBackupConfigGenerator();
        String location = gcsBackupConfigGenerator.generateBackupLocation(
                "mybucket/something/deeper",
                FluentClusterType.FREEIPA.value(),
                "mycluster",
                "12345");
        assertEquals("gs://mybucket/something/deeper/cluster-backups/freeipa/mycluster_12345", location);
    }
}
