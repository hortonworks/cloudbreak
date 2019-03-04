package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.AliasListEntry;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.KeyListEntry;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.ListAliasesRequest;
import com.amazonaws.services.kms.model.ListAliasesResult;
import com.amazonaws.services.kms.model.ListKeysRequest;
import com.amazonaws.services.kms.model.ListKeysResult;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@RunWith(MockitoJUnitRunner.class)
public class AwsPlatformResourcesTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private AwsPlatformResources underTest;

    @Mock
    private AwsClient awsClient;

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Mock
    private AwsPlatformParameters awsPlatformParameters;

    @Mock
    private AmazonIdentityManagement amazonCFClient;

    @Mock
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Mock
    private AWSKMSClient awskmsClient;

    @Mock
    private AmazonEC2Client amazonEC2Client;

    @Mock
    private DescribeRegionsResult describeRegionsResult;

    @Mock
    private DescribeAvailabilityZonesResult describeAvailabilityZonesResult;

    @Mock
    private Region region;

    @Mock
    private AvailabilityZone availabilityZone;

    @Before
    public void setUp() {
        Mockito.reset(awsClient);

        when(awsDefaultZoneProvider.getDefaultZone(any(CloudCredential.class))).thenReturn("eu-central-1");
        when(awsClient.createAccess(any(CloudCredential.class))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeRegions(any(DescribeRegionsRequest.class))).thenReturn(describeRegionsResult);
        when(amazonEC2Client.describeAvailabilityZones(any(DescribeAvailabilityZonesRequest.class))).thenReturn(describeAvailabilityZonesResult);
        when(describeRegionsResult.getRegions()).thenReturn(Collections.singletonList(region));
        when(describeAvailabilityZonesResult.getAvailabilityZones()).thenReturn(Collections.singletonList(availabilityZone));
        when(availabilityZone.getZoneName()).thenReturn("eu-central-1a");
        when(region.getRegionName()).thenReturn("eu-central-1");

        ReflectionTestUtils.setField(underTest, "vmTypes",
                Collections.singletonMap(region("eu-central-1"), Collections.singleton(VmType.vmType("m5.2xlarge"))));
    }

    @Test
    public void collectAccessConfigsWhenUserIsUnathorizedToGetInfoThenItShouldReturnEmptyList() {
        AmazonServiceException amazonServiceException = new AmazonServiceException("unauthorized.");
        amazonServiceException.setStatusCode(403);

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles()).thenThrow(amazonServiceException);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("Could not get instance profile roles because the user does not have enough permission.");

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential(1L, "aws-credential"), region("eu-central-1"), new HashMap<>());

        Assert.assertEquals(0L, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectAccessConfigsWhenUserGetAmazonExceptionToGetInfoThenItShouldReturnEmptyList() {
        AmazonServiceException amazonServiceException = new AmazonServiceException("Amazon problem.");
        amazonServiceException.setStatusCode(404);
        amazonServiceException.setErrorMessage("Amazon problem.");

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles()).thenThrow(amazonServiceException);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("Could not get instance profile roles from Amazon: Amazon problem.");

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential(1L, "aws-credential"), region("eu-central-1"), Collections.emptyMap());

        Assert.assertEquals(0L, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectAccessConfigsWhenUserGetServiceExceptionToGetInfoThenItShouldReturnEmptyList() {
        BadRequestException badRequestException = new BadRequestException("BadRequestException problem.");

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles()).thenThrow(badRequestException);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("Could not get instance profile roles from Amazon: BadRequestException problem.");

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential(1L, "aws-credential"), region("eu-central-1"), Collections.emptyMap());

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

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles()).thenReturn(listInstanceProfilesResult);

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential(1L, "aws-credential"), region("eu-central-1"), Collections.emptyMap());

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
                underTest.encryptionKeys(new CloudCredential(1L, "aws-credential"), region("London"), new HashMap<>());

        Assert.assertEquals(4L, cloudEncryptionKeys.getCloudEncryptionKeys().size());
    }

    @Test
    public void testVirtualMachinesDisabledTypesEmpty() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.emptyList());

        CloudVmTypes result = underTest.virtualMachines(new CloudCredential(1L, "aws-credential"), region("eu-central-1"), Collections.emptyMap());

        Assert.assertEquals("m5.2xlarge", result.getCloudVmResponses().get("eu-central-1a").iterator().next().value());
    }

    @Test
    public void testVirtualMachinesDisabledTypesContainsEmpty() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.singletonList(""));

        CloudVmTypes result = underTest.virtualMachines(new CloudCredential(1L, "aws-credential"), region("eu-central-1"), Collections.emptyMap());

        Assert.assertEquals("m5.2xlarge", result.getCloudVmResponses().get("eu-central-1a").iterator().next().value());
    }

    @Test
    public void testVirtualMachinesOkStartWith() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.singletonList("m5"));

        CloudVmTypes result = underTest.virtualMachines(new CloudCredential(1L, "aws-credential"), region("eu-central-1"), Collections.emptyMap());

        Assert.assertTrue(result.getCloudVmResponses().get("eu-central-1a").isEmpty());
    }

    @Test
    public void testVirtualMachinesOkFullMatch() {
        ReflectionTestUtils.setField(underTest, "disabledInstanceTypes", Collections.singletonList("m5.2xlarge"));

        CloudVmTypes result = underTest.virtualMachines(new CloudCredential(1L, "aws-credential"), region("eu-central-1"), Collections.emptyMap());

        Assert.assertTrue(result.getCloudVmResponses().get("eu-central-1a").isEmpty());
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
}