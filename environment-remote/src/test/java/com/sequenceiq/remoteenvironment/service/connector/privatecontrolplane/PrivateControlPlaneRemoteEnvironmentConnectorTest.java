package com.sequenceiq.remoteenvironment.service.connector.privatecontrolplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.EnvironmentSummary;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListEnvironmentsResponse;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.test.AsyncTaskExecutorTestImpl;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.RemoteEnvironmentBase;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;
import com.sequenceiq.remoteenvironment.controller.v1.converter.PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;
import com.sequenceiq.remoteenvironment.service.PrivateControlPlaneService;

@ExtendWith(MockitoExtension.class)
class PrivateControlPlaneRemoteEnvironmentConnectorTest {

    private static final String TENANT = "5abe6882-ff63-4ad2-af86-a5582872a9cd";

    private static final String ENV_CRN =
            String.format("crn:altus:environments:us-west-1:%s:environment:test-hybrid-1/06533e78-b2bd-41c9-8ac4-c4109af7797b", TENANT);

    private static final String PUBLIC_CLOUD_ACCOUNT_ID = "publicCloudAccountId";

    private static final String CONTROL_PLANE = "controlPlane";

    @Mock
    private PrivateControlPlaneEnvironmentToRemoteEnvironmentConverter converterMock;

    @Mock
    private PrivateControlPlaneService privateControlPlaneServiceMock;

    @Mock
    private PrivateControlPlaneClient privateControlPlaneClient;

    @Spy
    private AsyncTaskExecutorTestImpl taskExecutor;

