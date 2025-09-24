package com.sequenceiq.remoteenvironment.service;

import static com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType.CLASSIC_CLUSTER;
import static com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType.PRIVATE_CONTROL_PLANE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@ExtendWith(MockitoExtension.class)
class RemoteEnvironmentServiceTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private RemoteEnvironmentConnector connector1;

    @Mock
    private RemoteEnvironmentConnector connector2;

    @Mock
    private RemoteEnvironmentConnectorProvider remoteEnvironmentConnectorProvider;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private RemoteEnvironmentService underTest;

    @Test
    void testListWithAllType() {
        when(entitlementService.hybridCloudEnabled(any())).thenReturn(true);
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env4 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env5 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(CLASSIC_CLUSTER)).thenReturn(connector1);
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector2);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2));
        when(connector2.list(anyString())).thenReturn(Set.of(env3, env4, env5));
        SimpleRemoteEnvironmentResponses actual = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.list(List.of(CLASSIC_CLUSTER.name(), PRIVATE_CONTROL_PLANE.name())));
        assertTrue(actual.getResponses().containsAll(List.of(env1, env2, env3, env4, env5)));
        assertEquals(5, actual.getResponses().size());
    }

    @Test
    void testListWithUnknownTypes() {
        when(entitlementService.hybridCloudEnabled(any())).thenReturn(true);
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector1);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2, env3));
        SimpleRemoteEnvironmentResponses actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.list(List.of("unknown1", "unknown2")));
        assertTrue(actual.getResponses().containsAll(List.of(env1, env2, env3)));
        assertEquals(3, actual.getResponses().size());
    }

    @Test
    void testListWithMixedTypes() {
        when(entitlementService.hybridCloudEnabled(any())).thenReturn(true);
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env4 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env5 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(CLASSIC_CLUSTER)).thenReturn(connector1);
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector2);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2));
        when(connector2.list(anyString())).thenReturn(Set.of(env3, env4, env5));
        SimpleRemoteEnvironmentResponses actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.list(List.of("unknown1", "unknown2",
                PRIVATE_CONTROL_PLANE.name(), CLASSIC_CLUSTER.name(), "unknown3", CLASSIC_CLUSTER.name(), PRIVATE_CONTROL_PLANE.name())));
        assertTrue(actual.getResponses().containsAll(List.of(env1, env2, env3)));
        assertEquals(5, actual.getResponses().size());
    }

    @Test
    void testListWithEmptyTypesList() {
        when(entitlementService.hybridCloudEnabled(any())).thenReturn(true);
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector1);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2, env3));
        SimpleRemoteEnvironmentResponses actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.list(List.of()));
        assertTrue(actual.getResponses().containsAll(List.of(env1, env2, env3)));
        assertEquals(3, actual.getResponses().size());
    }

    @ParameterizedTest
    @EnumSource(RemoteEnvironmentConnectorType.class)
    void testListWithType(RemoteEnvironmentConnectorType connectorType) {
        when(entitlementService.hybridCloudEnabled(any())).thenReturn(true);
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(connectorType)).thenReturn(connector1);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2, env3));
        SimpleRemoteEnvironmentResponses actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.list(List.of(connectorType.name())));
        assertTrue(actual.getResponses().containsAll(List.of(env1, env2, env3)));
        assertEquals(3, actual.getResponses().size());
    }

    @Test
    void testListNoEntitlement() {
        when(entitlementService.hybridCloudEnabled(any())).thenReturn(false);
        SimpleRemoteEnvironmentResponses actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.list(null));
        verify(remoteEnvironmentConnectorProvider, never()).all();
        assertTrue(actual.getResponses().isEmpty());
    }
}
