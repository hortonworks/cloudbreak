package com.sequenceiq.environment.environment.service.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.common.api.type.EnvironmentType;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:1234:environment:abc";

    private static final String STACK_CRN_1 = "crn:cdp:datahub:us-west-1:1234:cluster:s1";

    private static final String STACK_CRN_2 = "crn:cdp:datahub:us-west-1:1234:cluster:s2";

    private static final String STACK_CRN_3 = "crn:cdp:datahub:us-west-1:1234:cluster:s3";

    private static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:encryption:us-west-1:1234:profile:ep1";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

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
}
