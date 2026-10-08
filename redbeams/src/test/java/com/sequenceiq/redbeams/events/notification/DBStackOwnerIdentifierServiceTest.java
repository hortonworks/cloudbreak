package com.sequenceiq.redbeams.events.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
class DBStackOwnerIdentifierServiceTest {

    private static final long INTERNAL_WORKSPACE_ID = 0L;

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc:cluster:dh";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc:datalake:dl";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc:environment:env";

    private static final String CLUSTER_NAME = "my-cluster";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxEndpoint sdxEndpoint;

    @InjectMocks
    private DBStackOwnerIdentifierService underTest;

    @Test
    void shouldResolveDataHubNotificationStateViaStackEndpoint() {
        StackV4Response stackV4Response = Mockito.mock(StackV4Response.class);
        when(stackV4Response.getName()).thenReturn(CLUSTER_NAME);
        when(stackV4Response.getCrn()).thenReturn(DATAHUB_CRN);
        when(stackV4Response.getNotificationState()).thenReturn(NotificationState.ENABLED);
        when(stackV4Endpoint.getByCrn(INTERNAL_WORKSPACE_ID, DATAHUB_CRN, Set.of())).thenReturn(stackV4Response);

        DBSourceClusterDto result = underTest.getOwningClusterNotificationState(DATAHUB_CRN);

        assertThat(result.sourceClusterName()).isEqualTo(CLUSTER_NAME);
        assertThat(result.sourceClusterCrn()).isEqualTo(DATAHUB_CRN);
        assertThat(result.stackType()).isEqualTo(StackType.WORKLOAD);
        assertThat(result.notificationState()).isEqualTo(NotificationState.ENABLED);
        verifyNoInteractions(sdxEndpoint);
    }

    @Test
    void shouldResolveDataLakeNotificationStateViaSdxEndpoint() {
        SdxClusterResponse sdxClusterResponse = Mockito.mock(SdxClusterResponse.class);
        when(sdxClusterResponse.getName()).thenReturn(CLUSTER_NAME);
        when(sdxClusterResponse.getCrn()).thenReturn(DATALAKE_CRN);
        when(sdxClusterResponse.getNotificationState()).thenReturn(NotificationState.DISABLED);
        when(sdxEndpoint.getByCrn(DATALAKE_CRN)).thenReturn(sdxClusterResponse);

        DBSourceClusterDto result = underTest.getOwningClusterNotificationState(DATALAKE_CRN);

        assertThat(result.sourceClusterName()).isEqualTo(CLUSTER_NAME);
        assertThat(result.sourceClusterCrn()).isEqualTo(DATALAKE_CRN);
        assertThat(result.stackType()).isEqualTo(StackType.DATALAKE);
        assertThat(result.notificationState()).isEqualTo(NotificationState.DISABLED);
        verifyNoInteractions(stackV4Endpoint);
    }

    @Test
    void shouldReturnDisabledForNonClusterCrnWithoutCallingAnyEndpoint() {
        DBSourceClusterDto result = underTest.getOwningClusterNotificationState(ENV_CRN);

        assertThat(result.sourceClusterCrn()).isEqualTo(ENV_CRN);
        assertThat(result.stackType()).isEqualTo(StackType.LEGACY);
        assertThat(result.notificationState()).isEqualTo(NotificationState.DISABLED);
        verifyNoInteractions(stackV4Endpoint, sdxEndpoint);
    }

    @Test
    void shouldFailClosedToDisabledWhenOwningEndpointThrows() {
        when(stackV4Endpoint.getByCrn(INTERNAL_WORKSPACE_ID, DATAHUB_CRN, Set.of())).thenThrow(new RuntimeException("core down"));

        DBSourceClusterDto result = underTest.getOwningClusterNotificationState(DATAHUB_CRN);

        assertThat(result.sourceClusterCrn()).isEqualTo(DATAHUB_CRN);
        assertThat(result.stackType()).isEqualTo(StackType.LEGACY);
        assertThat(result.notificationState()).isEqualTo(NotificationState.DISABLED);
    }
}
