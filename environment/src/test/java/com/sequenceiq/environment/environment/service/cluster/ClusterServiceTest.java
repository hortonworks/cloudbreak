package com.sequenceiq.environment.environment.service.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterServiceConfigurationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.ClusterServiceConfigurationResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:1234:environment:abc";

    private static final String STACK_CRN_1 = "crn:cdp:datahub:us-west-1:1234:cluster:s1";

    private static final String STACK_CRN_2 = "crn:cdp:datahub:us-west-1:1234:cluster:s2";

    private static final String STACK_CRN_3 = "crn:cdp:datahub:us-west-1:1234:cluster:s3";

    private static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:encryption:us-west-1:1234:profile:ep1";

    private static final String TRUSTED_REALM = "TRUSTED.REALM.EXAMPLE.COM";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private FreeIpaService freeIpaService;

    @InjectMocks
    private ClusterService underTest;

    @Test
    void getClustersNamesByEncryptionProfileDelegatesToEndpoint() {
        List<String> expectedNames = List.of("cluster1", "cluster2");
        when(stackV4Endpoint.getClustersNamesByEncryptionProfile(0L, ENCRYPTION_PROFILE_CRN)).thenReturn(expectedNames);

        List<String> result = underTest.getClustersNamesByEncryptionProfile(ENCRYPTION_PROFILE_CRN);

        assertThat(result).containsExactlyElementsOf(expectedNames);
        verify(stackV4Endpoint).getClustersNamesByEncryptionProfile(0L, ENCRYPTION_PROFILE_CRN);
    }

    @Test
    void getStackCrnsForConfigUpdateReturnsOperationalStacks() {
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.AVAILABLE, StackType.WORKLOAD),
                stackView(STACK_CRN_2, Status.STOPPED, StackType.WORKLOAD)
        );
        when(stackV4Endpoint.list(0L, ENV_CRN, false)).thenReturn(responses);

        List<String> result = underTest.getStackCrnsForConfigUpdate(ENV_CRN, EnvironmentType.PUBLIC_CLOUD);

        assertThat(result).containsExactly(STACK_CRN_1);
    }

    @Test
    void getStackCrnsForConfigUpdateFiltersOutAllInoperableStates() {
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.CREATE_FAILED, StackType.WORKLOAD),
                stackView(STACK_CRN_2, Status.DELETE_IN_PROGRESS, StackType.WORKLOAD),
                stackView(STACK_CRN_3, Status.DELETE_COMPLETED, StackType.WORKLOAD)
        );
        when(stackV4Endpoint.list(0L, ENV_CRN, false)).thenReturn(responses);

        List<String> result = underTest.getStackCrnsForConfigUpdate(ENV_CRN, EnvironmentType.PUBLIC_CLOUD);

        assertThat(result).isEmpty();
    }

    @Test
    void getStackCrnsForConfigUpdateForHybridEnvironmentIncludesOnlyWorkloads() {
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.AVAILABLE, StackType.WORKLOAD),
                stackView(STACK_CRN_2, Status.AVAILABLE, StackType.DATALAKE)
        );
        when(stackV4Endpoint.list(0L, ENV_CRN, false)).thenReturn(responses);

        List<String> result = underTest.getStackCrnsForConfigUpdate(ENV_CRN, EnvironmentType.HYBRID);

        assertThat(result).containsExactly(STACK_CRN_1);
    }

    @Test
    void getStackCrnsForConfigUpdateForPublicCloudIncludesAllStackTypes() {
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.AVAILABLE, StackType.WORKLOAD),
                stackView(STACK_CRN_2, Status.AVAILABLE, StackType.DATALAKE)
        );
        when(stackV4Endpoint.list(0L, ENV_CRN, false)).thenReturn(responses);

        List<String> result = underTest.getStackCrnsForConfigUpdate(ENV_CRN, EnvironmentType.PUBLIC_CLOUD);

        assertThat(result).containsExactlyInAnyOrder(STACK_CRN_1, STACK_CRN_2);
    }

    @Test
    void getStackCrnsForConfigUpdateReturnsEmptyWhenNoStacks() {
        StackViewV4Responses responses = buildStackViewV4Responses();
        when(stackV4Endpoint.list(0L, ENV_CRN, false)).thenReturn(responses);

        List<String> result = underTest.getStackCrnsForConfigUpdate(ENV_CRN, EnvironmentType.PUBLIC_CLOUD);

        assertThat(result).isEmpty();
    }

    @Test
    void updateTrustedRealmsOnClustersWithEnvironmentDtoSendsTrustedRealm() {
        EnvironmentDto environmentDto = buildEnvironmentDto(ENV_CRN, EnvironmentType.PUBLIC_CLOUD);
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.AVAILABLE, StackType.WORKLOAD)
        );
        when(stackV4Endpoint.list(0L, ENV_CRN, false)).thenReturn(responses);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse(TRUSTED_REALM)));
        when(stackV4Endpoint.getClusterServiceConfiguration(anyLong(), any(), any())).thenReturn(new ClusterServiceConfigurationResponse());

        underTest.updateTrustedRealmsOnClusters(Optional.of(environmentDto));

        ArgumentCaptor<UpdateClusterServiceConfigurationRequest> captor =
                ArgumentCaptor.forClass(UpdateClusterServiceConfigurationRequest.class);
        verify(stackV4Endpoint).updateClusterServiceConfiguration(eq(0L), eq(STACK_CRN_1), captor.capture());
        UpdateClusterServiceConfigurationRequest request = captor.getValue();
        assertThat(request.getServiceConfigurations()).hasSize(1);
        assertThat(request.getServiceConfigurations().get(0).getServiceName()).isEqualTo("core_settings");
        assertThat(request.getServiceConfigurations().get(0).getConfigName()).isEqualTo("trusted_realms");
        assertThat(request.getServiceConfigurations().get(0).getValue()).isEqualTo(TRUSTED_REALM);
    }

    @Test
    void updateTrustedRealmsOnClustersWithEmptyOptionalUsesPublicCloudType() {
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.AVAILABLE, StackType.WORKLOAD)
        );
        when(freeIpaService.describe(null)).thenReturn(Optional.empty());

        assertThrows(CloudbreakServiceException.class, () -> underTest.updateTrustedRealmsOnClusters(Optional.empty()));

        verify(stackV4Endpoint, never()).updateClusterServiceConfiguration(eq(0L), eq(STACK_CRN_1), any());
    }

    @Test
    void updateTrustedRealmsOnClustersWithNoFreeIpaSendsNullRealm() {
        EnvironmentDto environmentDto = buildEnvironmentDto(ENV_CRN, EnvironmentType.PUBLIC_CLOUD);
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.AVAILABLE, StackType.WORKLOAD)
        );
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.empty());

        assertThrows(CloudbreakServiceException.class, () -> underTest.updateTrustedRealmsOnClusters(Optional.of(environmentDto)));

        ArgumentCaptor<UpdateClusterServiceConfigurationRequest> captor =
                ArgumentCaptor.forClass(UpdateClusterServiceConfigurationRequest.class);
        verify(stackV4Endpoint, never()).updateClusterServiceConfiguration(eq(0L), eq(STACK_CRN_1), captor.capture());
    }

    @Test
    void updateTrustedRealmsOnClustersSkipsInoperableStacks() {
        EnvironmentDto environmentDto = buildEnvironmentDto(ENV_CRN, EnvironmentType.PUBLIC_CLOUD);
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.STOPPED, StackType.WORKLOAD),
                stackView(STACK_CRN_2, Status.DELETE_IN_PROGRESS, StackType.WORKLOAD)
        );
        when(stackV4Endpoint.list(0L, ENV_CRN, false)).thenReturn(responses);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse(TRUSTED_REALM)));

        underTest.updateTrustedRealmsOnClusters(Optional.of(environmentDto));

        verify(stackV4Endpoint, never()).updateClusterServiceConfiguration(anyLong(), any(), any());
    }

    @Test
    void updateTrustedRealmsOnClustersUpdatesMultipleStacks() {
        EnvironmentDto environmentDto = buildEnvironmentDto(ENV_CRN, EnvironmentType.PUBLIC_CLOUD);
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.AVAILABLE, StackType.WORKLOAD),
                stackView(STACK_CRN_2, Status.AVAILABLE, StackType.WORKLOAD)
        );
        when(stackV4Endpoint.list(0L, ENV_CRN, false)).thenReturn(responses);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse(TRUSTED_REALM)));
        when(stackV4Endpoint.getClusterServiceConfiguration(anyLong(), any(), any())).thenReturn(new ClusterServiceConfigurationResponse());
        underTest.updateTrustedRealmsOnClusters(Optional.of(environmentDto));

        verify(stackV4Endpoint, times(1)).updateClusterServiceConfiguration(eq(0L), eq(STACK_CRN_1), any());
        verify(stackV4Endpoint, times(1)).updateClusterServiceConfiguration(eq(0L), eq(STACK_CRN_2), any());
    }

    @Test
    void updateTrustedRealmsOnClustersForHybridSkipsDatalakeStacks() {
        EnvironmentDto environmentDto = buildEnvironmentDto(ENV_CRN, EnvironmentType.HYBRID);
        StackViewV4Responses responses = buildStackViewV4Responses(
                stackView(STACK_CRN_1, Status.AVAILABLE, StackType.WORKLOAD),
                stackView(STACK_CRN_2, Status.AVAILABLE, StackType.DATALAKE)
        );
        when(stackV4Endpoint.list(0L, ENV_CRN, false)).thenReturn(responses);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse(TRUSTED_REALM)));
        when(stackV4Endpoint.getClusterServiceConfiguration(anyLong(), any(), any())).thenReturn(new ClusterServiceConfigurationResponse());

        underTest.updateTrustedRealmsOnClusters(Optional.of(environmentDto));

        verify(stackV4Endpoint, times(1)).updateClusterServiceConfiguration(eq(0L), eq(STACK_CRN_1), any());
        verify(stackV4Endpoint, never()).updateClusterServiceConfiguration(eq(0L), eq(STACK_CRN_2), any());
    }

    private StackViewV4Responses buildStackViewV4Responses(StackViewV4Response... stacks) {
        StackViewV4Responses responses = new StackViewV4Responses();
        responses.setResponses(java.util.Arrays.asList(stacks));
        return responses;
    }

    private StackViewV4Response stackView(String crn, Status clusterStatus, StackType stackType) {
        ClusterViewV4Response cluster = new ClusterViewV4Response();
        cluster.setStatus(clusterStatus);

        StackViewV4Response stack = new StackViewV4Response();
        stack.setCrn(crn);
        stack.setCluster(cluster);
        stack.setStackType(stackType.name());
        stack.setStatus(clusterStatus);
        return stack;
    }

    private EnvironmentDto buildEnvironmentDto(String resourceCrn, EnvironmentType environmentType) {
        EnvironmentDto dto = new EnvironmentDto();
        dto.setResourceCrn(resourceCrn);
        dto.setEnvironmentType(environmentType);
        return dto;
    }

    private DescribeFreeIpaResponse describeFreeIpaResponse(String realm) {
        TrustResponse trustResponse = new TrustResponse();
        trustResponse.setRealm(realm);

        DescribeFreeIpaResponse response = new DescribeFreeIpaResponse();
        response.setTrust(trustResponse);
        return response;
    }
}

