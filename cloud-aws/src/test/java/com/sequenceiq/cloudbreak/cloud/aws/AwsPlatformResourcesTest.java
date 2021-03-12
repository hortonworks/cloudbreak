package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceTypeOfferingsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypeOfferingsResult;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceTypesResult;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.DiskInfo;
import com.amazonaws.services.ec2.model.EbsEncryptionSupport;
import com.amazonaws.services.ec2.model.EbsInfo;
import com.amazonaws.services.ec2.model.InstanceStorageInfo;
import com.amazonaws.services.ec2.model.InstanceTypeInfo;
import com.amazonaws.services.ec2.model.InstanceTypeOffering;
import com.amazonaws.services.ec2.model.MemoryInfo;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.VCpuInfo;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.ListAliasesRequest;
import com.amazonaws.services.kms.model.ListAliasesResult;
import com.amazonaws.services.kms.model.ListKeysRequest;
import com.amazonaws.services.kms.model.ListKeysResult;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTable;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;

@RunWith(MockitoJUnitRunner.class)
public class AwsPlatformResourcesTest {

    private static final String AZ_NAME = "eu-central-1a";

    private static final String REGION_NAME = "eu-central-1";

    private static final String NOT_ENABLED_REGION_NAME = "not-enabled-region";

    private static final String NOT_ENABLED_AZ_NAME = "not-enabled-az";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private AwsPlatformResources underTest;

    @Mock
    private AwsAvailabilityZoneProvider awsAvailabilityZoneProvider;

    @Mock
    private AwsClient awsClient;

    @Mock
    private AmazonIdentityManagementClient amazonCFClient;

    @Mock
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Mock
    private AmazonKmsClient awskmsClient;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @Mock
    private AwsSubnetIgwExplorer awsSubnetIgwExplorer;

    @Mock
    private DescribeRegionsResult describeRegionsResult;

    @Mock
    private DescribeAvailabilityZonesResult describeAvailabilityZonesResult;

    @Mock
    private DescribeInstanceTypesResult describeInstanceTypesResult;

    @Mock
    private DescribeInstanceTypeOfferingsResult describeInstanceTypeOfferingsResult;

    private final Region region = new Region();

    private final InstanceTypeOffering instanceTypeOffering = new InstanceTypeOffering();

    private final List<InstanceTypeInfo> instanceTypeInfos = new ArrayList<>();

    private final AvailabilityZone availabilityZone = new AvailabilityZone();

    @Mock
    private AmazonDynamoDBClient amazonDynamoDB;

    @Before
    public void setUp() {
        availabilityZone.setZoneName(AZ_NAME);
        region.setRegionName(REGION_NAME);
        instanceTypeOffering.setInstanceType("m5.2xlarge");

        instanceTypeInfos.add(getInstanceTypeInfo("m5.2xlarge"));
        describeInstanceTypesResult.setInstanceTypes(instanceTypeInfos);

        Mockito.reset(awsClient);

        when(awsDefaultZoneProvider.getDefaultZone(any(CloudCredential.class))).thenReturn(REGION_NAME);
        when(awsClient.createEc2Client(any(AwsCredentialView.class))).thenReturn(amazonEC2Client);
        when(awsClient.createEc2Client(any(AwsCredentialView.class), any())).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeRegions(any(DescribeRegionsRequest.class))).thenReturn(describeRegionsResult);
        when(describeRegionsResult.getRegions()).thenReturn(Collections.singletonList(region));
        when(awsAvailabilityZoneProvider.describeAvailabilityZones(any(), any(), any()))
                .thenReturn(List.of(availabilityZone));
        when(amazonEC2Client.describeInstanceTypeOfferings(any(DescribeInstanceTypeOfferingsRequest.class)))
                .thenReturn(describeInstanceTypeOfferingsResult);
        when(amazonEC2Client.describeInstanceTypes(any(DescribeInstanceTypesRequest.class)))
                .thenReturn(describeInstanceTypesResult);
        when(describeInstanceTypesResult.getInstanceTypes()).thenReturn(instanceTypeInfos);
        when(describeInstanceTypeOfferingsResult.getInstanceTypeOfferings())
                .thenReturn(Collections.singletonList(instanceTypeOffering));

