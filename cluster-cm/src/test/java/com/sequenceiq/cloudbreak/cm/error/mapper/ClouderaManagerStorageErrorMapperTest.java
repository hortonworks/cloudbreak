package com.sequenceiq.cloudbreak.cm.error.mapper;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cm.exception.CloudStorageConfigurationFailedException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.AccountMapping;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;

class ClouderaManagerStorageErrorMapperTest {

    private static final String EXCEPTION_MESSAGE = "Serious problem.";

    private ClouderaManagerStorageErrorMapper underTest;

    private CloudStorageConfigurationFailedException exception;

    private Cluster cluster;

    private FileSystem fileSystem;

    private CloudStorage cloudStorage;

    private CloudIdentity cloudIdentity;

    private AccountMapping accountMapping;

    @BeforeEach
    void setUp() {
        underTest = new ClouderaManagerStorageErrorMapper();
        exception = new CloudStorageConfigurationFailedException(EXCEPTION_MESSAGE);

        cluster = new Cluster();
        fileSystem = new FileSystem();
        cloudStorage = new CloudStorage();
        cloudIdentity = new CloudIdentity();
        cloudIdentity.setIdentityType(CloudIdentityType.ID_BROKER);
        cloudStorage.setCloudIdentities(List.of(cloudIdentity));
        accountMapping = new AccountMapping();
        accountMapping.setUserMappings(Map.ofEntries(entry("hive", "myDataAccessRole"), entry("solr", "myRangerAuditRole")));
        cloudStorage.setAccountMapping(accountMapping);
        StorageLocation locationRangerAudit = new StorageLocation();
        locationRangerAudit.setType(CloudStorageCdpService.RANGER_AUDIT);
        locationRangerAudit.setValue("myRangerAuditLocation");
        cloudStorage.setLocations(List.of(locationRangerAudit));
        fileSystem.setCloudStorage(cloudStorage);
        cluster.setFileSystem(fileSystem);
    }

    static Object[][] mapTestWhenRazDataProvider() {
        return new Object[][]{
                // testCaseName message expectedResult
                {"message=null", null, "Ranger RAZ is enabled on this cluster."},
                {"message=\"\"", "", "Ranger RAZ is enabled on this cluster."},
                {"message=\"Serious problem without trailing period\"", "Serious problem without trailing period",
                        "Serious problem without trailing period. Ranger RAZ is enabled on this cluster."},
                {"message=EXCEPTION_MESSAGE", EXCEPTION_MESSAGE, EXCEPTION_MESSAGE + " Ranger RAZ is enabled on this cluster."},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mapTestWhenRazDataProvider")
    void mapTestWhenRaz(String testCaseName, String message, String expectedResult) {
        exception = new CloudStorageConfigurationFailedException(message);
        cluster.setRangerRazEnabled(true);

        String result = underTest.map(exception, CloudPlatform.AWS.name(), cluster);

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void mapTestWhenNoFileSystem() {
        cluster.setFileSystem(null);

        String result = underTest.map(exception, CloudPlatform.AWS.name(), cluster);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    void mapTestWhenNoCloudStorage() {
        cluster.setFileSystem(new FileSystem());

        String result = underTest.map(exception, CloudPlatform.AWS.name(), cluster);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    void mapTestWhenNoCloudIdentities() {
        cloudStorage = mock(CloudStorage.class, withSettings().extraInterfaces(CloudStorageJacksonHack.class));
        fileSystem = mock(FileSystem.class);
        when(fileSystem.getCloudStorage()).thenReturn(cloudStorage);
        cluster.setFileSystem(fileSystem);

        String result = underTest.map(exception, CloudPlatform.AWS.name(), cluster);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    void mapTestWhenNoAccountMapping() {
        cloudStorage.setAccountMapping(null);
        fileSystem.setCloudStorage(cloudStorage);

        String result = underTest.map(exception, CloudPlatform.AWS.name(), cluster);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    void mapTestWhenNoUserMappings() {
        accountMapping = mock(AccountMapping.class, withSettings().extraInterfaces(AccountMappingJacksonHack.class));
        when(accountMapping.getUserMappings()).thenReturn(null);
        cloudStorage = mock(CloudStorage.class, withSettings().extraInterfaces(CloudStorageJacksonHack.class));
        when(cloudStorage.getCloudIdentities()).thenReturn(List.of(cloudIdentity));
        when(cloudStorage.getAccountMapping()).thenReturn(accountMapping);
        fileSystem = mock(FileSystem.class);
        when(fileSystem.getCloudStorage()).thenReturn(cloudStorage);
        cluster.setFileSystem(fileSystem);

        String result = underTest.map(exception, CloudPlatform.AWS.name(), cluster);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    void mapTestWhenAwsAndNoIdentity() {
        String result = underTest.map(exception, CloudPlatform.AWS.name(), cluster);

        assertThat(result).isEqualTo("Serious problem. Services running on the cluster were unable to write to the cloud storage. " +
                "Please refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp/latest/requirements-aws/topics/mc-idbroker-minimum-setup.html for the required rights.");
    }

    @Test
    void mapTestWhenAzureAndNoIdentity() {
        String result = underTest.map(exception, CloudPlatform.AZURE.name(), cluster);

        assertThat(result).isEqualTo("Serious problem. Services running on the cluster were unable to write to the cloud storage. " +
                "Please refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp/latest/requirements-azure/topics/mc-az-minimal-setup-for-cloud-storage.html for the required rights.");
    }

    @Test
    void mapTestWhenGcpAndNoIdentity() {
        String result = underTest.map(exception, CloudPlatform.GCP.name(), cluster);

        assertThat(result).isEqualTo("Serious problem. Services running on the cluster were unable to write to the cloud storage. Please refer to Cloudera " +
                "documentation at https://docs.cloudera.com/cdp/latest/requirements-gcp/topics/mc-gcp_minimum_setup_for_cloud_storage.html " +
                "for the required rights.");
    }

    @Test
    void mapTestWhenNotSupportedCloudPlatform() {
        String result = underTest.map(exception, CloudPlatform.YARN.name(), cluster);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    // Nasty hacks to avoid "com.fasterxml.jackson.core.JsonProcessingException: Infinite recursion (StackOverflowError)"
    // See https://stackoverflow.com/questions/22851462/infinite-recursion-when-serializing-objects-with-jackson-and-mockito/43864854

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    protected interface CloudStorageJacksonHack {
        @JsonProperty
        String getS3GuardDynamoTableName();

        @JsonProperty
        List<StorageLocation> getLocations();

        @JsonProperty
        List<CloudIdentity> getCloudIdentities();

        @JsonProperty
        AccountMapping getAccountMapping();
    }

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    protected interface AccountMappingJacksonHack {
        @JsonProperty
        Map<String, String> getGroupMappings();

        @JsonProperty
        Map<String, String> getUserMappings();
    }

}