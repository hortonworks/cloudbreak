package com.sequenceiq.environment.environment.validation.securitygroup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
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
import com.sequenceiq.environment.environment.validation.securitygroup.azure.AzureEnvironmentSecurityGroupValidator;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@ExtendWith(MockitoExtension.class)
public class AzureEnvironmentSecurityGroupValidatorTest {

    private static final String SECURITY_GROUP_1 = "/subscriptions/a9d4456e-349f-44f6-bc73-54a8d523e504/resourceGroups/mock/providers/" +
            "Microsoft.Network/networkSecurityGroups/sec-1";

    private static final String SECURITY_GROUP_2 = "/subscriptions/a9d4456e-349f-44f6-bc73-54a8d523e504/resourceGroups/mock/providers/" +
            "Microsoft.Network/networkSecurityGroups/sec-2";

    private static final String REGION = "West US";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PlatformParameterService platformParameterService;

    @InjectMocks
    private AzureEnvironmentSecurityGroupValidator underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new AzureEnvironmentSecurityGroupValidator(platformParameterService);
    }

    @Test
    public void testValidationWhenGroupsInTheSameRegionReturnValid() {
        Region region = getRegion();
        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(REGION, SECURITY_GROUP_1, SECURITY_GROUP_2));

        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withRegions(Set.of(region))
                .withSecurityAccess(getSecurityAccessDto(SECURITY_GROUP_1, SECURITY_GROUP_2))
                .withCredential(getCredential())
                .build();

        ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsValid(builder);
    }

    @Test
    public void testValidationWhenOnlyOneGroupDefinedReturnInvalid() {
        Region region = getRegion();
        String sec1 = null;

        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups(REGION, sec1, SECURITY_GROUP_2));

        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withRegions(Set.of(region))
                .withSecurityAccess(getSecurityAccessDto(sec1, SECURITY_GROUP_2))
                .withCredential(getCredential())
                .build();

        ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsInvalid(builder);
    }

    @Test
    public void testValidationWhenGroupsInDifferentRegionReturnInvalid() {
        Region region = getRegion();
        CloudSecurityGroups cloudSecurityGroups = new CloudSecurityGroups();

        when(platformParameterService.getSecurityGroups(any(PlatformResourceRequest.class)))
                .thenReturn(cloudSecurityGroups);

        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withRegions(Set.of(region))
                .withSecurityAccess(getSecurityAccessDto(SECURITY_GROUP_1, SECURITY_GROUP_2))
                .withCredential(getCredential())
                .build();

        ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsInvalid(builder);
    }

    @Test
    public void testValidationWhenNewGroupsRequestedAndUserWantNewNetworkReturnValid() {
        Region region = getRegion();

        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withRegions(Set.of(region))
                .withSecurityAccess(getNewSecurityAccessDto())
                .withNetwork(getNewNetworkDto(false))
                .withCredential(getCredential())
                .build();

        ValidationResultBuilder builder = ValidationResult.builder();
        underTest.validate(environmentDto, builder);
        requestIsValid(builder);
    }

    private void requestIsValid(ValidationResultBuilder builder) {
        assertFalse(builder.build().hasError());
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

    private NetworkDto getNewNetworkDto(boolean noPublicIp) {
        return NetworkDto.builder()
                .withNetworkCidr("0.0.0.0/0")
                .withAzure(getAzureParams(noPublicIp))
                .build();
    }

    private AzureParams getAzureParams(boolean noPublicIp) {
        return AzureParams.builder()
                .withNetworkId("aNetworkId")
                .withResourceGroupName("aResourceGroupId")
                .withNoPublicIp(noPublicIp)
                .build();
    }

    private Region getRegion() {
        Region region = new Region();
        region.setName(REGION);
        return region;
    }

    private Credential getCredential() {
        Credential credential = new Credential();
        credential.setName("azure-credential");
        return credential;
    }

    public CloudSecurityGroups cloudSecurityGroups(String region, String... securityGroupIds) {
        CloudSecurityGroups cloudSecurityGroups = new CloudSecurityGroups();
        cloudSecurityGroups.getCloudSecurityGroupsResponses().put(region, new HashSet<>());

        for (String securityGroupId : securityGroupIds) {
            CloudSecurityGroup cloudSecurityGroup = new CloudSecurityGroup(
                    securityGroupId + "x",
                    securityGroupId,
                    new HashMap<>()
            );
            cloudSecurityGroups.getCloudSecurityGroupsResponses().get(region).add(cloudSecurityGroup);
        }
        return cloudSecurityGroups;
    }

}
