package com.sequenceiq.cloudbreak.recipe.moduletest;

import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static com.sequenceiq.cloudbreak.TestUtil.storageLocation;
import static com.sequenceiq.cloudbreak.recipe.util.RecipeTestUtil.generalClusterDefinitionView;
import static com.sequenceiq.cloudbreak.recipe.util.RecipeTestUtil.generalClusterConfigs;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.recipe.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.GcsFileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.S3FileSystem;
import com.sequenceiq.cloudbreak.services.filesystem.WasbFileSystem;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.adlsgen2.AdlsGen2FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.gcs.GcsFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.s3.S3FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.wasb.WasbFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class RecipeModulTestModelProvider {

    private RecipeModulTestModelProvider() {
    }

    static TemplatePreparationObject testTemplatePreparationObject() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(DatabaseType.HIVE))))
                .build();
    }

    static TemplatePreparationObject testTemplateWithLocalLdap() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withLdapConfig(ldapConfig(), "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject testTemplateWithLongLdapUrl() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setServerHost(String.format("%s%s", StringUtils.repeat("some-superlong-content", "-", 93), ".com"));
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withLdapConfig(ldapConfig, "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject testTemplateWithInvalidLdapUrl() {
        LdapConfig ldapConfig = ldapConfig();
        ldapConfig.setServerHost("\\");
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withLdapConfig(ldapConfig, "cn=admin,dc=example,dc=org", "admin")
                .build();
    }

    static TemplatePreparationObject testTemplateWithSingleS3Storage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getS3ConfigView(getStorageLocationViews(1)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithTwoS3Storage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getS3ConfigView(getStorageLocationViews(2)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithSingleGcsStorage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getGcsConfigView(getStorageLocationViews(1)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithTwoGcsStorage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getGcsConfigView(getStorageLocationViews(2)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithSingleAdlsGen2Storage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getAdlsGen2ConfigView(getStorageLocationViews(1)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithTwoAdlsGen2Storage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getAdlsGen2ConfigView(getStorageLocationViews(2)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithSingleAdlsStorage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getAdlsConfigView(getStorageLocationViews(1)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithTwoAdlsStorage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getAdlsConfigView(getStorageLocationViews(2)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithSingleWasbStorage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getWasbConfigView(getStorageLocationViews(1)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithTwoWasbStorage() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withFileSystemConfigurationView(getWasbConfigView(getStorageLocationViews(2)))
                .build();
    }

    static TemplatePreparationObject testTemplateWithDruidRds() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.DRUID), rdsConfig(DatabaseType.HIVE)))
                .build();
    }

    static TemplatePreparationObject testTemplateWhenSharedServiceIsOnWithRangerAndHiveRds() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.RANGER), rdsConfig(DatabaseType.HIVE)))
                .withSharedServiceConfigs(datalakeSharedServiceConfig())
                .build();
    }

    static TemplatePreparationObject testTemplateWithNoSharedServiceAndRds() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>())
                .withSharedServiceConfigs(null)
                .build();
    }

    static TemplatePreparationObject testTemplateWhenSharedServiceIsOnWithOnlyHiveRds() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.HIVE)))
                .withSharedServiceConfigs(datalakeSharedServiceConfig())
                .build();
    }

    static TemplatePreparationObject testTemplateWhenSharedServiceIsOnWithOnlyRangerRds() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.RANGER)))
                .withSharedServiceConfigs(datalakeSharedServiceConfig())
                .build();
    }

    static TemplatePreparationObject testTemplateWhenBlueprintVersionIs25() {
        return getPreparedBuilder("master")
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.5", "HDP"))
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.RANGER)))
                .withSharedServiceConfigs(datalakeSharedServiceConfig())
                .build();
    }

    static Builder getPreparedBuilder(String... hostNames) {
        SmartSenseSubscription smartSenseSubscription = new SmartSenseSubscription();
        smartSenseSubscription.setSubscriptionId("A-99900000-C-00000000");
        return Builder.builder()
                .withGeneralClusterConfigs(generalClusterConfigs())
                .withClusterDefinitionView(generalClusterDefinitionView("", "2.6", "HDP"))
                .withSmartSenseSubscription(smartSenseSubscription)
                .withSharedServiceConfigs(datalakeSharedServiceConfig())
                .withRdsConfigs(Sets.newHashSet(rdsConfig(DatabaseType.RANGER), rdsConfig(DatabaseType.HIVE)))
                .withHostgroups(hostNames.length == 0 ? getHostGroups("master", "worker", "compute") : getHostGroups(hostNames));
    }

    private static S3FileSystemConfigurationsView getS3ConfigView(Collection<StorageLocationView> locationViews) {
        var fs = new S3FileSystem();
        fs.setInstanceProfile("InstanceProfileValue");
        fs.setStorageContainer("someContainer");
        return new S3FileSystemConfigurationsView(fs, locationViews, false);
    }

    private static GcsFileSystemConfigurationsView getGcsConfigView(Collection<StorageLocationView> locationViews) {
        var fs = new GcsFileSystem();
        fs.setServiceAccountEmail("some.account@email.address.com");
        fs.setStorageContainer("someContainer");
        return new GcsFileSystemConfigurationsView(fs, locationViews, false);
    }

    private static AdlsGen2FileSystemConfigurationsView getAdlsGen2ConfigView(Collection<StorageLocationView> locationViews) {
        var fs = new AdlsGen2FileSystem();
        fs.setAccountKey("someKeyValue");
        fs.setAccountName("nameOfAccount");
        fs.setStorageContainerName("ContainerName");
        fs.setStorageContainer("StorageContainer");
        return new AdlsGen2FileSystemConfigurationsView(fs, locationViews, false);
    }

    private static AdlsFileSystemConfigurationsView getAdlsConfigView(Collection<StorageLocationView> locationViews) {
        var fs = new AdlsFileSystem();
        fs.setAccountName("nameOfAccount");
        fs.setClientId("123456789");
        fs.setCredential("credentialName");
        fs.setTenantId("412145-23523523-235235");
        fs.setStorageContainer("StorageContainer");
        return new AdlsFileSystemConfigurationsView(fs, locationViews, false);
    }

    private static WasbFileSystemConfigurationsView getWasbConfigView(Collection<StorageLocationView> locationViews) {
        var fs = new WasbFileSystem();
        fs.setAccountKey("SomeAccountKey");
        fs.setAccountName("NameOfAccount");
        fs.setSecure(true);
        fs.setStorageContainerName("ContainerName");
        fs.setStorageContainer("StorageContainer");
        return new WasbFileSystemConfigurationsView(fs, locationViews, false);
    }

    private static Collection<StorageLocationView> getStorageLocationViews(int quantity) {
        var views = new LinkedHashSet<StorageLocationView>(quantity);
        for (int i = 1; i <= quantity; i++) {
            views.add(new StorageLocationView(storageLocation(i)));
        }
        return views;
    }

    private static SharedServiceConfigsView datalakeSharedServiceConfig() {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        sharedServiceConfigsView.setDatalakeCluster(true);
        return sharedServiceConfigsView;
    }

    private static Set<HostGroup> getHostGroups(String... names) {
        Set<HostGroup> hostGroups = new HashSet<>(names.length);
        for (String name : names) {
            hostGroups.add(hostGroup(name));
        }
        return hostGroups;
    }

    static TestFile getTestFile(String fileName) throws IOException {
        return new TestFile(new File(fileName).toPath(), FileReaderUtils.readFileFromClasspath(fileName));
    }

    static String getFileName(String folder, String filename) {
        return folder + '/' + filename + ".recipe";
    }

}