package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.internal.SdkInternalList;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
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

    @Before
    public void setUp() {
        Mockito.reset(awsClient);
    }

    @Test
    public void collectAccessConfigsWhenUserIsUnathorizedToGetInfoThenItShouldReturnEmptyList() throws Exception {
        AmazonServiceException amazonServiceException = new AmazonServiceException("unauthorized.");
        amazonServiceException.setStatusCode(403);

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles()).thenThrow(amazonServiceException);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("Could not get instance profile roles because the user does not have enough permission.");

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential(1L, "aws-credential"), region("London"), new HashMap<>());

        Assert.assertEquals(0, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectAccessConfigsWhenUserGetAmazonExceptionToGetInfoThenItShouldReturnEmptyList() throws Exception {
        AmazonServiceException amazonServiceException = new AmazonServiceException("Amazon problem.");
        amazonServiceException.setStatusCode(404);
        amazonServiceException.setErrorMessage("Amazon problem.");

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles()).thenThrow(amazonServiceException);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("Could not get instance profile roles from Amazon: Amazon problem.");

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential(1L, "aws-credential"), region("London"), new HashMap<>());

        Assert.assertEquals(0, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectAccessConfigsWhenUserGetServiceExceptionToGetInfoThenItShouldReturnEmptyList() throws Exception {
        BadRequestException badRequestException = new BadRequestException("BadRequestException problem.");

        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonCFClient);
        when(amazonCFClient.listInstanceProfiles()).thenThrow(badRequestException);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("Could not get instance profile roles from Amazon: BadRequestException problem.");

        CloudAccessConfigs cloudAccessConfigs =
                underTest.accessConfigs(new CloudCredential(1L, "aws-credential"), region("London"), new HashMap<>());

        Assert.assertEquals(0, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    @Test
    public void collectAccessConfigsWhenWeGetBackInfoThenItShouldReturnListWithElements() throws Exception {
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
                underTest.accessConfigs(new CloudCredential(1L, "aws-credential"), region("London"), new HashMap<>());

        Assert.assertEquals(4, cloudAccessConfigs.getCloudAccessConfigs().size());
    }

    private InstanceProfile instanceProfile(int i) {
        InstanceProfile instanceProfile = new InstanceProfile();
        instanceProfile.setArn(String.format("arn-%s", i));
        instanceProfile.setCreateDate(new Date());
        instanceProfile.setInstanceProfileId(String.format("profilId-%s", i));
        instanceProfile.setInstanceProfileName(String.format("profilName-%s", i));
        SdkInternalList<Role> roles = new SdkInternalList();
        Role role = new Role();
        role.setRoleName(String.format("roleArn-%s", i));
        roles.add(role);
        return instanceProfile;
    }
}