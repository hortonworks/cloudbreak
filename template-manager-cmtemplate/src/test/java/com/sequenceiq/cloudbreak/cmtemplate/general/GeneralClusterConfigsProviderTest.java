package com.sequenceiq.cloudbreak.cmtemplate.general;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.workspace.model.User;

@ExtendWith(MockitoExtension.class)
class GeneralClusterConfigsProviderTest {

    public static final String CLUSTER_MANAGER_VARIANT = "clusterManagerVariant";

    @InjectMocks
    private GeneralClusterConfigsProvider underTest;

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private ClusterView cluster;

    @Mock
    private InstanceMetadataView primaryGatewayInstance;

    @Mock
    private SecurityConfig securityConfig;

    @Test
    void generalClusterConfigsTestVariantWhenStackDtoDelegate() {
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getPrimaryGatewayInstance()).thenReturn(primaryGatewayInstance);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(stack.getCreator()).thenReturn(new User());

        when(cluster.getVariant()).thenReturn(CLUSTER_MANAGER_VARIANT);

        GeneralClusterConfigs generalClusterConfigs = underTest.generalClusterConfigs(stack, Credential.builder().build());

        assertThat(generalClusterConfigs.getVariant()).isEqualTo(CLUSTER_MANAGER_VARIANT);
    }

    @Test
    void generalClusterConfigsTestVariantWhenStackV4Request() {
        StackV4Request stackRequest = new StackV4Request();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        stackRequest.setCluster(clusterRequest);

        GeneralClusterConfigs generalClusterConfigs = underTest.generalClusterConfigs(stackRequest, Credential.builder().build(), null,
                CLUSTER_MANAGER_VARIANT);

        assertThat(generalClusterConfigs.getVariant()).isEqualTo(CLUSTER_MANAGER_VARIANT);
    }

}