        ReflectionTestUtils.setField(underTest, "fetchMaxItems", 500);
        ReflectionTestUtils.setField(underTest, "enabledRegions", Set.of(region(REGION_NAME)));
        ReflectionTestUtils.setField(underTest, "enabledAvailabilityZones", Set.of(availabilityZone(AZ_NAME)));
    }

    private InstanceTypeInfo getInstanceTypeInfo(String name) {
        return new InstanceTypeInfo()
                .withInstanceType(name)
                .withInstanceStorageSupported(true)
                .withBareMetal(false)
                .withInstanceStorageInfo(new InstanceStorageInfo()
                        .withDisks(new DiskInfo()
                                .withCount(2)
                                .withSizeInGB(600L)))
                .withEbsInfo(new EbsInfo()
                        .withEncryptionSupport(EbsEncryptionSupport.Supported))
                .withVCpuInfo(new VCpuInfo()
                        .withDefaultCores(6))
                .withMemoryInfo(new MemoryInfo()
                        .withSizeInMiB(1024L));
    }

    @Test
    public void collectAccessConfigsWhenUserIsUnathorizedToGetInfoThenItShouldReturnEmptyList() {
        AmazonServiceException amazonServiceException = new AmazonServiceException("unauthorized.");
        amazonServiceException.setStatusCode(403);

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles(any(ListInstanceProfilesRequest.class))).thenThrow(amazonServiceException);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("unauthorized.");

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), new HashMap<>());

        Assert.assertEquals(0L, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectAccessConfigsWhenUserGetAmazonExceptionToGetInfoThenItShouldReturnEmptyList() {
        AmazonServiceException amazonServiceException = new AmazonServiceException("Amazon problem.");
        amazonServiceException.setStatusCode(404);
        amazonServiceException.setErrorMessage("Amazon problem.");

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles(any(ListInstanceProfilesRequest.class))).thenThrow(amazonServiceException);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("Amazon problem.");

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Collections.emptyMap());

        Assert.assertEquals(0L, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectAccessConfigsWhenUserGetServiceExceptionToGetInfoThenItShouldReturnEmptyList() {
        BadRequestException badRequestException = new BadRequestException("BadRequestException problem.");

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles(any(ListInstanceProfilesRequest.class))).thenThrow(badRequestException);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("BadRequestException problem.");

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Collections.emptyMap());

        Assert.assertEquals(0L, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectAccessConfigsWhenWeGetBackInfoThenItShouldReturnListWithElements() {
        ListInstanceProfilesResult listInstanceProfilesResult = new ListInstanceProfilesResult();

        Set<InstanceProfile> instanceProfileSet = new HashSet<>();
        instanceProfileSet.add(instanceProfile(1));
        instanceProfileSet.add(instanceProfile(2));
        instanceProfileSet.add(instanceProfile(3));
        instanceProfileSet.add(instanceProfile(4));

        listInstanceProfilesResult.setInstanceProfiles(instanceProfileSet);
        listInstanceProfilesResult.setIsTruncated(false);

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles(any(ListInstanceProfilesRequest.class))).thenReturn(listInstanceProfilesResult);

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Collections.emptyMap());

        Assert.assertEquals(4L, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectEncryptionKeysWhenWeGetBackInfoThenItShouldReturnListWithElements() {
        ListKeysResult listKeysResult = new ListKeysResult();

        Set<KeyListEntry> listEntries = new HashSet<>();
        listEntries.add(keyListEntry(1));
        listEntries.add(keyListEntry(2));
        listEntries.add(keyListEntry(3));
        listEntries.add(keyListEntry(4));

        listKeysResult.setKeys(listEntries);

        DescribeKeyResult describeKeyResult = new DescribeKeyResult();
        describeKeyResult.setKeyMetadata(new KeyMetadata());

        ListAliasesResult describeAliasResult = new ListAliasesResult();

        Set<AliasListEntry> aliasListEntries = new HashSet<>();
        aliasListEntries.add(aliasListEntry(1));
        aliasListEntries.add(aliasListEntry(2));
        aliasListEntries.add(aliasListEntry(3));
        aliasListEntries.add(aliasListEntry(4));

        describeAliasResult.setAliases(aliasListEntries);

        when(awsClient.createAWSKMS(any(AwsCredentialView.class), anyString())).thenReturn(awskmsClient);
        when(awskmsClient.listKeys(any(ListKeysRequest.class))).thenReturn(listKeysResult);
        when(awskmsClient.describeKey(any(DescribeKeyRequest.class))).thenReturn(describeKeyResult);
        when(awskmsClient.listAliases(any(ListAliasesRequest.class))).thenReturn(describeAliasResult);

        CloudEncryptionKeys cloudEncryptionKeys =
                underTest.encryptionKeys(new CloudCredential("crn", "aws-credential"), region("London"), new HashMap<>());

        Assert.assertEquals(4L, cloudEncryptionKeys.getCloudEncryptionKeys().size());
    }

    @Test
    public void testVirtualMachinesDisabledTypesEmpty() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.emptyList());

        CloudVmTypes result = underTest.virtualMachines(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Collections.emptyMap());

        Assert.assertEquals("m5.2xlarge", result.getCloudVmResponses().get(AZ_NAME).iterator().next().value());
    }

    @Test
    public void testVirtualMachinesDisabledTypesContainsEmpty() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.singletonList(""));

        CloudVmTypes result = underTest.virtualMachines(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Collections.emptyMap());

        Assert.assertEquals("m5.2xlarge", result.getCloudVmResponses().get(AZ_NAME).iterator().next().value());
    }

    @Test
    public void testVirtualMachinesOkStartWith() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.singletonList("m5"));

        CloudVmTypes result = underTest.virtualMachines(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Collections.emptyMap());

        Assert.assertTrue(result.getCloudVmResponses().get(AZ_NAME).isEmpty());
    }

    @Test
    public void testVirtualMachinesOkFullMatch() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.singletonList("m5.2xlarge"));

        CloudVmTypes result = underTest.virtualMachines(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Collections.emptyMap());

        Assert.assertTrue(result.getCloudVmResponses().get(AZ_NAME).isEmpty());
    }

    private InstanceProfile instanceProfile(int i) {
        InstanceProfile instanceProfile = new InstanceProfile();
        instanceProfile.setArn(String.format("arn-%s", i));
        instanceProfile.setCreateDate(new Date());
        instanceProfile.setInstanceProfileId(String.format("profilId-%s", i));
        instanceProfile.setInstanceProfileName(String.format("profilName-%s", i));
        return instanceProfile;
    }

    private KeyListEntry keyListEntry(int i) {
        KeyListEntry keyListEntry = new KeyListEntry();
        keyListEntry.setKeyArn(String.format("key-%s", i));
        keyListEntry.setKeyId(String.format("%s", i));
        return keyListEntry;
    }

    private AliasListEntry aliasListEntry(int i) {
        AliasListEntry aliasListEntry = new AliasListEntry();
        aliasListEntry.setAliasArn(String.format("key-%s", i));
        aliasListEntry.setAliasName(String.format("%s", i));
        aliasListEntry.setTargetKeyId(String.format("%s", i));
        return aliasListEntry;
    }

    @Test
    public void noSqlTables() {
        when(awsClient.createDynamoDbClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonDynamoDB);
        when(amazonDynamoDB.listTables(any(ListTablesRequest.class))).thenReturn(
                new ListTablesResult().withTableNames("a", "b").withLastEvaluatedTableName("b"),
                new ListTablesResult().withTableNames("c", "d")
        );

        CloudNoSqlTables cloudNoSqlTables = underTest.noSqlTables(new CloudCredential(), region("region"), null);
        assertThat(cloudNoSqlTables.getCloudNoSqlTables()).hasSameElementsAs(List.of(
                new CloudNoSqlTable("a"),
                new CloudNoSqlTable("b"),
                new CloudNoSqlTable("c"),
                new CloudNoSqlTable("d")));
    }

    @Test
    public void networksSubnetsShouldBeFilteredByEnabledRegions() {
        DescribeRouteTablesResult routeTables = new DescribeRouteTablesResult();
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(routeTables);
        DescribeVpcsResult vpcs = new DescribeVpcsResult().withVpcs(new Vpc());
        when(amazonEC2Client.describeVpcs(any())).thenReturn(vpcs);
        DescribeSubnetsResult subnets = new DescribeSubnetsResult();
        String availabilityZone = "not-enabled-az";
        subnets.setSubnets(List.of(new Subnet().withAvailabilityZone(availabilityZone).withMapPublicIpOnLaunch(true)));
        when(amazonEC2Client.describeSubnets(any())).thenReturn(subnets);
        CloudNetworks cloudNetworks = underTest.networks(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Map.of());

        assertThat(cloudNetworks.getCloudNetworkResponses().get(REGION_NAME))
                .allMatch(cloudNetwork -> cloudNetwork.getSubnets().isEmpty());
    }

    @Test
    public void regionsShouldBeFilteredByEnabledRegionsAndEnabledAvailabilityZones() {
        setUpRegions();

        CloudRegions cloudRegions = underTest.regions(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Map.of(), true);
        assertThat(cloudRegions.getCloudRegions())
                .containsKey(region(REGION_NAME))
                .doesNotContainKey(region(NOT_ENABLED_REGION_NAME));
        assertThat(cloudRegions.getCloudRegions().get(region(REGION_NAME)))
                .contains(availabilityZone(AZ_NAME))
                .doesNotContain(availabilityZone(NOT_ENABLED_AZ_NAME));
    }

    private void setUpRegions() {
        DescribeRegionsResult regions = new DescribeRegionsResult().withRegions(
                new Region().withRegionName(NOT_ENABLED_REGION_NAME),
                new Region().withRegionName(REGION_NAME));
        when(amazonEC2Client.describeRegions(any())).thenReturn(regions);
        when(awsDefaultZoneProvider.getDefaultZone(any(CloudCredential.class))).thenReturn(REGION_NAME);
        when(awsAvailabilityZoneProvider.describeAvailabilityZones(any(), any(), any())).thenReturn(List.of(
                new AvailabilityZone().withZoneName(NOT_ENABLED_AZ_NAME),
                new AvailabilityZone().withZoneName(AZ_NAME)));
    }

    @Test
    public void virtualMachinesShouldBeFilteredByEnabledAvailabilityZones() {
        setUpRegions();
        instanceTypeInfos.add(getInstanceTypeInfo("vm1"));
        instanceTypeInfos.add(getInstanceTypeInfo("vm2"));
        describeInstanceTypesResult.setInstanceTypes(instanceTypeInfos);
        when(amazonEC2Client.describeInstanceTypes(any(DescribeInstanceTypesRequest.class)))
                .thenReturn(describeInstanceTypesResult);
        when(describeInstanceTypesResult.getInstanceTypes()).thenReturn(instanceTypeInfos);

        ReflectionTestUtils.setField(underTest, "defaultVmTypes", Map.of(
                region(REGION_NAME), vmType("vm1"),
                region(NOT_ENABLED_REGION_NAME), vmType("vm2")));
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", List.of());

        CloudVmTypes cloudVmTypes = underTest.virtualMachines(new CloudCredential("crn", "aws-credential"), region(REGION_NAME), Map.of());
        assertThat(cloudVmTypes.getCloudVmResponses())
                .containsKey(AZ_NAME)
                .doesNotContainKey(NOT_ENABLED_AZ_NAME);
        assertThat(cloudVmTypes.getDefaultCloudVmResponses())
                .containsKey(AZ_NAME)
                .doesNotContainKey(NOT_ENABLED_AZ_NAME);
    }

    @Test
    public void testEncryptionWhenNoEbsInfoShouldReturnFalse() {
        InstanceTypeInfo instanceTypeInfo = new InstanceTypeInfo();
        boolean encryptionSupported = underTest.getEncryptionSupported(instanceTypeInfo);
        Assert.assertFalse(encryptionSupported);
    }

    @Test
    public void testEncryptionWhenEncryptionSupportedNotPresentedShouldReturnFalse() {
        InstanceTypeInfo instanceTypeInfo = new InstanceTypeInfo();
        EbsInfo ebsInfo = new EbsInfo();
        instanceTypeInfo.setEbsInfo(ebsInfo);
        boolean encryptionSupported = underTest.getEncryptionSupported(instanceTypeInfo);
        Assert.assertFalse(encryptionSupported);
    }

    @Test
    public void testEncryptionWhenEncryptionSupportedWithSupportedValuePresentedShouldReturnTrue() {
        InstanceTypeInfo instanceTypeInfo = new InstanceTypeInfo();
        EbsInfo ebsInfo = new EbsInfo();
        ebsInfo.setEncryptionSupport("supported");
        instanceTypeInfo.setEbsInfo(ebsInfo);
        boolean encryptionSupported = underTest.getEncryptionSupported(instanceTypeInfo);
        Assert.assertTrue(encryptionSupported);
    }

    @Test
    public void testEncryptionWhenEncryptionSupportedWithNotSupportedValuePresentedShouldReturnFalse() {
        InstanceTypeInfo instanceTypeInfo = new InstanceTypeInfo();
        EbsInfo ebsInfo = new EbsInfo();
        ebsInfo.setEncryptionSupport("nonsupported");
        instanceTypeInfo.setEbsInfo(ebsInfo);
        boolean encryptionSupported = underTest.getEncryptionSupported(instanceTypeInfo);
        Assert.assertFalse(encryptionSupported);
    }
}
