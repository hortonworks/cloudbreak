package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterView;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxClusterViewRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxClusterViewRepository sdxClusterViewRepository;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EnvironmentService underTest;

    static Object[][] failedParamProvider() {
        return new Object[][]{
                {EnvironmentStatus.CREATE_FAILED},
                {EnvironmentStatus.DELETE_FAILED},
                {EnvironmentStatus.UPDATE_FAILED},
                {EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE}
        };
    }

    static Object[][] startParamProvider() {
        return new Object[][]{
                {EnvironmentStatus.ENV_STOPPED, "The environment is stopped. Please start the environment first!"},
                {EnvironmentStatus.STOP_FREEIPA_STARTED, "The environment is stopped. Please start the environment first!"},
                {EnvironmentStatus.START_FREEIPA_STARTED, "The environment is starting. Please wait until finished!"}
        };
    }

    static Object[][] deleteInProgressParamProvider() {
        return new Object[][]{
                {EnvironmentStatus.DELETE_INITIATED},
                {EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS},
                {EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS},
                {EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS},
                {EnvironmentStatus.UMS_RESOURCE_DELETE_IN_PROGRESS},
                {EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS},
                {EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS}
        };
    }

    @Test
    void testWaitEnvironmentNetworkCreationFinished() {

        Long sdxId = 42L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(sdxId);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn("crn");
        sdxCluster.setClusterName("cluster");

        Optional<SdxCluster> sdxClusterOptional = Optional.of(sdxCluster);
        when(sdxClusterRepository.findById(sdxId)).thenReturn(sdxClusterOptional);

        when(environmentEndpoint.getByCrn("crn"))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS));

        PollingConfig pollingConfig = new PollingConfig(100, TimeUnit.MILLISECONDS, 10, TimeUnit.SECONDS);

        DetailedEnvironmentResponse environment = underTest.waitAndGetEnvironment(sdxId, pollingConfig, EnvironmentStatus::isNetworkCreationFinished);

        assertThat(environment.getEnvironmentStatus(), is(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS));
        verify(sdxClusterRepository).findById(sdxId);
        verifyNoMoreInteractions(sdxClusterRepository);
        verify(environmentEndpoint, times(3)).getByCrn("crn");
        verifyNoMoreInteractions(environmentEndpoint);
    }

    @Test
    void testWaitEnvironmentAvailable() {

        Long sdxId = 42L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(sdxId);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn("crn");
        sdxCluster.setClusterName("cluster");

        Optional<SdxCluster> sdxClusterOptional = Optional.of(sdxCluster);
        when(sdxClusterRepository.findById(sdxId)).thenReturn(sdxClusterOptional);

        when(environmentEndpoint.getByCrn("crn"))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS))
                .thenReturn(getDetailedEnvironmentResponseWithStatus(EnvironmentStatus.AVAILABLE));

        PollingConfig pollingConfig = new PollingConfig(100, TimeUnit.MILLISECONDS, 10, TimeUnit.SECONDS);

        DetailedEnvironmentResponse environment = underTest.waitAndGetEnvironment(sdxId, pollingConfig, EnvironmentStatus::isAvailable);

        assertThat(environment.getEnvironmentStatus(), is(EnvironmentStatus.AVAILABLE));
        verify(sdxClusterRepository).findById(sdxId);
        verifyNoMoreInteractions(sdxClusterRepository);
        verify(environmentEndpoint, times(5)).getByCrn("crn");
        verifyNoMoreInteractions(environmentEndpoint);
    }

    @Test
    void testGetResourceCrnByResourceName() {
        when(sdxClusterRepository.findAuthorizationResourcesByAccountIdAndEnvName(any(), any())).thenReturn(List.of());
        assertThrows(NotFoundException.class, () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.getResourceCrnByResourceName("name")));

        when(sdxClusterRepository.findAuthorizationResourcesByAccountIdAndEnvName(any(), any())).thenReturn(List.of(
                new ResourceWithId(1L, "crn")));
        assertThrows(NotFoundException.class, () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.getResourceCrnByResourceName("name")));

        when(sdxClusterRepository.findAuthorizationResourcesByAccountIdAndEnvName(any(), any())).thenReturn(List.of(
                new ResourceWithId(1L, "crn", Optional.empty())));
        assertThrows(NotFoundException.class, () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.getResourceCrnByResourceName("name")));

        when(sdxClusterRepository.findAuthorizationResourcesByAccountIdAndEnvName(any(), any())).thenReturn(List.of(
                new ResourceWithId(1L, "crn", Optional.of("parent")), new ResourceWithId(1L, "crn", Optional.of("parent"))));
        assertEquals("parent", ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.getResourceCrnByResourceName("name")));
    }

    @Test
    void testGetResourceCrnListByResourceNameList() {
        when(sdxClusterRepository.findAuthorizationResourcesByAccountIdAndEnvNames(any(), any())).thenReturn(List.of());
        assertTrue(ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.getResourceCrnListByResourceNameList(List.of("name"))).isEmpty());

        when(sdxClusterRepository.findAuthorizationResourcesByAccountIdAndEnvNames(any(), any())).thenReturn(List.of(
                new ResourceWithId(1L, "crn1", Optional.empty()),
                new ResourceWithId(1L, "crn1", "env1"),
                new ResourceWithId(2L, "crn2", "env2"),
                new ResourceWithId(3L, "crn3"),
                new ResourceWithId(4L, "crn4", "env2"),
                new ResourceWithId(5L, "crn5", "env3")
        ));
        List<String> envList = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getResourceCrnListByResourceNameList(List.of("name")));
        assertFalse(envList.isEmpty());
        assertEquals(3, envList.size());
        assertTrue(envList.containsAll(List.of("env1", "env2", "env3")));
    }

    @Test
    void testGetNamesByCrns() {
        when(sdxClusterViewRepository.findByAccountIdAndEnvCrnIn(any(), any())).thenReturn(Set.of());
        assertTrue(ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getNamesByCrnsForMessage(List.of("crn"))).isEmpty());

        when(sdxClusterViewRepository.findByAccountIdAndEnvCrnIn(any(), any())).thenReturn(Set.of(
                getClusterView(null, null),
                getClusterView("envCrn1", null),
                getClusterView("envCrn2", "envName2"),
                getClusterView("envCrn2", "envName2"),
                getClusterView(null, "whatever")
        ));
        Map<String, Optional<String>> nameMap = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.getNamesByCrnsForMessage(List.of("crn")));
        assertFalse(nameMap.isEmpty());
        assertTrue(nameMap.keySet().containsAll(Set.of("envCrn2")));
        assertEquals(1, nameMap.entrySet().size());
        assertTrue(nameMap.values().stream().map(Optional::get).collect(Collectors.toSet()).contains("envName2"));
        assertFalse(nameMap.values().stream().map(Optional::get).collect(Collectors.toSet()).contains("whatever"));
    }

    @ParameterizedTest
    @MethodSource("failedParamProvider")
    void testValidateAndGetEnvironmentButEnvInFailedPhase(EnvironmentStatus environmentStatus) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        detailedEnvironmentResponse.setName("envName");

        when(environmentEndpoint.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateAndGetEnvironment("envName"),
                "BadRequestException should thrown");

        assertEquals("The environment is in failed phase. Please fix the environment or create a new one first!", badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("startParamProvider")
    void testValidateAndGetEnvironmentButEnvInStoppedStatus(EnvironmentStatus environmentStatus, String exceptionMessage) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        detailedEnvironmentResponse.setName("envName");

        when(environmentEndpoint.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateAndGetEnvironment("envName"), "BadRequestException should thrown");

        assertEquals(exceptionMessage, badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("deleteInProgressParamProvider")
    void testValidateAndGetEnvironmentButEnvInDeleteInProgressPhase(EnvironmentStatus environmentStatus) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        detailedEnvironmentResponse.setName("envName");

        when(environmentEndpoint.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateAndGetEnvironment("envName"), "BadRequestException should thrown");

        assertEquals("The environment is in delete in progress phase. Please create a new environment first!", badRequestException.getMessage());
    }

    @Test
    void testValidateAndGetEnvironmentForHybridEnvironment() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setName("envName");
        detailedEnvironmentResponse.setAccountId("123");
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setEnvironmentType(EnvironmentType.HYBRID_BASE.toString());

        when(environmentEndpoint.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        when(entitlementService.internalTenant(detailedEnvironmentResponse.getAccountId())).thenReturn(false);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateAndGetEnvironment("envName"), "BadRequestException should thrown");

        assertEquals("Creating or Resizing datalake is not supported for Hybrid Environment", badRequestException.getMessage());
    }

    private SdxClusterView getClusterView(String envCrn, String envName) {
        SdxClusterView clusterView = new SdxClusterView();
        clusterView.setEnvCrn(envCrn);
        clusterView.setEnvName(envName);
        return clusterView;
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponseWithStatus(EnvironmentStatus status) {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        env.setEnvironmentStatus(status);
        return env;
    }
}
