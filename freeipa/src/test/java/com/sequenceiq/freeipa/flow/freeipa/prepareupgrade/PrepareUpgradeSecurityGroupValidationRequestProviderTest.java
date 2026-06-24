package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
class PrepareUpgradeSecurityGroupValidationRequestProviderTest {

    private static final long STACK_ID = 7L;

    private static final String ENV_CRN = "crn:altus:environments:us-west-1:tenant:environment:freeipa-env";

    private static final String VPC_ID = "vpc-freeipa";

    @Mock
    private ResourceService resourceService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private Stack stack;

    @InjectMocks
    private PrepareUpgradeSecurityGroupValidationRequestProvider underTest;

    @Test
    void collectSecurityGroupIdsMergesInstanceGroupAndPersistedResources() {
        InstanceGroup instanceGroup = new InstanceGroup();
        SecurityGroup securityGroup = new SecurityGroup();
        securityGroup.setSecurityGroupIds(Set.of("sg-metadata"));
        instanceGroup.setSecurityGroup(securityGroup);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup));
        Resource persistedResource = new Resource();
        persistedResource.setResourceReference("sg-resource");
        when(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED, ResourceType.AWS_SECURITY_GROUP, STACK_ID))
                .thenReturn(List.of(persistedResource));

        Set<String> result = underTest.collectSecurityGroupIds(stack);

        assertThat(result).containsExactlyInAnyOrder("sg-metadata", "sg-resource");
    }

    @Test
    void resolveAwsVpcIdReturnsVpcFromEnvironment() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        EnvironmentNetworkAwsParams aws = new EnvironmentNetworkAwsParams();
        aws.setVpcId(VPC_ID);
        network.setAws(aws);
        environment.setNetwork(network);
        when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(environment);

        assertThat(underTest.resolveAwsVpcId(ENV_CRN)).isEqualTo(VPC_ID);
    }

    @Test
    void resolveAwsVpcIdReturnsNullWhenEnvironmentHasNoAwsNetwork() {
        when(cachedEnvironmentClientService.getByCrn(ENV_CRN)).thenReturn(new DetailedEnvironmentResponse());

        assertThat(underTest.resolveAwsVpcId(ENV_CRN)).isNull();
    }
}
