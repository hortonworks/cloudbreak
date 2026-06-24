package com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class AwsSecurityGroupValidationRequestProviderTest {

    private static final long STACK_ID = 42L;

    private static final String ENV_CRN = "crn:altus:environments:us-west-1:tenant:environment:my-env";

    private static final String VPC_ID = "vpc-123";

    @Mock
    private ResourceService resourceService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private StackDto stack;

    @InjectMocks
    private AwsSecurityGroupValidationRequestProvider underTest;

    @Test
    void collectSecurityGroupIdsMergesInstanceGroupAndPersistedResources() {
        InstanceGroup instanceGroup = new InstanceGroup();
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setSecurityGroupIds(Set.of("sg-from-metadata"));
        instanceGroup.setSecurityGroup(securityGroup);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(instanceGroup, List.of())));
        Resource persistedResource = new Resource();
        persistedResource.setResourceReference("sg-from-resource");
        when(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED, ResourceType.AWS_SECURITY_GROUP, STACK_ID))
                .thenReturn(List.of(persistedResource));

        Set<String> result = underTest.collectSecurityGroupIds(stack);

        assertThat(result).containsExactlyInAnyOrder("sg-from-metadata", "sg-from-resource");
    }

    @Test
    void collectSecurityGroupIdsIgnoresBlankResourceReferences() {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getInstanceGroupDtos()).thenReturn(List.of());
        Resource blankReference = new Resource();
        blankReference.setResourceReference("  ");
        when(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED, ResourceType.AWS_SECURITY_GROUP, STACK_ID))
                .thenReturn(List.of(blankReference));

        assertThat(underTest.collectSecurityGroupIds(stack)).isEmpty();
    }

    @Test
    void resolveAwsVpcIdReturnsVpcFromEnvironment() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        EnvironmentNetworkAwsParams aws = new EnvironmentNetworkAwsParams();
        aws.setVpcId(VPC_ID);
        network.setAws(aws);
        environment.setNetwork(network);
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(environment);

        assertThat(underTest.resolveAwsVpcId(ENV_CRN)).isEqualTo(VPC_ID);
    }

    @Test
    void resolveAwsVpcIdReturnsNullWhenEnvironmentHasNoAwsNetwork() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        when(environmentService.getByCrn(ENV_CRN)).thenReturn(environment);

        assertThat(underTest.resolveAwsVpcId(ENV_CRN)).isNull();
    }
}