    @InjectMocks
    private PrivateControlPlaneRemoteEnvironmentConnector underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "taskExecutor", taskExecutor);
    }

    @Test
    public void testListRemoteEnvironmentShouldReturnEnvs() {
        PrivateControlPlane privateControlPlane1 = new PrivateControlPlane();
        privateControlPlane1.setResourceCrn("CRN1");
        privateControlPlane1.setName("NAME1");
        privateControlPlane1.setUrl("URL1");
        privateControlPlane1.setAccountId("ACCOUNT1");
        privateControlPlane1.setId(1L);
        privateControlPlane1.setPrivateCloudAccountId("privateAccountId");
        PrivateControlPlane privateControlPlane2 = new PrivateControlPlane();
        privateControlPlane2.setResourceCrn("CRN2");
        privateControlPlane2.setName("NAME2");
        privateControlPlane2.setUrl("URL2");
        privateControlPlane2.setAccountId("ACCOUNT2");
        privateControlPlane2.setId(2L);
        privateControlPlane2.setPrivateCloudAccountId("privateAccountId");
        when(privateControlPlaneServiceMock.listByAccountId(anyString()))
                .thenReturn(new HashSet<>(Arrays.asList(privateControlPlane1, privateControlPlane2)));

        EnvironmentSummary environment1 = new EnvironmentSummary();
        environment1.setEnvironmentName("NAME1");
        environment1.setCrn("crn:cdp:hybrid:us-west-1:privateAccountId:environment:b24");
        environment1.setStatus("AVAILABLE");
        environment1.setCloudPlatform("HYBRID");
        EnvironmentSummary environment2 = new EnvironmentSummary();
        environment2.setEnvironmentName("NAME2");
        environment2.setCrn("CRN2");
        environment2.setStatus("AVAILABLE");
        environment2.setCloudPlatform("HYBRID");
        environment2.setCrn("crn:cdp:hybrid:us-west-1:privateAccountId:environment:b25");

        ListEnvironmentsResponse environmentResponses1 = new ListEnvironmentsResponse();
        environmentResponses1.setEnvironments(List.of(environment1));

        ListEnvironmentsResponse environmentResponses2 = new ListEnvironmentsResponse();
        environmentResponses2.setEnvironments(List.of(environment2));

        when(privateControlPlaneClient.listEnvironments(eq("CRN1"), any())).thenReturn(environmentResponses1);
        when(privateControlPlaneClient.listEnvironments(eq("CRN2"), any())).thenReturn(environmentResponses2);

        SimpleRemoteEnvironmentResponse response1 = new SimpleRemoteEnvironmentResponse();
        response1.setName("NAME1");
        response1.setUrl("URL1");
        response1.setRegion("REGION1");
        response1.setStatus("STATUS1");
        response1.setPrivateControlPlaneName("PVC1");
        response1.setCreated(new Date().getTime());
        response1.setCloudPlatform("HYBRID");
        SimpleRemoteEnvironmentResponse response2 = new SimpleRemoteEnvironmentResponse();
        response2.setName("NAME2");
        response2.setUrl("URL2");
        response2.setRegion("REGION2");
        response2.setStatus("STATUS2");
        response2.setPrivateControlPlaneName("PVC2");
        response2.setCreated(new Date().getTime());
        response2.setCloudPlatform("HYBRID");
        when(converterMock.convert(eq(environment1), eq(privateControlPlane1))).thenReturn(response1);
        when(converterMock.convert(eq(environment2), eq(privateControlPlane2))).thenReturn(response2);

        Collection<SimpleRemoteEnvironmentResponse> result = underTest.list("sampleAccountId");

        assertEquals(2, result.size());
        verify(taskExecutor, times(result.size())).submit(any(Callable.class));
        assertTrue(result.stream()
                .map(RemoteEnvironmentBase::getName)
                .anyMatch(e -> e.equalsIgnoreCase(response2.getName())));
        assertTrue(result.stream()
                .map(RemoteEnvironmentBase::getName)
                .anyMatch(e -> e.equalsIgnoreCase(response1.getName())));
    }

    @Test
    public void testListRemoteEnvironmentWhenEnvAccountIdMismatchWithCPShouldReturnEmptyList() {
        EnvironmentSummary environment1 = new EnvironmentSummary();
        environment1.setEnvironmentName("NAME1");
        environment1.setCrn("crn:cdp:hybrid:us-west-1:suspiciousAccountId:environment:b24");
        environment1.setStatus("AVAILABLE");
        environment1.setCloudPlatform("HYBRID");
        ListEnvironmentsResponse environmentResponses1 = new ListEnvironmentsResponse();
        environmentResponses1.setEnvironments(List.of(environment1));

        Collection<SimpleRemoteEnvironmentResponse> result = underTest.list("sampleAccountId");

        assertTrue(result.isEmpty());
        verifyNoInteractions(converterMock);
    }

    @Test
    public void testGetRemoteEnvironmentWhenCrnIsInvalid() {
        DescribeRemoteEnvironment describeRemoteEnvironment = new DescribeRemoteEnvironment();
        describeRemoteEnvironment.setCrn("crn:altus:us-west-1:5abe6882-ff63:environment:test-hybrid-1/06533e78-b2bd");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest
                .describeV1(PUBLIC_CLOUD_ACCOUNT_ID, "crn:altus:us-west-1:5abe6882-ff63:environment:test-hybrid-1/06533e78-b2bd"));

        assertEquals("The provided environment CRN('crn:altus:us-west-1:5abe6882-ff63:environment:test-hybrid-1/06533e78-b2bd') is invalid",
                ex.getMessage());
        verifyNoInteractions(privateControlPlaneServiceMock);
        verifyNoInteractions(privateControlPlaneClient);
    }

    @Test
    public void testGetRemoteEnvironmentWhenPrivateControlPlanePresentedShouldReturnEnv() {
        PrivateControlPlane privateControlPlane = new PrivateControlPlane();
        privateControlPlane.setPrivateCloudAccountId("accountId");
        privateControlPlane.setUrl("url");
        privateControlPlane.setResourceCrn("crn");
        privateControlPlane.setId(1L);
        privateControlPlane.setPrivateCloudAccountId("5abe6882-ff63-4ad2-af86-a5582872a9cd");

        DescribeEnvironmentV2Response describeEnvironmentResponse = new DescribeEnvironmentV2Response();

        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(anyString(), anyString()))
                .thenReturn(Optional.of(privateControlPlane));
        when(privateControlPlaneClient.getEnvironment(anyString(), any(), anyString()))
                .thenReturn(describeEnvironmentResponse);

        DescribeEnvironmentResponse result = underTest
                .describeV1(
                        PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN
                        );

        assertEquals(describeEnvironmentResponse, result);
    }

    @Test
    public void testGetDescribeV2WhenPrivateControlPlanePresentedShouldReturnEnv() {
        PrivateControlPlane privateControlPlane = new PrivateControlPlane();
        privateControlPlane.setPrivateCloudAccountId("accountId");
        privateControlPlane.setUrl("url");
        privateControlPlane.setResourceCrn("crn");
        privateControlPlane.setId(1L);
        privateControlPlane.setPrivateCloudAccountId("5abe6882-ff63-4ad2-af86-a5582872a9cd");

        DescribeEnvironmentV2Response describeEnvironmentResponse = new DescribeEnvironmentV2Response();
        Environment environment = new Environment();
        environment.setEnvironmentName("env1");
        describeEnvironmentResponse.setEnvironment(environment);

        when(privateControlPlaneClient.getEnvironment(anyString(), any(), anyString()))
                .thenReturn(describeEnvironmentResponse);
        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(any(), any()))
                .thenReturn(Optional.of(privateControlPlane));

        DescribeEnvironmentV2Response result = underTest
                .describeV2(
                        PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN
                );

        assertEquals(describeEnvironmentResponse, result);

    }

    @Test
    public void testGetRemoteEnvironmentWhenPrivateControlPlaneNotPresented() {
        DescribeRemoteEnvironment describeRemoteEnvironment = new DescribeRemoteEnvironment();
        describeRemoteEnvironment.setCrn(ENV_CRN);

        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest
                .describeV1(
                        PUBLIC_CLOUD_ACCOUNT_ID,
                        ENV_CRN));

        assertEquals("There is no control plane for this account with account id 5abe6882-ff63-4ad2-af86-a5582872a9cd.",
                ex.getMessage());
    }

    @Test
    public void testGetRemoteEnvironmentThrowsExceptionWhenClusterProxyFails() {
        DescribeRemoteEnvironment describeRemoteEnvironment = new DescribeRemoteEnvironment();
        describeRemoteEnvironment.setCrn(ENV_CRN);

        PrivateControlPlane privateControlPlane = mock(PrivateControlPlane.class);
        when(privateControlPlane.getResourceCrn()).thenReturn(CONTROL_PLANE);
        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(TENANT, PUBLIC_CLOUD_ACCOUNT_ID))
                .thenReturn(Optional.of(privateControlPlane));
        when(privateControlPlaneClient.getEnvironment(eq(CONTROL_PLANE), any(), eq(ENV_CRN)))
                .thenThrow(new RuntimeException());

        RuntimeException runtimeException = assertThrows(RuntimeException.class,
                () -> underTest.describeV1(PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN));
        assertEquals(String.format(String.format("Unable to fetch environment for crn %s", ENV_CRN)), runtimeException.getMessage());
    }

    @Test
    public void testGetRemoteDataContext() {
        PrivateControlPlane privateControlPlane = mock(PrivateControlPlane.class);
        when(privateControlPlane.getResourceCrn()).thenReturn(CONTROL_PLANE);
        DescribeDatalakeAsApiRemoteDataContextResponse describeDatalakeAsApiRemoteDataContextResponse
                = mock(DescribeDatalakeAsApiRemoteDataContextResponse.class);

        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(TENANT, PUBLIC_CLOUD_ACCOUNT_ID))
                .thenReturn(Optional.of(privateControlPlane));
        when(privateControlPlaneClient.getRemoteDataContext(eq(CONTROL_PLANE), any(), eq(ENV_CRN)))
                .thenReturn(describeDatalakeAsApiRemoteDataContextResponse);

        DescribeDatalakeAsApiRemoteDataContextResponse result = underTest.getRemoteDataContext(PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN);
        assertEquals(describeDatalakeAsApiRemoteDataContextResponse, result);
    }

    @Test
    public void testGetRemoteDataContextThrowsBadRequestExceptionInvalidCrn() {
        String invalidCrn = "test";
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.getRemoteDataContext(PUBLIC_CLOUD_ACCOUNT_ID, invalidCrn));
        assertEquals(String.format("The provided environment CRN('%s') is invalid", invalidCrn), badRequestException.getMessage());
    }

    @Test
    public void testGetRemoteDataContextThrowsBadRequestExceptionControlPlaneDoesNotExist() {
        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(TENANT, PUBLIC_CLOUD_ACCOUNT_ID))
                .thenReturn(Optional.empty());

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.getRemoteDataContext(PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN));
        assertEquals(String.format("There is no control plane for this account with account id %s.", TENANT), badRequestException.getMessage());
    }

    @Test
    public void testGetRemoteDataContextThrowsExceptionWhenClusterProxyFails() {
        PrivateControlPlane privateControlPlane = mock(PrivateControlPlane.class);
        when(privateControlPlane.getResourceCrn()).thenReturn(CONTROL_PLANE);
        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(TENANT, PUBLIC_CLOUD_ACCOUNT_ID))
                .thenReturn(Optional.of(privateControlPlane));
        when(privateControlPlaneClient.getRemoteDataContext(eq(CONTROL_PLANE), any(), eq(ENV_CRN)))
                .thenThrow(new RuntimeException());

        RuntimeException runtimeException = assertThrows(RuntimeException.class,
                () -> underTest.getRemoteDataContext(PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN));
        assertEquals(String.format(String.format("Unable to fetch remote data context for crn %s", ENV_CRN)), runtimeException.getMessage());
    }

    @Test
    public void testGetDatalakeServices() {
        PrivateControlPlane privateControlPlane = mock(PrivateControlPlane.class);
        when(privateControlPlane.getResourceCrn()).thenReturn(CONTROL_PLANE);
        DescribeDatalakeServicesResponse describeDatalakeServicesResponse
                = mock(DescribeDatalakeServicesResponse.class);

        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(TENANT, PUBLIC_CLOUD_ACCOUNT_ID))
                .thenReturn(Optional.of(privateControlPlane));
        when(privateControlPlaneClient.getDatalakeServices(eq(CONTROL_PLANE), any(), eq(ENV_CRN)))
                .thenReturn(describeDatalakeServicesResponse);

        DescribeDatalakeServicesResponse result = underTest.getDatalakeServices(PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN);
        assertEquals(describeDatalakeServicesResponse, result);
    }

    @Test
    public void testGetDatalakeServicesThrowsBadRequestExceptionInvalidCrn() {
        String invalidCrn = "test";
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.getDatalakeServices(PUBLIC_CLOUD_ACCOUNT_ID, invalidCrn));
        assertEquals(String.format("The provided environment CRN('%s') is invalid", invalidCrn), badRequestException.getMessage());
    }

    @Test
    public void testGetDatalakeServicesThrowsBadRequestExceptionControlPlaneDoesNotExist() {

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.getDatalakeServices(PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN));
        assertEquals(String.format("There is no control plane for this account with account id %s.", TENANT), badRequestException.getMessage());
    }

    @Test
    public void testGetDatalakeServicesThrowsExceptionWhenClusterProxyFails() {
        PrivateControlPlane privateControlPlane = mock(PrivateControlPlane.class);
        when(privateControlPlane.getResourceCrn()).thenReturn(CONTROL_PLANE);
        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(TENANT, PUBLIC_CLOUD_ACCOUNT_ID))
                .thenReturn(Optional.of(privateControlPlane));
        when(privateControlPlaneClient.getDatalakeServices(eq(CONTROL_PLANE), any(), eq(ENV_CRN)))
                .thenThrow(new RuntimeException());

        RuntimeException runtimeException = assertThrows(RuntimeException.class,
                () -> underTest.getDatalakeServices(PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN));
        assertEquals(String.format(String.format("Unable to fetch data lake services for crn %s", ENV_CRN)), runtimeException.getMessage());
    }

    @Test
    public void testGetRootCertificateWhenPrivateControlPlanePresentedShouldReturn() {
        PrivateControlPlane privateControlPlane = new PrivateControlPlane();
        privateControlPlane.setPrivateCloudAccountId("accountId");
        privateControlPlane.setUrl("url");
        privateControlPlane.setResourceCrn("crn");
        privateControlPlane.setId(1L);
        privateControlPlane.setPrivateCloudAccountId("5abe6882-ff63-4ad2-af86-a5582872a9cd");

        when(privateControlPlaneServiceMock.getByPrivateCloudAccountIdAndPublicCloudAccountId(anyString(), anyString()))
                .thenReturn(Optional.of(privateControlPlane));
        when(privateControlPlaneClient.getRootCertificate(anyString(), any(), anyString()))
                .thenReturn(new GetRootCertificateResponse().contents("certecske"));

        GetRootCertificateResponse result = underTest
                .getRootCertificate(
                        PUBLIC_CLOUD_ACCOUNT_ID, ENV_CRN
                );

        assertEquals("certecske", result.getContents());
    }
}