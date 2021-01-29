package com.sequenceiq.datalake.service.validation.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.PerformanceMode;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.ThroughputMode;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.AwsEfsParameters;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.datalake.service.validation.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class CloudStorageValidatorTest {

    // this value should be equal to or larger than 2 to test certain cases and avoid the index out of boundary
    private static final int GENERAL_TEST_QUANTITY = 2;

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static String userCrn;

    private static String[] groupNames;

    private List<InstanceGroupV4Request> instanceGroups;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private SecretService secretService;

    @Mock
    private StackV4Request stackV4Request;

    @Mock
    private ClusterV4Request clusterV4Request;

    @Mock
    private CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @InjectMocks
    private CloudStorageValidator underTest;

    @InjectMocks
    private CloudStorageRequest cloudStorageRequest;

    @BeforeAll
    public static void setUp() {
        userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (Strings.isNullOrEmpty(userCrn)) {
            ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
            userCrn = USER_CRN;
        }

        groupNames = new String[GENERAL_TEST_QUANTITY];
        for (int i = 0; i < GENERAL_TEST_QUANTITY; i++) {
            groupNames[i] = String.format("group-%d", i);
        }
        groupNames[0] = "master";
        groupNames[1] = "core";
    }

    @BeforeEach
    public void setUpTest() {
        instanceGroups = createInstanceGroups();
        cloudStorageRequest = new CloudStorageRequest();
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(clusterV4Request.getCloudStorage()).thenReturn(cloudStorageRequest);
    }

    @Test
    public void validateEnvironmentRequestCloudStorageValidationDisabled() {
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.DISABLED);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(stackV4Request, environment, validationResultBuilder);
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEnvironmentRequestCloudStorageValidationNoEntitlement() {
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.ENABLED);
        when(entitlementService.cloudStorageValidationEnabled(any())).thenReturn(false);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(stackV4Request, environment, validationResultBuilder);
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEnvironmentRequestCloudStorageValidationMissingEntitlement() {
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.ENABLED);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(stackV4Request, environment, validationResultBuilder);
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEfsAssociatedSecurityGroupIdCountValid() {
        //GIVEN
        setupEfsTest();

        //WHEN
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        Set<String> securityGroupIds = new HashSet<>(Arrays.asList("sg-1", "sg-2", "sg-3", "sg-4", "sg-5"));
        SecurityGroupV4Request securityGroupV4Request = new SecurityGroupV4Request();
        securityGroupV4Request.setSecurityGroupIds(securityGroupIds);

        for (InstanceGroupV4Request instanceGroupV4Request : instanceGroups) {
            instanceGroupV4Request.setSecurityGroup(securityGroupV4Request);
        }

        AwsStorageParameters awsStorageParameters = new AwsStorageParameters();
        awsStorageParameters.setEfsParameters(createEfsParameters());
        cloudStorageRequest.setAws(awsStorageParameters);

        //THEN
        underTest.validate(stackV4Request, environment, validationResultBuilder);
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEfsAssociatedSecurityGroupRuleCountValid() {
        //GIVEN
        setupEfsTest();

        //WHEN
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        SecurityRuleV4Request securityRule = new SecurityRuleV4Request();
        List<SecurityRuleV4Request> securityRules = List.of(securityRule);
        SecurityGroupV4Request securityGroupV4Request = new SecurityGroupV4Request();
        securityGroupV4Request.setSecurityRules(securityRules);

        for (InstanceGroupV4Request instanceGroupV4Request : instanceGroups) {
            instanceGroupV4Request.setSecurityGroup(securityGroupV4Request);
        }

        AwsStorageParameters awsStorageParameters = new AwsStorageParameters();
        awsStorageParameters.setEfsParameters(createEfsParameters());
        cloudStorageRequest.setAws(awsStorageParameters);

        //THEN
        underTest.validate(stackV4Request, environment, validationResultBuilder);
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEfsAssociatedSecurityGroupCountZeroInvalid() {
        //GIVEN
        setupEfsTest();

        //WHEN
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        Set<String> securityGroupIds = new HashSet<>();
        SecurityGroupV4Request securityGroupV4Request = new SecurityGroupV4Request();
        securityGroupV4Request.setSecurityGroupIds(securityGroupIds);

        for (InstanceGroupV4Request instanceGroupV4Request : instanceGroups) {
            instanceGroupV4Request.setSecurityGroup(securityGroupV4Request);
        }

        AwsStorageParameters awsStorageParameters = new AwsStorageParameters();
        awsStorageParameters.setEfsParameters(createEfsParameters());
        cloudStorageRequest.setAws(awsStorageParameters);

        //THEN
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validate(stackV4Request, environment, validationResultBuilder);
                });
    }

    @Test
    public void validateEfsAssociatedSecurityGroupCountSixInvalid() {
        //GIVEN
        setupEfsTest();

        //WHEN
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        Set<String> securityGroupIds = new HashSet<>(Arrays.asList("sg-1", "sg-2", "sg-3", "sg-4", "sg-5", "sg-6"));
        SecurityGroupV4Request securityGroupV4Request = new SecurityGroupV4Request();
        securityGroupV4Request.setSecurityGroupIds(securityGroupIds);

        for (InstanceGroupV4Request instanceGroupV4Request : instanceGroups) {
            instanceGroupV4Request.setSecurityGroup(securityGroupV4Request);
        }

        AwsStorageParameters awsStorageParameters = new AwsStorageParameters();
        awsStorageParameters.setEfsParameters(createEfsParameters());
        cloudStorageRequest.setAws(awsStorageParameters);

        //THEN
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validate(stackV4Request, environment, validationResultBuilder);
        });
    }

    private List<InstanceGroupV4Request> createInstanceGroups() {
        List<InstanceGroupV4Request> instanceGroups = new ArrayList<>(GENERAL_TEST_QUANTITY);
        for (int i = 0; i < GENERAL_TEST_QUANTITY; i++) {
            InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
            InstanceTemplateV4Request template = new InstanceTemplateV4Request();
            template.setAttachedVolumes(Collections.EMPTY_SET);
            instanceGroup.setName(groupNames[i]);
            instanceGroup.setTemplate(template);
            instanceGroup.setType(InstanceGroupType.CORE);
            instanceGroup.setNodeCount(i);
            instanceGroups.add(instanceGroup);
        }
        return instanceGroups;
    }

    private AwsEfsParameters createEfsParameters() {
        String fileSystemName = "efs-name-1";
        Map<String, String> tags = new HashMap<>();
        tags.put(CloudEfsConfiguration.KEY_FILESYSTEM_TAGS_NAME, fileSystemName);
        AwsEfsParameters efsParameters = new AwsEfsParameters();
        efsParameters.setName(fileSystemName);
        efsParameters.setEncrypted(true);
        efsParameters.setFileSystemTags(tags);
        efsParameters.setPerformanceMode(PerformanceMode.GENERALPURPOSE.toString());
        efsParameters.setThroughputMode(ThroughputMode.BURSTING.toString());
        efsParameters.setAssociatedInstanceGroupNames(Arrays.asList(groupNames));

        return efsParameters;
    }

    private CredentialResponse createTestCredential() {
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setName("cred");
        credentialResponse.setCloudPlatform("aws");
        return credentialResponse;
    }

    private void setupEfsTest() {
        CredentialResponse credentialResponse = createTestCredential();
        ObjectStorageValidateResponse objectStorageValidateResponse = new ObjectStorageValidateResponse();
        objectStorageValidateResponse.setStatus(ResponseStatus.OK);
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.ENABLED);
        when(environment.getCredential()).thenReturn(new CredentialResponse());
        when(entitlementService.cloudStorageValidationEnabled(any())).thenReturn(true);
        when(environment.getCredential()).thenReturn(credentialResponse);
        when(secretService.getByResponse(any())).thenReturn("testSecret");
        when(stackV4Request.getInstanceGroups()).thenReturn(instanceGroups);
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(new CloudCredential());
        when(cloudProviderServicesV4Endpoint.validateObjectStorage(any())).thenReturn(objectStorageValidateResponse);
    }
}
