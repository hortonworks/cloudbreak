package com.sequenceiq.remoteenvironment.service;

import static com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType.CLASSIC_CLUSTER;
import static com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType.PRIVATE_CONTROL_PLANE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.cloudbreak.util.test.AsyncTaskExecutorTestImpl;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponses;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnector;
import com.sequenceiq.remoteenvironment.service.connector.RemoteEnvironmentConnectorType;

@ExtendWith(MockitoExtension.class)
class RemoteEnvironmentServiceTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String ACCOUNT_ID = "1234";

    private static final String ENVIRONMENT_CRN = "crn";

    @Mock
    private RemoteEnvironmentConnector connector1;

    @Mock
    private RemoteEnvironmentConnector connector2;

    @Mock
    private RemoteEnvironmentConnectorProvider remoteEnvironmentConnectorProvider;

    @Spy
    private AsyncTaskExecutorTestImpl intermediateBuilderExecutor;

    @InjectMocks
    private RemoteEnvironmentService underTest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "intermediateBuilderExecutor", intermediateBuilderExecutor);
    }

    @Test
    void testListWithAllType() {
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env4 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env5 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(CLASSIC_CLUSTER)).thenReturn(connector1);
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector2);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2));
        when(connector2.list(anyString())).thenReturn(Set.of(env3, env4, env5));
        SimpleRemoteEnvironmentResponses actual = underTest.list(USER_CRN, List.of(CLASSIC_CLUSTER.name(), PRIVATE_CONTROL_PLANE.name()));
        assertTrue(actual.getResponses().containsAll(List.of(env1, env2, env3, env4, env5)));
        assertEquals(5, actual.getResponses().size());
    }

    @Test
    void testListWithUnknownTypes() {
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector1);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2, env3));
        SimpleRemoteEnvironmentResponses actual = underTest.list(USER_CRN, List.of("unknown1", "unknown2"));
        assertTrue(actual.getResponses().containsAll(List.of(env1, env2, env3)));
        assertEquals(3, actual.getResponses().size());
    }

    @Test
    void testListWithMixedTypes() {
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env4 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env5 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(CLASSIC_CLUSTER)).thenReturn(connector1);
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector2);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2));
        when(connector2.list(anyString())).thenReturn(Set.of(env3, env4, env5));
        SimpleRemoteEnvironmentResponses actual = underTest.list(USER_CRN, List.of("unknown1", "unknown2",
                PRIVATE_CONTROL_PLANE.name(), CLASSIC_CLUSTER.name(), "unknown3", CLASSIC_CLUSTER.name(), PRIVATE_CONTROL_PLANE.name()));
        assertTrue(actual.getResponses().containsAll(List.of(env1, env2, env3)));
        assertEquals(5, actual.getResponses().size());
    }

    @Test
    void testListWithEmptyTypesList() {
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector1);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2, env3));
        SimpleRemoteEnvironmentResponses actual = underTest.list(USER_CRN, List.of());
        assertTrue(List.of(env1, env2, env3).containsAll(actual.getResponses()));
        assertEquals(3, actual.getResponses().size());
    }

    @Test
    void testListWithPrivateControlPlaceType() {
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector1);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2, env3));
        SimpleRemoteEnvironmentResponses actual = underTest.list(USER_CRN, List.of(PRIVATE_CONTROL_PLANE.name()));
        assertTrue(List.of(env1, env2, env3).containsAll(actual.getResponses()));
        assertEquals(3, actual.getResponses().size());
        verifyNoMoreInteractions(remoteEnvironmentConnectorProvider);
    }

    @Test
    void testListWithClassicClusterType() {
        RemoteEnvironmentConnectorType connectorType = CLASSIC_CLUSTER;
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        env1.setEnvironmentCrn("crn:env1");
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        env2.setEnvironmentCrn("crn:env2");
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(connectorType)).thenReturn(connector1);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2, env3));
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector2);
        SimpleRemoteEnvironmentResponse pvcEnv1 = new SimpleRemoteEnvironmentResponse();
        pvcEnv1.setEnvironmentCrn("crn:env2");
        when(connector2.list(anyString())).thenReturn(Set.of(pvcEnv1));
        SimpleRemoteEnvironmentResponses actual = underTest.list(USER_CRN, List.of(connectorType.name()));
        assertTrue(List.of(env2, env3).containsAll(actual.getResponses()));
        assertEquals(2, actual.getResponses().size());
    }

    @Test
    void testListWithBothTypes() {
        SimpleRemoteEnvironmentResponse env1 = new SimpleRemoteEnvironmentResponse();
        env1.setCrn("crn:env1");
        SimpleRemoteEnvironmentResponse env2 = new SimpleRemoteEnvironmentResponse();
        SimpleRemoteEnvironmentResponse env3 = new SimpleRemoteEnvironmentResponse();
        when(remoteEnvironmentConnectorProvider.getForType(CLASSIC_CLUSTER)).thenReturn(connector1);
        when(connector1.list(anyString())).thenReturn(Set.of(env1, env2, env3));
        when(remoteEnvironmentConnectorProvider.getForType(PRIVATE_CONTROL_PLANE)).thenReturn(connector2);
        SimpleRemoteEnvironmentResponse pvcEnv1 = new SimpleRemoteEnvironmentResponse();
        pvcEnv1.setCrn("crn:env1");
        when(connector2.list(anyString())).thenReturn(Set.of(pvcEnv1));
        SimpleRemoteEnvironmentResponses actual = underTest.list(USER_CRN, List.of(CLASSIC_CLUSTER.name(), PRIVATE_CONTROL_PLANE.name()));
        assertTrue(List.of(env1, env2, env3, pvcEnv1).containsAll(actual.getResponses()));
        assertEquals(4, actual.getResponses().size());
    }

    @Test
    void testDescribeV1() {
        DescribeEnvironmentResponse response = new DescribeEnvironmentResponse();
        DescribeRemoteEnvironment request = new DescribeRemoteEnvironment();
        request.setCrn(ENVIRONMENT_CRN);
        when(remoteEnvironmentConnectorProvider.getForCrn(ENVIRONMENT_CRN)).thenReturn(connector1);
        when(connector1.describeV1(USER_CRN, ENVIRONMENT_CRN)).thenReturn(response);

        DescribeEnvironmentResponse result = underTest.describeV1(USER_CRN, request);

        assertEquals(response, result);
    }

    @Test
    void testDescribeV2() {
        DescribeEnvironmentV2Response response = new DescribeEnvironmentV2Response();
        DescribeRemoteEnvironment request = new DescribeRemoteEnvironment();
        request.setCrn(ENVIRONMENT_CRN);
        when(remoteEnvironmentConnectorProvider.getForCrn(ENVIRONMENT_CRN)).thenReturn(connector1);
        when(connector1.describeV2(USER_CRN, ENVIRONMENT_CRN)).thenReturn(response);

        DescribeEnvironmentV2Response result = underTest.describeV2(USER_CRN, request);

        assertEquals(response, result);
    }
}
