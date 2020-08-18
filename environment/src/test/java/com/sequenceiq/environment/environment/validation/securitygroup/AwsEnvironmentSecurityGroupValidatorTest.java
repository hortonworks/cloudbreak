package com.sequenceiq.environment.environment.validation.securitygroup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.environment.validation.securitygroup.aws.AwsEnvironmentSecurityGroupValidator;
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
        Region region = getRegion();
        String sec1 = "sec-1";
        String sec2 = "sec-2";
        String vpcId = "vpc-123";

        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(region.getName(), vpcId, sec1, sec2));

        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withRegions(Set.of(region))
                .withSecurityAccess(getSecurityAccessDto(sec1, sec2))
                .withNetwork(getNetworkDto(vpcId))
                .withCredential(getCredential())
                .build();

        ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsValid(builder);
    }

    @Test
    public void testValidationWhenOnlyOneGroupDefinedReturnInValid() {
        Region region = getRegion();
        String sec1 = null;
        String sec2 = "sec-2";
        String vpcId = "vpc-123";

        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(region.getName(), vpcId, sec1, sec2));

        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withRegions(Set.of(region))
                .withSecurityAccess(getSecurityAccessDto(sec1, sec2))
                .withNetwork(getNetworkDto(vpcId))
                .withCredential(getCredential())
                .build();

        ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsInvalid(builder);
    }

    @Test
    public void testValidationWhenGroupsInDifferentVpcReturnInValid() {
        Region region = getRegion();
        String sec1 = "sec-1";
        String sec2 = "sec-2";
        String vpcId = "vpc-123";
        String requestVpcId = "vpc-124";
        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(region.getName(), vpcId, sec1, sec2));
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withRegions(Set.of(region))
                .withSecurityAccess(getSecurityAccessDto(sec1, sec2))
                .withNetwork(getNetworkDto(requestVpcId))
                .withCredential(getCredential())
                .build();
        ValidationResultBuilder builder = ValidationResult.builder();

        underTest.validate(environmentDto, builder);

        requestIsInvalid(builder);
    }

    @Test
    public void testValidationWhenGroupsDefinedButUserWantNewVpcReturnInValid() {
        Region region = getRegion();
        String sec1 = "sec-1";
        String sec2 = "sec-2";
        String vpcId = "vpc-123";

        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(region.getName(), vpcId, sec1, sec2));

        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withRegions(Set.of(region))
                .withSecurityAccess(getSecurityAccessDto(sec1, sec2))
                .withNetwork(getNewNetworkDto())
                .withCredential(getCredential())
                .build();

        ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsInvalid(builder);
    }

    @Test
    public void testValidationWhenNewGroupsRequestedAndUserWantNewVpcReturnValid() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withRegions(Set.of(getRegion()))
                .withSecurityAccess(getNewSecurityAccessDto())
                .withNetwork(getNewNetworkDto())
                .withCredential(getCredential())
                .build();

        ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsValid(builder);
    }

    private void requestIsValid(ValidationResultBuilder builder) {
        assertFalse(builder.build().hasError());
    }

    private Region getRegion() {
        String regionName = "eu-west-1";
        Region region = new Region();
        region.setName(regionName);
        return region;
    }

    private void requestIsInvalid(ValidationResultBuilder builder) {
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

        return NetworkDto.builder()
                .withAws(awsParams)
                .build();
    }

    private NetworkDto getNewNetworkDto() {
        return NetworkDto.builder()
                .withNetworkCidr("0.0.0.0/0")
                .withRegistrationType(RegistrationType.CREATE_NEW)
                .build();
    }

    private Credential getCredential() {
        Credential credential = new Credential();
        credential.setName("apple");
        return credential;
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
