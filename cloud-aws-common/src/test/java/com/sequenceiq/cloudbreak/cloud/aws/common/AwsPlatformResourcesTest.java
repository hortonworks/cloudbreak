package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.kms.AmazonKmsUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext.Builder;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificateType;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificates;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTable;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.ec2.model.ArchitectureType;
import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.DiskInfo;
import software.amazon.awssdk.services.ec2.model.EbsEncryptionSupport;
import software.amazon.awssdk.services.ec2.model.EbsInfo;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.InstanceStorageInfo;
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo;
import software.amazon.awssdk.services.ec2.model.InstanceTypeOffering;
import software.amazon.awssdk.services.ec2.model.LocationType;
import software.amazon.awssdk.services.ec2.model.MemoryInfo;
import software.amazon.awssdk.services.ec2.model.ProcessorInfo;
import software.amazon.awssdk.services.ec2.model.Region;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.VCpuInfo;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.paginators.DescribeInstanceTypeOfferingsIterable;
import software.amazon.awssdk.services.iam.model.InstanceProfile;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesRequest;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesResponse;
import software.amazon.awssdk.services.kms.model.AliasListEntry;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KmsException;
import software.amazon.awssdk.services.kms.model.ListAliasesRequest;
import software.amazon.awssdk.services.kms.model.ListAliasesResponse;
import software.amazon.awssdk.services.rds.model.Certificate;
import software.amazon.awssdk.services.rds.model.DescribeCertificatesRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AwsPlatformResourcesTest {

    private static final String AZ_NAME = "eu-central-1a";

    private static final String REGION_NAME = "eu-central-1";

    private static final String NOT_ENABLED_REGION_NAME = "not-enabled-region";

    private static final String NOT_ENABLED_AZ_NAME = "not-enabled-az";

    private static final int KMS_KEYS_COUNT = 100;

    @InjectMocks
    private AwsPlatformResources underTest;

    @Mock
    private AwsAvailabilityZoneProvider awsAvailabilityZoneProvider;

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Mock
    private AmazonIdentityManagementClient amazonCFClient;

    @Mock
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AmazonKmsClient amazonKmsClient;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @Mock
    private AwsSubnetIgwExplorer awsSubnetIgwExplorer;

    @Mock
    private MinimalHardwareFilter minimalHardwareFilter;

    @Mock
    private AmazonDynamoDBClient amazonDynamoDB;

    @Mock
    private AwsPageCollector awsPageCollector;

    @Mock
    private AmazonKmsUtil amazonKmsUtil;

    private ExtendedCloudCredential cloudCredential;

    private com.sequenceiq.cloudbreak.cloud.model.Region region;

    @BeforeEach
    public void setUp() {
        AvailabilityZone availabilityZone = AvailabilityZone.builder()
                .zoneName(AZ_NAME).build();

        DescribeInstanceTypesResponse describeInstanceTypesResponse = DescribeInstanceTypesResponse.builder()
                .instanceTypes(List.of(getInstanceTypeInfo("m5.2xlarge")))
                .build();
        DescribeRegionsResponse describeRegionsResponse = DescribeRegionsResponse.builder()
                .regions(Collections.singletonList(Region.builder().regionName(REGION_NAME).build()))
                .build();

        when(awsDefaultZoneProvider.getDefaultZone(any(CloudCredential.class))).thenReturn(REGION_NAME);
        when(awsClient.createEc2Client(any(AwsCredentialView.class))).thenReturn(amazonEC2Client);
        when(awsClient.createEc2Client(any(AwsCredentialView.class), any())).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeRegions(any(DescribeRegionsRequest.class))).thenReturn(describeRegionsResponse);
        when(awsAvailabilityZoneProvider.describeAvailabilityZones(any(), any(), any()))
                .thenReturn(List.of(availabilityZone));

        List<InstanceTypeOffering> offerings = Collections.singletonList(InstanceTypeOffering.builder().instanceType("m5.2xlarge").build());
        DescribeInstanceTypeOfferingsIterable describeInstanceTypeOfferingsIterable = mock(DescribeInstanceTypeOfferingsIterable.class);
        SdkIterable<InstanceTypeOffering> sdkIterable = mock(SdkIterable.class);
        when(sdkIterable.stream()).thenAnswer(invocation -> offerings.stream());
        when(describeInstanceTypeOfferingsIterable.instanceTypeOfferings()).thenReturn(sdkIterable);
        when(amazonEC2Client.describeInstanceTypeOfferings(any()))
                .thenReturn(describeInstanceTypeOfferingsIterable);

        when(amazonEC2Client.describeInstanceTypes(any(DescribeInstanceTypesRequest.class)))
                .thenReturn(describeInstanceTypesResponse);

        ReflectionTestUtils.setField(underTest, "fetchMaxItems", 500);
        region = region(REGION_NAME);
        ReflectionTestUtils.setField(underTest, "enabledRegions", Set.of(region));
        ReflectionTestUtils.setField(underTest, "enabledAvailabilityZones", Set.of(availabilityZone(AZ_NAME)));

        cloudCredential = new ExtendedCloudCredential(
                new CloudCredential("crn", "aws-credential", "account"),
                "AWS",
                null,
                "id",
                new ArrayList<>());

    }

    private InstanceTypeInfo getInstanceTypeInfo(String name) {
        return InstanceTypeInfo.builder()
                .instanceType(name)
                .instanceStorageSupported(true)
                .bareMetal(false)
                .instanceStorageInfo(InstanceStorageInfo.builder()
                        .disks(DiskInfo.builder()
                                .count(2)
                                .sizeInGB(600L).build())
                        .build())
                .ebsInfo(EbsInfo.builder()
                        .encryptionSupport(EbsEncryptionSupport.SUPPORTED).build())
                .vCpuInfo(VCpuInfo.builder()
                        .defaultCores(6).build())
                .processorInfo(ProcessorInfo.builder()
                        .supportedArchitectures(ArchitectureType.X86_64)
                        .build())
                .memoryInfo(MemoryInfo.builder()
                        .sizeInMiB(1024L).build())
                .build();
    }

    @Test
    public void collectAccessConfigsWhenUserIsUnauthorizedToGetInfoThenItShouldReturnEmptyList() {
        AwsServiceException amazonServiceException = AwsServiceException.builder().message("unauthorized.").statusCode(403).build();

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles(any(ListInstanceProfilesRequest.class))).thenThrow(amazonServiceException);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.accessConfigs(cloudCredential, region, new HashMap<>()));
        assertThat(cloudConnectorException).hasMessageStartingWith("unauthorized.");
    }

    @Test
    public void collectAccessConfigsWhenUserGetAmazonExceptionToGetInfoThenItShouldReturnEmptyList() {
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message("Amazon problem.")
                .statusCode(404)
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage("Amazon problem.").build())
                .build();

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles(any(ListInstanceProfilesRequest.class))).thenThrow(amazonServiceException);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.accessConfigs(cloudCredential, region, Collections.emptyMap()));
        assertThat(cloudConnectorException).hasMessageStartingWith("Amazon problem.");
    }

    @Test
    public void collectAccessConfigsWhenUserGetServiceExceptionToGetInfoThenItShouldReturnEmptyList() {
        BadRequestException badRequestException = new BadRequestException("BadRequestException problem.");

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles(any(ListInstanceProfilesRequest.class))).thenThrow(badRequestException);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.accessConfigs(cloudCredential, region, Collections.emptyMap()));
        assertThat(cloudConnectorException).hasMessageContaining("BadRequestException problem.");
    }

    @Test
    public void collectAccessConfigsWhenWeGetBackInfoThenItShouldReturnListWithElements() {
        Set<InstanceProfile> instanceProfileSet = new HashSet<>();
        instanceProfileSet.add(instanceProfile(1));
        instanceProfileSet.add(instanceProfile(2));
        instanceProfileSet.add(instanceProfile(3));
        instanceProfileSet.add(instanceProfile(4));

        ListInstanceProfilesResponse listInstanceProfilesResult = ListInstanceProfilesResponse.builder()
                .instanceProfiles(instanceProfileSet)
                .isTruncated(false)
                .build();

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles(any(ListInstanceProfilesRequest.class))).thenReturn(listInstanceProfilesResult);

        CloudAccessConfigs cloudAccessConfigs = underTest.accessConfigs(cloudCredential, region, Collections.emptyMap());

        assertEquals(4L, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectEncryptionKeysWhenWeGetBackInfoThenItShouldReturnListWithElements() {
        Set<KeyListEntry> listEntries = new HashSet<>();
        listEntries.add(keyListEntry(1));
        listEntries.add(keyListEntry(2));
        listEntries.add(keyListEntry(3));
        listEntries.add(keyListEntry(4));

        Set<AliasListEntry> aliasListEntries = new HashSet<>();
        aliasListEntries.add(aliasListEntry(1));
        aliasListEntries.add(aliasListEntry(2));
        aliasListEntries.add(aliasListEntry(3));
        aliasListEntries.add(aliasListEntry(4));
        ListAliasesResponse listAliasesResponse = ListAliasesResponse.builder().aliases(aliasListEntries).build();

        when(awsClient.createAWSKMS(any(AwsCredentialView.class), anyString())).thenReturn(amazonKmsClient);
        when(amazonKmsUtil.listKeysWithAllPages(amazonKmsClient)).thenReturn(List.copyOf(listEntries));
        when(amazonKmsClient.listAliases(any(ListAliasesRequest.class))).thenReturn(listAliasesResponse);
        KeyMetadata keyMetadata = KeyMetadata.builder().build();
        when(amazonKmsUtil.getKeyMetadataByKeyId(eq(amazonKmsClient), anyString())).thenReturn(keyMetadata);
        when(amazonKmsUtil.extractKeyMetadataMap(keyMetadata)).thenReturn(Map.of());

        CloudEncryptionKeys cloudEncryptionKeys = underTest.encryptionKeys(cloudCredential, region("London"), new HashMap<>());

        assertEquals(4L, cloudEncryptionKeys.getCloudEncryptionKeys().size());
    }

    @Test
    public void isEncryptionKeyUsable() {
        String keyArn = "keyArn";
        String region = "us-east-1";

        when(awsClient.createAWSKMS(any(AwsCredentialView.class), anyString())).thenReturn(amazonKmsClient);
        when(amazonKmsClient.describeKey(any(DescribeKeyRequest.class))).thenThrow(KmsException.class);
        boolean result = underTest.isEncryptionKeyUsable(cloudCredential, region, keyArn);
        assertFalse(result);
        when(amazonKmsClient.describeKey(any(DescribeKeyRequest.class))).thenReturn(DescribeKeyResponse.builder().build());
        result = underTest.isEncryptionKeyUsable(cloudCredential, region, keyArn);
        assertTrue(result);
    }

    @Test
    public void collectEncryptionKeysWhenWeGetBackInfoThenItShouldReturnListWithPaginatedElements() {
        List<KeyListEntry> listEntries = new ArrayList<>();
        for (int i = 0; i <  KMS_KEYS_COUNT; i++) {
            listEntries.add(keyListEntry(i));
        }

        List<AliasListEntry> aliasListEntriesPage1 = new ArrayList<>();
        List<AliasListEntry> aliasListEntriesPage2 = new ArrayList<>();
        for (int i = 0; i < KMS_KEYS_COUNT / 2; i++) {
            aliasListEntriesPage1.add(aliasListEntry(i));
        }

        for (int i = KMS_KEYS_COUNT / 2; i < KMS_KEYS_COUNT; i++) {
            aliasListEntriesPage2.add(aliasListEntry(i));
        }

        ListAliasesResponse listAliasesResultWithMarker = ListAliasesResponse.builder().aliases(aliasListEntriesPage1).nextMarker("testMarker").build();
        ListAliasesResponse listAliasesResult = ListAliasesResponse.builder().aliases(aliasListEntriesPage2).build();

        when(awsClient.createAWSKMS(any(AwsCredentialView.class), anyString())).thenReturn(amazonKmsClient);
        when(amazonKmsUtil.listKeysWithAllPages(amazonKmsClient)).thenReturn(List.copyOf(listEntries));
        when(amazonKmsClient.listAliases(any(ListAliasesRequest.class))).thenReturn(listAliasesResultWithMarker, listAliasesResult);
        KeyMetadata keyMetadata = KeyMetadata.builder().build();
        when(amazonKmsUtil.getKeyMetadataByKeyId(eq(amazonKmsClient), anyString())).thenReturn(keyMetadata);
        when(amazonKmsUtil.extractKeyMetadataMap(keyMetadata)).thenReturn(Map.of());

        CloudEncryptionKeys cloudEncryptionKeys = underTest.encryptionKeys(cloudCredential, region("London"), new HashMap<>());

        assertEquals(KMS_KEYS_COUNT, cloudEncryptionKeys.getCloudEncryptionKeys().size());
    }

    @Test
    public void testVirtualMachinesDisabledTypesEmpty() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.emptyList());

        CloudVmTypes result = underTest.virtualMachines(cloudCredential, region, Collections.emptyMap());

        assertEquals("m5.2xlarge", result.getCloudVmResponses().get(AZ_NAME).iterator().next().value());
    }

    @Test
    public void testVirtualMachinesDisabledTypesContainsEmpty() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.singletonList(""));

        CloudVmTypes result = underTest.virtualMachines(cloudCredential, region, Collections.emptyMap());

        assertEquals("m5.2xlarge", result.getCloudVmResponses().get(AZ_NAME).iterator().next().value());
    }

    @Test
    public void testVirtualMachinesOkStartWith() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.singletonList("m5"));

        CloudVmTypes result = underTest.virtualMachines(cloudCredential, region, Collections.emptyMap());

        assertTrue(result.getCloudVmResponses().get(AZ_NAME).isEmpty());
    }

    @Test
    public void testVirtualMachinesOkFullMatch() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.singletonList("m5.2xlarge"));

        CloudVmTypes result = underTest.virtualMachines(cloudCredential, region, Collections.emptyMap());

        assertTrue(result.getCloudVmResponses().get(AZ_NAME).isEmpty());
    }

    private InstanceProfile instanceProfile(int i) {
        return InstanceProfile.builder()
                .arn(String.format("arn-%s", i))
                .createDate(Instant.now())
                .instanceProfileId(String.format("profileId-%s", i))
                .instanceProfileName(String.format("profileName-%s", i))
                .build();
    }

    private KeyListEntry keyListEntry(int i) {
        return KeyListEntry.builder()
                .keyArn(String.format("key-%s", i))
                .keyId(String.format("%s", i))
                .build();
    }

    private AliasListEntry aliasListEntry(int i) {
        return AliasListEntry.builder()
                .aliasArn(String.format("key-%s", i))
                .aliasName(String.format("%s", i))
                .targetKeyId(String.format("%s", i))
                .build();
    }

    @Test
    public void noSqlTables() {
        when(awsClient.createDynamoDbClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonDynamoDB);
        when(amazonDynamoDB.listTables(any(ListTablesRequest.class))).thenReturn(
                ListTablesResponse.builder().tableNames("a", "b").lastEvaluatedTableName("b").build(),
                ListTablesResponse.builder().tableNames("c", "d").build()
        );

        CloudNoSqlTables cloudNoSqlTables = underTest.noSqlTables(cloudCredential, region("region"), null);
        assertThat(cloudNoSqlTables.getCloudNoSqlTables()).hasSameElementsAs(List.of(
                new CloudNoSqlTable("a"),
                new CloudNoSqlTable("b"),
                new CloudNoSqlTable("c"),
                new CloudNoSqlTable("d")));
    }

    @Test
    public void networksSubnetsShouldBeFilteredByEnabledRegionsAndNotCdpTrialAccount() {
        DescribeRouteTablesResponse routeTables = DescribeRouteTablesResponse.builder().build();
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(routeTables);
        DescribeVpcsResponse vpcs = DescribeVpcsResponse.builder().vpcs(Vpc.builder().build()).build();
        when(amazonEC2Client.describeVpcs(any())).thenReturn(vpcs);
        DescribeSubnetsResponse subnets = DescribeSubnetsResponse.builder()
                .subnets(List.of(Subnet.builder().availabilityZone("not-enabled-az").mapPublicIpOnLaunch(true).build()))
                .build();
        when(entitlementService.cdpTrialEnabled(anyString())).thenReturn(false);
        when(amazonEC2Client.describeSubnets(any())).thenReturn(subnets);
        CloudNetworks cloudNetworks = underTest.networks(cloudCredential, region, Map.of());

        assertThat(cloudNetworks.getCloudNetworkResponses().get(REGION_NAME))
                .allMatch(cloudNetwork -> cloudNetwork.getSubnets().isEmpty());
    }

    @Test
    public void networksSubnetsShouldBeFilteredByEnabledRegionsAndCdpTrialAccount() {
        DescribeRouteTablesResponse routeTables = DescribeRouteTablesResponse.builder().build();
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(routeTables);
        DescribeVpcsResponse vpcs = DescribeVpcsResponse.builder().vpcs(Vpc.builder().build()).build();
        when(amazonEC2Client.describeVpcs(any())).thenReturn(vpcs);
        DescribeSubnetsResponse subnets = DescribeSubnetsResponse.builder()
                .subnets(List.of(Subnet.builder().availabilityZone("not-enabled-az").mapPublicIpOnLaunch(true).build()))
                .build();
        when(entitlementService.cdpTrialEnabled(anyString())).thenReturn(true);
        when(amazonEC2Client.describeSubnets(any())).thenReturn(subnets);
        CloudNetworks cloudNetworks = underTest.networks(cloudCredential, region, Map.of());

        assertThat(cloudNetworks.getCloudNetworkResponses().get(REGION_NAME))
                .allMatch(cloudNetwork -> cloudNetwork.getSubnets().isEmpty());
    }

    @Test
    public void regionsShouldBeFilteredByEnabledRegionsAndEnabledAvailabilityZones() {
        setUpRegions();

        CloudRegions cloudRegions = underTest.regions(cloudCredential, region, Map.of(), true);
        assertThat(cloudRegions.getCloudRegions())
                .containsKey(region)
                .doesNotContainKey(region(NOT_ENABLED_REGION_NAME));
        assertThat(cloudRegions.getCloudRegions().get(region))
                .contains(availabilityZone(AZ_NAME))
                .doesNotContain(availabilityZone(NOT_ENABLED_AZ_NAME));
    }

    private void setUpRegions() {
        DescribeRegionsResponse regions = DescribeRegionsResponse.builder().regions(
                Region.builder().regionName(NOT_ENABLED_REGION_NAME).build(),
                Region.builder().regionName(REGION_NAME).build()).build();
        when(amazonEC2Client.describeRegions(any())).thenReturn(regions);
        when(awsDefaultZoneProvider.getDefaultZone(any(CloudCredential.class))).thenReturn(REGION_NAME);
        when(awsAvailabilityZoneProvider.describeAvailabilityZones(any(), any(), any())).thenReturn(List.of(
                AvailabilityZone.builder().zoneName(NOT_ENABLED_AZ_NAME).build(),
                AvailabilityZone.builder().zoneName(AZ_NAME).build()));
    }

    @Test
    public void virtualMachinesShouldBeFilteredByEnabledAvailabilityZones() {
        setUpRegions();
        DescribeInstanceTypesResponse describeInstanceTypesResponse = DescribeInstanceTypesResponse.builder()
                .instanceTypes(List.of(getInstanceTypeInfo("vm1"), getInstanceTypeInfo("vm2"))).build();
        when(amazonEC2Client.describeInstanceTypes(any(DescribeInstanceTypesRequest.class)))
                .thenReturn(describeInstanceTypesResponse);

        DescribeInstanceTypeOfferingsRequest describeInstanceTypesByAzRequest = DescribeInstanceTypeOfferingsRequest.builder()
                .locationType(LocationType.AVAILABILITY_ZONE)
                .filters(Filter.builder().name("location").values(AZ_NAME).build())
                .build();

        List<InstanceTypeOffering> offerings = List.of(InstanceTypeOffering.builder().instanceType("vm1").build(),
                InstanceTypeOffering.builder().instanceType("vm2").build());
        DescribeInstanceTypeOfferingsIterable describeInstanceTypeOfferingsIterable = mock(DescribeInstanceTypeOfferingsIterable.class);
        SdkIterable<InstanceTypeOffering> sdkIterable = mock(SdkIterable.class);
        when(sdkIterable.stream()).thenAnswer(invocation -> offerings.stream());
        when(describeInstanceTypeOfferingsIterable.instanceTypeOfferings()).thenReturn(sdkIterable);
        when(amazonEC2Client.describeInstanceTypeOfferings(eq(describeInstanceTypesByAzRequest)))
                .thenReturn(describeInstanceTypeOfferingsIterable);

        ReflectionTestUtils.setField(underTest, "defaultVmTypes", Map.of(
                region, vmType("vm1"),
                region(NOT_ENABLED_REGION_NAME), vmType("vm2")));
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", List.of());

        CloudVmTypes cloudVmTypes = underTest.virtualMachines(cloudCredential, region, Map.of());
        assertThat(cloudVmTypes.getCloudVmResponses())
                .containsKey(AZ_NAME)
                .doesNotContainKey(NOT_ENABLED_AZ_NAME);
        assertEquals(2, cloudVmTypes.getCloudVmResponses().get(AZ_NAME).size());
        assertThat(cloudVmTypes.getDefaultCloudVmResponses())
                .containsKey(AZ_NAME)
                .doesNotContainKey(NOT_ENABLED_AZ_NAME);
    }

    @Test
    public void testEncryptionWhenNoEbsInfoShouldReturnFalse() {
        InstanceTypeInfo instanceTypeInfo = InstanceTypeInfo.builder().build();
        boolean encryptionSupported = underTest.getEncryptionSupported(instanceTypeInfo);
        assertFalse(encryptionSupported);
    }

    @Test
    public void testEncryptionWhenEncryptionSupportedNotPresentedShouldReturnFalse() {
        InstanceTypeInfo instanceTypeInfo = InstanceTypeInfo.builder()
                .ebsInfo(EbsInfo.builder().build())
                .build();
        boolean encryptionSupported = underTest.getEncryptionSupported(instanceTypeInfo);
        assertFalse(encryptionSupported);
    }

    @Test
    public void testEncryptionWhenEncryptionSupportedWithSupportedValuePresentedShouldReturnTrue() {
        InstanceTypeInfo instanceTypeInfo = InstanceTypeInfo.builder()
                .ebsInfo(EbsInfo.builder().encryptionSupport("supported").build()).build();
        boolean encryptionSupported = underTest.getEncryptionSupported(instanceTypeInfo);
        assertTrue(encryptionSupported);
    }

    @Test
    public void testEncryptionWhenEncryptionSupportedWithNotSupportedValuePresentedShouldReturnFalse() {
        InstanceTypeInfo instanceTypeInfo = InstanceTypeInfo.builder()
                .ebsInfo(EbsInfo.builder().encryptionSupport("unsupported").build()).build();
        boolean encryptionSupported = underTest.getEncryptionSupported(instanceTypeInfo);
        assertFalse(encryptionSupported);
    }

    @Test
    void databaseServerGeneralSslRootCertificatesTestNPEWhenNullCloudCredential() {
        assertThrows(NullPointerException.class, () -> underTest.databaseServerGeneralSslRootCertificates(null, region));
    }

    @Test
    void databaseServerGeneralSslRootCertificatesTestNPEWhenNullRegion() {
        assertThrows(NullPointerException.class, () -> underTest.databaseServerGeneralSslRootCertificates(cloudCredential, null));
    }

    @Test
    void databaseServerGeneralSslRootCertificatesTestWhenSuccess() {
        Certificate certificate1 = Certificate.builder().certificateIdentifier("cert1").build();
        Certificate certificate2 = Certificate.builder().certificateIdentifier("cert2").customerOverride(true).build();

        AmazonRdsClient amazonRdsClient = mock(AmazonRdsClient.class);
        when(amazonRdsClient.describeCertificates(any(DescribeCertificatesRequest.class))).thenReturn(List.of(certificate1, certificate2));
        when(awsClient.createRdsClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(amazonRdsClient);

        CloudDatabaseServerSslCertificates cloudDatabaseServerSslCertificates = underTest.databaseServerGeneralSslRootCertificates(cloudCredential, region);

        assertThat(cloudDatabaseServerSslCertificates).isNotNull();

        Set<CloudDatabaseServerSslCertificate> sslCertificates = cloudDatabaseServerSslCertificates.sslCertificates();
        assertThat(sslCertificates).isNotNull();
        assertThat(sslCertificates).hasSize(2);
        verifySslRootCertificate(sslCertificates, "cert1", false);
        verifySslRootCertificate(sslCertificates, "cert2", true);
    }

    private void verifySslRootCertificate(Set<CloudDatabaseServerSslCertificate> sslCertificates, String certificateIdentifier, boolean overriddenExpected) {
        Optional<CloudDatabaseServerSslCertificate> match = sslCertificates.stream()
                .filter(c -> certificateIdentifier.equals(c.certificateIdentifier()))
                .findFirst();
        assertThat(match).overridingErrorMessage("No cert found for certificateIdentifier %s", certificateIdentifier).isNotEmpty();

        CloudDatabaseServerSslCertificate sslCertificate = match.get();
        assertThat(sslCertificate.certificateType()).isEqualTo(CloudDatabaseServerSslCertificateType.ROOT);
        assertThat(sslCertificate.overridden()).isEqualTo(overriddenExpected);
    }

    @Test
    void collectInstanceStorageCountTest() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.emptyList());
        CloudContext cloudContext = new Builder().withLocation(Location.location(region, availabilityZone(AZ_NAME))).build();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);

        InstanceStoreMetadata instanceStoreMetadata = underTest.collectInstanceStorageCount(ac, Collections.singletonList("m5.2xlarge"), List.of());

        assertEquals(2, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("m5.2xlarge"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("unsupported"));
    }

    @Test
    void collectInstanceStorageCountWhenInstanceTypeIsNotFoundTest() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.emptyList());
        CloudContext cloudContext = new Builder().withLocation(Location.location(region, availabilityZone(AZ_NAME))).build();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, cloudCredential);

        InstanceStoreMetadata instanceStoreMetadata = underTest.collectInstanceStorageCount(ac, Collections.singletonList("unsupported"), List.of());

        assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("unsupported"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("unsupported"));
        assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("m5.2xlarge"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("m5.2xlarge"));

        instanceStoreMetadata = underTest.collectInstanceStorageCount(ac, new ArrayList<>(), List.of());

        assertNull(instanceStoreMetadata.mapInstanceTypeToInstanceStoreCount("m5.2xlarge"));
        assertEquals(0, instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled("m5.2xlarge"));
    }
}