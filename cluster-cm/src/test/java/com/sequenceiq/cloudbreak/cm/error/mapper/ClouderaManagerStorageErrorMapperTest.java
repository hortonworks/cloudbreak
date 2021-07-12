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
import com.sequenceiq.cloudbreak.domain.cloudstorage.AdlsGen2Identity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.GcsIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.S3Identity;
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
        when(cloudStorage.getCloudIdentities()).thenReturn(null);
        when(cloudStorage.getAccountMapping()).thenReturn(accountMapping);
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
    void mapTestWhenRuntimeException() {
        Map<String, String> userMappings = mock(Map.class);
        when(userMappings.get("hive")).thenThrow(new UnsupportedOperationException("No mapping today"));
        accountMapping = mock(AccountMapping.class, withSettings().extraInterfaces(AccountMappingJacksonHack.class));
        when(accountMapping.getUserMappings()).thenReturn(userMappings);
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

        assertThat(result).isEqualTo("Services running on the cluster were unable to write to myRangerAuditLocation location. " +
                "This problem usually occurs due to cloud storage permission misconfiguration. " +
                "Services on the cluster are using Data Access Role (myDataAccessRole) and Ranger Audit Role (myRangerAuditRole) to write to the Ranger " +
                "Audit location (myRangerAuditLocation), " +
                "therefore please verify that these roles have write access to this location. " +
                "During Data Lake cluster creation, CDP Control Plane attaches Assumer Instance Profile () to the IDBroker Virtual Machine. " +
                "IDBroker will then use it to assume the Data Access Role and Ranger Audit Role, therefore Assumer Instance Profile () " +
                "permissions must, at a minimum, allow to assume Data Access Role and Ranger Audit Role." +
                "Refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp/latest/requirements-aws/topics/mc-idbroker-minimum-setup.html for the required rights.");
    }

    @Test
    void mapTestWhenAwsAndNoLocations() {
        S3Identity s3Identity = new S3Identity();
        s3Identity.setInstanceProfile("myInstanceProfile");
        cloudIdentity.setS3Identity(s3Identity);
        cloudStorage.setLocations(List.of());
        fileSystem.setCloudStorage(cloudStorage);

        String result = underTest.map(exception, CloudPlatform.AWS.name(), cluster);

        assertThat(result).isEqualTo("Services running on the cluster were unable to write to  location. " +
                "This problem usually occurs due to cloud storage permission misconfiguration. " +
                "Services on the cluster are using Data Access Role (myDataAccessRole) and Ranger Audit Role (myRangerAuditRole) to write to the Ranger " +
                "Audit location (), " +
                "therefore please verify that these roles have write access to this location. " +
                "During Data Lake cluster creation, CDP Control Plane attaches Assumer Instance Profile (myInstanceProfile) to the IDBroker Virtual Machine. " +
                "IDBroker will then use it to assume the Data Access Role and Ranger Audit Role, therefore Assumer Instance Profile (myInstanceProfile) " +
                "permissions must, at a minimum, allow to assume Data Access Role and Ranger Audit Role." +
                "Refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp/latest/requirements-aws/topics/mc-idbroker-minimum-setup.html for the required rights.");
    }

    @Test
    void mapTestWhenAwsAndSuccess() {
        S3Identity s3Identity = new S3Identity();
        s3Identity.setInstanceProfile("myInstanceProfile");
        cloudIdentity.setS3Identity(s3Identity);
        fileSystem.setCloudStorage(cloudStorage);

        String result = underTest.map(exception, CloudPlatform.AWS.name(), cluster);

        assertThat(result).isEqualTo("Services running on the cluster were unable to write to myRangerAuditLocation location. " +
                "This problem usually occurs due to cloud storage permission misconfiguration. " +
                "Services on the cluster are using Data Access Role (myDataAccessRole) and Ranger Audit Role (myRangerAuditRole) to write to the Ranger " +
                "Audit location (myRangerAuditLocation), " +
                "therefore please verify that these roles have write access to this location. " +
                "During Data Lake cluster creation, CDP Control Plane attaches Assumer Instance Profile (myInstanceProfile) to the IDBroker Virtual Machine. " +
                "IDBroker will then use it to assume the Data Access Role and Ranger Audit Role, therefore Assumer Instance Profile (myInstanceProfile) " +
                "permissions must, at a minimum, allow to assume Data Access Role and Ranger Audit Role." +
                "Refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp/latest/requirements-aws/topics/mc-idbroker-minimum-setup.html for the required rights.");
    }

    @Test
    void mapTestWhenAzureAndNoIdentity() {
        String result = underTest.map(exception, CloudPlatform.AZURE.name(), cluster);

        assertThat(result).isEqualTo("Services running on the cluster were unable to write to myRangerAuditLocation location. " +
                "This problem usually occurs due to cloud storage permission misconfiguration. " +
                "Services on the cluster are using Data Access Identity (myDataAccessRole) and Ranger Audit Identity (myRangerAuditRole) to write to the " +
                "Ranger Audit location (myRangerAuditLocation), therefore please verify that these roles have write access to this location. " +
                "During Data Lake cluster creation, CDP Control Plane attaches Assumer Identity () to the IDBroker Virtual Machine. " +
                "IDBroker will then use it to attach the other managed identities to the IDBroker Virtual Machine, therefore Assumer Identity () " +
                "permissions must, at a minimum, allow to attach the Data Access Identity and Ranger Access Identity. " +
                "Refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp/latest/requirements-azure/topics/mc-az-minimal-setup-for-cloud-storage.html for the required rights.");
    }

    @Test
    void mapTestWhenAzureAndNoLocations() {
        AdlsGen2Identity adlsGen2Identity = new AdlsGen2Identity();
        adlsGen2Identity.setManagedIdentity("myManagedIdentity");
        cloudIdentity.setAdlsGen2Identity(adlsGen2Identity);
        cloudStorage.setLocations(List.of());
        fileSystem.setCloudStorage(cloudStorage);

        String result = underTest.map(exception, CloudPlatform.AZURE.name(), cluster);

        assertThat(result).isEqualTo("Services running on the cluster were unable to write to  location. " +
                "This problem usually occurs due to cloud storage permission misconfiguration. " +
                "Services on the cluster are using Data Access Identity (myDataAccessRole) and Ranger Audit Identity (myRangerAuditRole) to write to the " +
                "Ranger Audit location (), therefore please verify that these roles have write access to this location. " +
                "During Data Lake cluster creation, CDP Control Plane attaches Assumer Identity (myManagedIdentity) to the IDBroker Virtual Machine. " +
                "IDBroker will then use it to attach the other managed identities to the IDBroker Virtual Machine, therefore Assumer Identity " +
                "(myManagedIdentity) " +
                "permissions must, at a minimum, allow to attach the Data Access Identity and Ranger Access Identity. " +
                "Refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp/latest/requirements-azure/topics/mc-az-minimal-setup-for-cloud-storage.html for the required rights.");
    }

    @Test
    void mapTestWhenAzureAndSuccess() {
        AdlsGen2Identity adlsGen2Identity = new AdlsGen2Identity();
        adlsGen2Identity.setManagedIdentity("myManagedIdentity");
        cloudIdentity.setAdlsGen2Identity(adlsGen2Identity);
        fileSystem.setCloudStorage(cloudStorage);

        String result = underTest.map(exception, CloudPlatform.AZURE.name(), cluster);

        assertThat(result).isEqualTo("Services running on the cluster were unable to write to myRangerAuditLocation location. " +
                "This problem usually occurs due to cloud storage permission misconfiguration. " +
                "Services on the cluster are using Data Access Identity (myDataAccessRole) and Ranger Audit Identity (myRangerAuditRole) to write to the " +
                "Ranger Audit location (myRangerAuditLocation), therefore please verify that these roles have write access to this location. " +
                "During Data Lake cluster creation, CDP Control Plane attaches Assumer Identity (myManagedIdentity) to the IDBroker Virtual Machine. " +
                "IDBroker will then use it to attach the other managed identities to the IDBroker Virtual Machine, therefore Assumer Identity " +
                "(myManagedIdentity) " +
                "permissions must, at a minimum, allow to attach the Data Access Identity and Ranger Access Identity. " +
                "Refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp/latest/requirements-azure/topics/mc-az-minimal-setup-for-cloud-storage.html for the required rights.");
    }

    @Test
    void mapTestWhenGcpAndNoIdentity() {
        String result = underTest.map(exception, CloudPlatform.GCP.name(), cluster);

        assertThat(result).isEqualTo("Services running on the cluster were unable to write to myRangerAuditLocation " +
                "location. This problem usually occurs due to cloud storage permission misconfiguration. " +
                "Services on the cluster are using Data Access Service Account (myDataAccessRole) and Ranger " +
                "Audit Service Account (myRangerAuditRole) to write to the Ranger Audit location " +
                "(myRangerAuditLocation), therefore please verify that these roles have write access to this location. " +
                "During Data Lake cluster creation, CDP Control Plane attaches Service Account () to the IDBroker " +
                "Virtual Machine. IDBroker will then use it to assume the Data Access Service Account and Ranger " +
                "Audit Service Account, therefore Assumer Service Account () permissions must, at a minimum, " +
                "allow to assume Data Access Service Account and Ranger Audit Service Account.Refer to Cloudera " +
                "documentation at https://docs.cloudera.com/cdp/latest/requirements-gcp/topics/mc-gcp_minimum_setup_for_cloud_storage.html " +
                "for the required rights.");
    }

    @Test
    void mapTestWhenGcpAndNoLocations() {
        GcsIdentity gcsIdentity = new GcsIdentity();
        gcsIdentity.setServiceAccountEmail("myServiceAccountEmail");
        cloudIdentity.setGcsIdentity(gcsIdentity);
        cloudStorage.setLocations(List.of());
        fileSystem.setCloudStorage(cloudStorage);

        String result = underTest.map(exception, CloudPlatform.GCP.name(), cluster);

        assertThat(result).isEqualTo("Services running on the cluster were unable to write to  location. This problem " +
                "usually occurs due to cloud storage permission misconfiguration. Services on the cluster are " +
                "using Data Access Service Account (myDataAccessRole) and Ranger Audit Service Account " +
                "(myRangerAuditRole) to write to the Ranger Audit location (), therefore please verify that these " +
                "roles have write access to this location. During Data Lake cluster creation, CDP Control Plane " +
                "attaches Service Account (myServiceAccountEmail) to the IDBroker Virtual Machine. IDBroker will " +
                "then use it to assume the Data Access Service Account and Ranger Audit Service Account, therefore " +
                "Assumer Service Account (myServiceAccountEmail) permissions must, at a minimum, allow to assume Data " +
                "Access Service Account and Ranger Audit Service Account.Refer to Cloudera documentation at " +
                "https://docs.cloudera.com/cdp/latest/requirements-gcp/topics/mc-gcp_minimum_setup_for_cloud_storage.html " +
                "for the required rights.");
    }

    @Test
    void mapTestWhenGcpAndSuccess() {
        GcsIdentity gcsIdentity = new GcsIdentity();
        gcsIdentity.setServiceAccountEmail("myServiceAccountEmail");
        cloudIdentity.setGcsIdentity(gcsIdentity);
        fileSystem.setCloudStorage(cloudStorage);

        String result = underTest.map(exception, CloudPlatform.GCP.name(), cluster);

        assertThat(result).isEqualTo("Services running on the cluster were unable to write to myRangerAuditLocation " +
                "location. This problem usually occurs due to cloud storage permission misconfiguration. " +
                "Services on the cluster are using Data Access Service Account (myDataAccessRole) and Ranger " +
                "Audit Service Account (myRangerAuditRole) to write to the Ranger Audit location (myRangerAuditLocation), " +
                "therefore please verify that these roles have write access to this location. During Data Lake " +
                "cluster creation, CDP Control Plane attaches Service Account (myServiceAccountEmail) to the IDBroker " +
                "Virtual Machine. IDBroker will then use it to assume the Data Access Service Account and Ranger Audit " +
                "Service Account, therefore Assumer Service Account (myServiceAccountEmail) permissions must, at a " +
                "minimum, allow to assume Data Access Service Account and Ranger Audit Service Account.Refer to Cloudera " +
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