package com.sequenceiq.cloudbreak.blueprint.filesystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.api.model.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.blueprint.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.blueprint.filesystem.gcs.GcsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.blueprint.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.blueprint.filesystem.wasb.WasbFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.blueprint.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.blueprint.templates.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public class BlueprintTestUtil {

    private static final String IDENTITY_USER_EMAIL = "identity.user@email.com";

    private BlueprintTestUtil() {
    }

    public static GeneralClusterConfigs generalClusterConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAmbariIp("10.1.1.1");
        generalClusterConfigs.setInstanceGroupsPresented(true);
        generalClusterConfigs.setGatewayInstanceMetadataPresented(false);
        generalClusterConfigs.setClusterName("clustername");
        generalClusterConfigs.setExecutorType(ExecutorType.DEFAULT);
        generalClusterConfigs.setStackName("clustername");
        generalClusterConfigs.setUuid("111-222-333-444");
        generalClusterConfigs.setUserName("username");
        generalClusterConfigs.setPassword("Passw0rd");
        generalClusterConfigs.setNodeCount(1);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.ofNullable("fqdn.loal.com"));
        generalClusterConfigs.setIdentityUserEmail(IDENTITY_USER_EMAIL);
        return generalClusterConfigs;
    }

    public static GeneralClusterConfigs generalClusterConfigs(Cluster cluster) {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAmbariIp(cluster.getAmbariIp());
        generalClusterConfigs.setInstanceGroupsPresented(true);
        generalClusterConfigs.setGatewayInstanceMetadataPresented(true);
        generalClusterConfigs.setClusterName(cluster.getName());
        generalClusterConfigs.setExecutorType(cluster.getExecutorType());
        generalClusterConfigs.setStackName(cluster.getName());
        generalClusterConfigs.setUuid("111-222-333-444");
        generalClusterConfigs.setUserName(cluster.getUserName());
        generalClusterConfigs.setPassword(cluster.getPassword());
        generalClusterConfigs.setNodeCount(1);
        generalClusterConfigs.setIdentityUserEmail(IDENTITY_USER_EMAIL);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.ofNullable("fqdn.loal.com"));
        return generalClusterConfigs;
    }

    public static BlueprintView generalBlueprintView(String blueprintText, String version, String type) {
        return new BlueprintView(blueprintText, version, type);
    }

    public static AmbariDatabase generalAmbariDatabase() {
        return new AmbariDatabase("cloudbreak", "fancy ambari db name", "ambariDB", "10.1.1.2", 5432,
                "Ambar!UserName", "Ambar!Passw0rd");
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
