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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cm.exception.CloudStorageConfigurationFailedException;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.AccountMapping;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.StorageLocation;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerStorageErrorMapperTest {

    private static final String EXCEPTION_MESSAGE = "Serious problem.";

    private ClouderaManagerStorageErrorMapper underTest;

    private CloudStorageConfigurationFailedException exception;

    private StackDtoDelegate stack;

    private Cluster cluster;

    private FileSystem fileSystem;

    private CloudStorage cloudStorage;

    private CloudIdentity cloudIdentity;

    private AccountMapping accountMapping;

    @Mock
    private CommandDetails commandDetails;

    @BeforeEach
    void setUp() {
        underTest = new ClouderaManagerStorageErrorMapper();
        exception = new CloudStorageConfigurationFailedException(EXCEPTION_MESSAGE);

        stack = mock();
        cluster = new Cluster();
        when(stack.getCluster()).thenReturn(cluster);
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
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        String result = underTest.map(stack, List.of(commandDetails), message);

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void mapTestWhenNoFileSystem() {
        cluster.setFileSystem(null);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        String result = underTest.map(stack, List.of(commandDetails), EXCEPTION_MESSAGE);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    void mapTestWhenNoCloudStorage() {
        cluster.setFileSystem(new FileSystem());
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        String result = underTest.map(stack, List.of(commandDetails), EXCEPTION_MESSAGE);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    void mapTestWhenNoCloudIdentities() {
        cloudStorage = mock(CloudStorage.class, withSettings().extraInterfaces(CloudStorageJacksonHack.class));
        fileSystem = mock(FileSystem.class);
        when(fileSystem.getCloudStorage()).thenReturn(cloudStorage);
        cluster.setFileSystem(fileSystem);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        String result = underTest.map(stack, List.of(commandDetails), EXCEPTION_MESSAGE);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    void mapTestWhenNoAccountMapping() {
        cloudStorage.setAccountMapping(null);
        fileSystem.setCloudStorage(cloudStorage);
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        String result = underTest.map(stack, List.of(commandDetails), EXCEPTION_MESSAGE);

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
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        String result = underTest.map(stack, List.of(commandDetails), EXCEPTION_MESSAGE);

        assertThat(result).isEqualTo(EXCEPTION_MESSAGE);
    }

    @Test
    void mapTestWhenAwsAndNoIdentity() {
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());

        String result = underTest.map(stack, List.of(commandDetails), EXCEPTION_MESSAGE);

        assertThat(result).isEqualTo("Serious problem. Services running on the cluster were unable to write to the cloud storage. " +
                "Please refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-aws/topics/mc-idbroker-minimum-setup.html#mc-idbroker-minimum-setup" +
                " for the required rights.");
    }

    @Test
    void mapTestWhenAzureAndNoIdentity() {
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.AZURE.name());

        String result = underTest.map(stack, List.of(commandDetails), EXCEPTION_MESSAGE);

        assertThat(result).isEqualTo("Serious problem. Services running on the cluster were unable to write to the cloud storage. " +
                "Please refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-azure/topics/" +
                "mc-az-minimal-setup-for-cloud-storage.html#mc-az-minimal-setup-for-cloud-storage for the required rights.");
    }

    @Test
    void mapTestWhenGcpAndNoIdentity() {
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.GCP.name());

        String result = underTest.map(stack, List.of(commandDetails), EXCEPTION_MESSAGE);

        assertThat(result).isEqualTo("Serious problem. Services running on the cluster were unable to write to the cloud storage. Please refer to Cloudera " +
                "documentation at https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-gcp/topics/" +
                "mc-gcp_minimum_setup_for_cloud_storage.html#mc-gcp_minimum_setup_for_cloud_storage " +
                "for the required rights.");
    }

    @Test
    void mapTestWhenNotSupportedCloudPlatform() {
        when(stack.getCloudPlatform()).thenReturn(CloudPlatform.YARN.name());

        String result = underTest.map(stack, List.of(commandDetails), EXCEPTION_MESSAGE);

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