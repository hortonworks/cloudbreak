package com.sequenceiq.remoteenvironment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.remotecluster.RemoteClusterInternalProto.RegisterPvcBaseClusterRequest;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;

@ExtendWith(MockitoExtension.class)
class PrivateEnvironmentBaseClusterServiceTest {

    private static final String CONTROL_PLANE_NAME = "aPrivateControlPlane";

    private static final String CONTROL_PLANE_CRN = "controlPlaneCRN";

    private static final String ENVIRONMENT_NAME = "aPrivateEnv";

    @Mock
    private RemoteClusterServiceClient grpcRemoteClusterClient;

    @InjectMocks
    private PrivateEnvironmentBaseClusterService underTest;

    static Stream<Arguments> testRegisterHappyPathsValueProvider() {
        return Stream.of(
                Arguments.of("https://testcloud:7183", ""),
                Arguments.of("https://testcloud-1.test-hybrid.root.comops.site:7183", ""),
                Arguments.of("https://testcloud-1.test-hybrid.root.comops.site:7183/almafa", "https://testcloudknox:7183"),
                Arguments.of("http://testcloud-1.test-hybrid.root.comops.site:7180/almafa", "https://knoxGatewayUrl:7183/knoxknox"),
                Arguments.of("http://testcloud-1.test-hybrid.root.comops.site:7180/", ""),
                Arguments.of("https://testcloud-1.test-hybrid.root.comops.site:7183/cdp-proxy...", "https://knoxGatewayUrl:7183/knox/....")
        );
    }

    @ParameterizedTest
    @MethodSource("testRegisterHappyPathsValueProvider")
    void testRegisterHappyPaths(String cmUrl, String knoxGatewayUrl) {
        String baseClusterCrn = "baseClusterCrn";
        String privateEnvironmentCrn = "privateEnvironmentCrn";
        DescribeEnvironmentResponse envDetails = mock(Answers.RETURNS_DEEP_STUBS);
        when(envDetails.getEnvironment().getPvcEnvironmentDetails().getCmHost()).thenReturn(cmUrl);
        when(envDetails.getEnvironment().getPvcEnvironmentDetails().getKnoxGatewayUrl()).thenReturn(knoxGatewayUrl);
        when(envDetails.getEnvironment().getEnvironmentName()).thenReturn(ENVIRONMENT_NAME);
        when(envDetails.getEnvironment().getCrn()).thenReturn(privateEnvironmentCrn);
        when(grpcRemoteClusterClient.registerPrivateEnvironmentBaseCluster(any())).thenReturn(baseClusterCrn);

        String registerBaseClusterCrn = underTest.registerBaseCluster(envDetails, CONTROL_PLANE_CRN, CONTROL_PLANE_NAME);

        ArgumentCaptor<RegisterPvcBaseClusterRequest> regRequestCaptor = ArgumentCaptor.forClass(RegisterPvcBaseClusterRequest.class);
        verify(grpcRemoteClusterClient, times(1)).registerPrivateEnvironmentBaseCluster(regRequestCaptor.capture());
        assertEquals(baseClusterCrn, registerBaseClusterCrn);
        RegisterPvcBaseClusterRequest capturedRequest = regRequestCaptor.getValue();
        assertEquals(cmUrl, capturedRequest.getCmUrl());
        assertEquals(knoxGatewayUrl, capturedRequest.getKnoxGatewayUrl());
        assertEquals(CONTROL_PLANE_NAME + "_" + ENVIRONMENT_NAME + "_datacenter", capturedRequest.getDcName());
        assertEquals(privateEnvironmentCrn, capturedRequest.getEnvironmentCrn());
    }

    @Test
    void testRegisterWhenCMHostDoesNotMatchWithPattern() {
        DescribeEnvironmentResponse envDetails = mock(Answers.RETURNS_DEEP_STUBS);
        when(envDetails.getEnvironment().getPvcEnvironmentDetails().getCmHost()).thenReturn("https://testcloud:7199");
        when(envDetails.getEnvironment().getEnvironmentName()).thenReturn(ENVIRONMENT_NAME);

        String registerBaseClusterCrn = underTest.registerBaseCluster(envDetails, CONTROL_PLANE_CRN, CONTROL_PLANE_NAME);

        verifyNoInteractions(grpcRemoteClusterClient);
        assertNull(registerBaseClusterCrn);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testRegisterWhenCMHostIsNullOrEmpty(String cmHost) {
        DescribeEnvironmentResponse envDetails = mock(Answers.RETURNS_DEEP_STUBS);
        when(envDetails.getEnvironment().getPvcEnvironmentDetails().getCmHost()).thenReturn(cmHost);
        when(envDetails.getEnvironment().getEnvironmentName()).thenReturn(ENVIRONMENT_NAME);

        String registerBaseClusterCrn = underTest.registerBaseCluster(envDetails, CONTROL_PLANE_CRN, CONTROL_PLANE_NAME);

        verifyNoInteractions(grpcRemoteClusterClient);
        assertNull(registerBaseClusterCrn);
    }

    @Test
    void testRegisterWhenPrivateEnvironmentDetailsIsNotPreset() {
        DescribeEnvironmentResponse envDetails = mock(Answers.RETURNS_DEEP_STUBS);
        when(envDetails.getEnvironment().getPvcEnvironmentDetails()).thenReturn(null);
        when(envDetails.getEnvironment().getEnvironmentName()).thenReturn(ENVIRONMENT_NAME);

        String registerBaseClusterCrn = underTest.registerBaseCluster(envDetails, CONTROL_PLANE_CRN, CONTROL_PLANE_NAME);

        verifyNoInteractions(grpcRemoteClusterClient);
        assertNull(registerBaseClusterCrn);
    }
}