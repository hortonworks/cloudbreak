package com.sequenceiq.environment.environment.validation.securitygroup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@ExtendWith(MockitoExtension.class)
public class AwsEnvironmentSecurityGroupValidatorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PlatformParameterService platformParameterService;

    @InjectMocks
    private AwsEnvironmentSecurityGroupValidator underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new AwsEnvironmentSecurityGroupValidator(platformParameterService);
    }

    @Test
    public void testValidationWhenGroupsInTheSameVpcReturnValid() {
        String region = "eu-west-1";
        String sec1 = "sec-1";
        String sec2 = "sec-2";
        String vpcId = "vpc-123";

        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(region, vpcId, sec1, sec2));

        EnvironmentCreationDto environmentDto = EnvironmentCreationDto.Builder
                .anEnvironmentCreationDto()
                .withRegions(Sets.newHashSet(region))
                .withSecurityAccess(getSecurityAccessDto(sec1, sec2))
                .withNetwork(getNetworkDto(vpcId))
                .withCredential(getEnvironmentRequest())
                .build();

        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsValid(builder);
    }

    @Test
    public void testValidationWhenOnlyOneGroupDefinedReturnInValid() {
        String region = "eu-west-1";
        String sec1 = null;
        String sec2 = "sec-2";
        String vpcId = "vpc-123";

        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(region, vpcId, sec1, sec2));

        EnvironmentCreationDto environmentDto = EnvironmentCreationDto.Builder
                .anEnvironmentCreationDto()
                .withRegions(Sets.newHashSet(region))
                .withSecurityAccess(getSecurityAccessDto(sec1, sec2))
                .withNetwork(getNetworkDto(vpcId))
                .withCredential(getEnvironmentRequest())
                .build();

        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsInValid(builder);
    }

    @Test
    public void testValidationWhenGroupsInDifferentVpcReturnInValid() {
        String region = "eu-west-1";
        String sec1 = "sec-1";
        String sec2 = "sec-2";
        String vpcId = "vpc-123";
        String requestVpcId = "vpc-124";

        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(region, vpcId, sec1, sec2));

        EnvironmentCreationDto environmentDto = EnvironmentCreationDto.Builder
                .anEnvironmentCreationDto()
                .withRegions(Sets.newHashSet(region))
                .withSecurityAccess(getSecurityAccessDto(sec1, sec2))
                .withNetwork(getNetworkDto(requestVpcId))
                .withCredential(getEnvironmentRequest())
                .build();

        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsInValid(builder);
    }

    @Test
    public void testValidationWhenGroupsDefinedButUserWantNewVpcReturnInValid() {
        String region = "eu-west-1";
        String sec1 = "sec-1";
        String sec2 = "sec-2";
        String vpcId = "vpc-123";

        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(region, vpcId, sec1, sec2));

        EnvironmentCreationDto environmentDto = EnvironmentCreationDto.Builder
                .anEnvironmentCreationDto()
                .withRegions(Sets.newHashSet(region))
                .withSecurityAccess(getSecurityAccessDto(sec1, sec2))
                .withNetwork(getNewNetworkDto())
                .withCredential(getEnvironmentRequest())
                .build();

        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsInValid(builder);
    }

    @Test
    public void testValidationWhenNewGroupsRequestedAndUserWantNewVpcReturnValid() {
        String region = "eu-west-1";

        EnvironmentCreationDto environmentDto = EnvironmentCreationDto.Builder
                .anEnvironmentCreationDto()
                .withRegions(Sets.newHashSet(region))
                .withSecurityAccess(getNewSecurityAccessDto())
                .withNetwork(getNewNetworkDto())
                .withCredential(getEnvironmentRequest())
                .build();

        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsValid(builder);
    }

    private void requestIsValid(ValidationResult.ValidationResultBuilder builder) {
        assertFalse(builder.build().hasError());
    }

    private void requestIsInValid(ValidationResult.ValidationResultBuilder builder) {
        assertTrue(builder.build().hasError());
    }

    private SecurityAccessDto getSecurityAccessDto(String sec1, String sec2) {
        return SecurityAccessDto.builder()
                    .withDefaultSecurityGroupId(sec1)
                    .withSecurityGroupIdForKnox(sec2)
                    .build();
    }

    private SecurityAccessDto getNewSecurityAccessDto() {
        return SecurityAccessDto.builder()
                .withCidr("0.0.0.0/0")
                .build();
    }

    private NetworkDto getNetworkDto(String vpcId) {
        AwsParams awsParams = new AwsParams();
        awsParams.setVpcId(vpcId);

        return NetworkDto.Builder.aNetworkDto()
                .withAws(awsParams)
                .build();
    }

    private NetworkDto getNewNetworkDto() {
        return NetworkDto.Builder.aNetworkDto()
                .withNetworkCidr("0.0.0.0/0")
                .build();
    }

    private EnvironmentRequest getEnvironmentRequest() {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setCredentialName("apple");
        return environmentRequest;
    }

    public CloudSecurityGroups cloudSecurityGroups(String region, String vpcId, String... securityGroupIds) {
        CloudSecurityGroups cloudSecurityGroups = new CloudSecurityGroups();
        cloudSecurityGroups.getCloudSecurityGroupsResponses().put(region, new HashSet<>());

        for (String securityGroupId : securityGroupIds) {
            CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(
                    securityGroupId + "x",
                    securityGroupId,
                    Map.of("vpcId", vpcId)
            );
            cloudSecurityGroups.getCloudSecurityGroupsResponses().get(region).add(cloudSecurityGroup);
        }
        return cloudSecurityGroups;
    }

}
