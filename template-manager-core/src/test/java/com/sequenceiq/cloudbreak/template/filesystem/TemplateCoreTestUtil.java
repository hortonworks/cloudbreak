package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.gcs.GcsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.wasb.WasbFileSystemConfigurationsView;

public class TemplateCoreTestUtil {

    private static final String IDENTITY_USER_EMAIL = "identity.user@email.com";

    private TemplateCoreTestUtil() {
    }

    public static GcsFileSystem gcsFileSystem() {
        GcsFileSystem gcsFileSystem = new GcsFileSystem();
        gcsFileSystem.setServiceAccountEmail("serviceaccountemail");
        return gcsFileSystem;
    }

    public static GcsFileSystemConfigurationsView gcsFileSystemConfiguration(Collection<StorageLocationView> storageLocationViews) {
        return new GcsFileSystemConfigurationsView(gcsFileSystem(), storageLocationViews, false);
    }

    public static S3FileSystem s3FileSystem() {
        S3FileSystem s3FileSystem = new S3FileSystem();
        s3FileSystem.setInstanceProfile("instanceprofile");
        return s3FileSystem;
    }

    public static S3FileSystemConfigurationsView s3FileSystemConfiguration(Collection<StorageLocationView> storageLocationViews) {
        return new S3FileSystemConfigurationsView(s3FileSystem(), storageLocationViews, false);
    }

    public static WasbFileSystem wasbFileSystem(boolean secure) {
        WasbFileSystem wasbFileSystem = new WasbFileSystem();
        wasbFileSystem.setAccountKey("accountkey");
        wasbFileSystem.setAccountName("accountname");
        wasbFileSystem.setSecure(secure);
        return wasbFileSystem;
    }

    public static WasbFileSystemConfigurationsView wasbSecureFileSystemConfiguration(Collection<StorageLocationView> storageLocationViews) {
        return new WasbFileSystemConfigurationsView(wasbFileSystem(true), storageLocationViews, false);
    }

    public static WasbFileSystemConfigurationsView wasbUnSecureFileSystemConfiguration(Collection<StorageLocationView> storageLocationViews) {
        return new WasbFileSystemConfigurationsView(wasbFileSystem(false), storageLocationViews, false);

    }

    public static AdlsFileSystem adlsFileSystem() {
        AdlsFileSystem adlsFileSystem = new AdlsFileSystem();
        adlsFileSystem.setClientId("clientid");
        adlsFileSystem.setAccountName("accountname");
        adlsFileSystem.setCredential("1");
        adlsFileSystem.setTenantId("tenantid");
        return adlsFileSystem;
    }

    public static AdlsFileSystemConfigurationsView adlsFileSystemConfiguration(Collection<StorageLocationView> storageLocationViews) {
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), storageLocationViews, false);
        adlsFileSystemConfigurationsView.setAdlsTrackingClusterNameKey("normal-cluster");
        adlsFileSystemConfigurationsView.setAdlsTrackingClusterTypeKey("normal-cluster-type");
        adlsFileSystemConfigurationsView.setResourceGroupName("group");
        return adlsFileSystemConfigurationsView;
    }

    public static List<StorageLocationView> emptyStorageLocationViews() {
        return new ArrayList<>();
    }

    public static List<StorageLocationView> storageLocationViews() {
        List<StorageLocationView> storageLocations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            storageLocations.add(storageLocationView(storageLocation(i)));
        }
        return storageLocations;
    }

    public static List<StorageLocationView> storageLocationViewsWithDuplicatedKey() {
        List<StorageLocationView> storageLocations = new ArrayList<>();
        storageLocations.add(storageLocationView(storageLocation(0)));
        StorageLocation storageLocation = storageLocation(1);
        storageLocation.setConfigFile("0_file");
        storageLocations.add(storageLocationView(storageLocation));
        return storageLocations;
    }

    public static StorageLocation storageLocation(int i) {
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setValue(i + "_test/test/end");
        storageLocation.setProperty(i + "_property");
        storageLocation.setConfigFile(i + "_file");
        return storageLocation;
    }

    public static StorageLocationView storageLocationView(StorageLocation storageLocation) {
        return new StorageLocationView(storageLocation);
    }

}
