package com.sequenceiq.environment.environment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.RegionWrapper;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.GcpNetwork;
import com.sequenceiq.environment.network.dao.domain.MockNetwork;
import com.sequenceiq.environment.network.dao.domain.YarnNetwork;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    private static final String ENV_NAME = "someEnv";

    @InjectMocks
    private EnvironmentService environmentServiceUnderTest;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private EnvironmentDtoConverter environmentDtoConverter;

    @Mock
    private EnvironmentValidatorService environmentValidatorService;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private RoleCrnGenerator roleCrnGenerator;

    private Environment environment;

    private EnvironmentDto environmentDto;

    @BeforeEach
    void setup() {
        lenient().when(roleCrnGenerator.getBuiltInEnvironmentAdminResourceRoleCrn(anyString()))
                .thenReturn("crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentAdmin");
        environment = new Environment();
        environmentDto = new EnvironmentDto();
    }

    @Test
    void getByNameAndAccountIdNotFound() {
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> environmentServiceUnderTest
                .getByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID));
    }

    @Test
    void testListByCrnInternal() {
        when(environmentRepository.findOneByResourceCrnEvenIfDeleted(anyString())).thenReturn(
                Optional.of(new Environment())
        );
        when(environmentDtoConverter.environmentToDto(any())).thenReturn(new EnvironmentDto());

        EnvironmentDto environmentDto = environmentServiceUnderTest.internalGetByCrn("crn");
        Assert.assertNotNull(environmentDto);
    }

    @Test
    void getByNameAndAccountIdFound() {
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any(Environment.class))).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentServiceUnderTest
                .getByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID));
    }

    @Test
    void getByCrnAndAccountIdNotFound() {
        when(environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> environmentServiceUnderTest
                .getByCrnAndAccountId(TestConstants.CRN, TestConstants.ACCOUNT_ID));
    }

    @Test
    void getByCrnAndAccountId() {
        when(environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any(Environment.class))).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentServiceUnderTest
                .getByCrnAndAccountId(TestConstants.CRN, TestConstants.ACCOUNT_ID));
    }

    @Test
    void listByAccountId() {
        Set<Environment> twoEnvironment = Set.of(new Environment(), new Environment());
        final int twoInvocations = 2;
        when(environmentRepository.findByAccountId(eq(TestConstants.ACCOUNT_ID))).thenReturn(twoEnvironment);
        environmentServiceUnderTest.listByAccountId(TestConstants.ACCOUNT_ID);
        verify(environmentRepository).findByAccountId(eq(TestConstants.ACCOUNT_ID));
        verify(environmentDtoConverter, times(twoInvocations)).environmentToDto(any(Environment.class));
    }

    @Test
    void findEnvironmentByIdEmpty() {
        assertTrue(environmentServiceUnderTest.findEnvironmentById(1L).isEmpty());
        verify(environmentRepository).findById(eq(1L));
    }

    @Test
    void findEnvironmentById() {
        when(environmentRepository.findById(eq(1L))).thenReturn(Optional.of(environment));
        assertEquals(environment, environmentServiceUnderTest.findEnvironmentById(1L).get());
        verify(environmentRepository).findById(eq(1L));
    }

    @Test
    void findById() {
        when(environmentDtoConverter.environmentToDto(eq(environment))).thenReturn(environmentDto);
        when(environmentRepository.findById(eq(1L))).thenReturn(Optional.of(environment));
        assertEquals(environmentDto, environmentServiceUnderTest.findById(1L).get());
        verify(environmentRepository).findById(eq(1L));
    }

    @Test
    void findByIdEmpty() {
        when(environmentRepository.findById(eq(1L))).thenReturn(Optional.empty());
        assertTrue(environmentServiceUnderTest.findById(1L).isEmpty());
        verify(environmentRepository).findById(eq(1L));
    }

    @Test
    void setRegions() {
        environmentServiceUnderTest.setRegions(environment, Set.of("r1"), EnvironmentTestData.getCloudRegions());
        String resultText = environment.getRegions().getValue();
        assertTrue(resultText.contains("{\"name\":\"region-1\",\"displayName\":\"Here\"}"));
        assertFalse(resultText.contains("{\"name\":\"region2\",\"displayName\":\"region 2\"}"));
    }

    @Test
    void setRegionsTwoRegions() {
        environmentServiceUnderTest.setRegions(environment, Set.of("r1", "r2"), EnvironmentTestData.getCloudRegions());
        String resultText = environment.getRegions().getValue();
        assertTrue(resultText.contains("{\"name\":\"region-1\",\"displayName\":\"Here\"}"));
        assertTrue(resultText.contains("{\"name\":\"region-2\",\"displayName\":\"There\"}"));
    }

    @Test
    void setLocationByCoordinates() {
        RegionWrapper location = new RegionWrapper("r1", "Somewhere else", 1.2, 1.3, Collections.emptySet());
        environmentServiceUnderTest.setLocation(environment, location, EnvironmentTestData.getCloudRegions());
        assertEquals("Here", environment.getLocationDisplayName());
    }

    @Test
    void setLocationByLocation() {
        RegionWrapper location = new RegionWrapper("r3", "Somewhere else", 1.2, 1.3, Collections.emptySet());
        environmentServiceUnderTest.setLocation(environment, location, EnvironmentTestData.getCloudRegions());
        assertEquals("Somewhere else", environment.getLocationDisplayName());
    }

    @Test
    void setLocationLocationCoordinatesAreInvalid() {
        RegionWrapper location = new RegionWrapper("r3", "Somewhere else", null, 1.3, Collections.emptySet());
        assertThrows(BadRequestException.class, () -> environmentServiceUnderTest.setLocation(environment, location, EnvironmentTestData.getCloudRegions()));
    }

    @Test
    void findAllByIdInAndStatusIn() {
        Set<Long> resourceIds = Set.of(1L, 2L);
        Set<EnvironmentStatus> statuses = Set.of(EnvironmentStatus.AVAILABLE, EnvironmentStatus.CREATION_INITIATED);
        Set<Long> expectedResourceIds = Set.of(1L);
        when(environmentRepository
                .findAllIdByIdInAndStatusInAndArchivedIsFalse(eq(resourceIds), eq(statuses))).thenReturn(expectedResourceIds);

        assertEquals(expectedResourceIds, environmentServiceUnderTest.findAllIdByIdInAndStatusIn(resourceIds,
                statuses));
    }

    @Test
    void testRoleAssignment() {
        ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> {
            environmentServiceUnderTest.assignEnvironmentAdminRole(TestConstants.CRN, ENV_CRN);
        });

        verify(grpcUmsClient)
                .assignResourceRole(eq(TestConstants.CRN), eq(ENV_CRN), eq("crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentAdmin"), any());
    }

    @Test
    void testRoleAssignmentFail() {
        doThrow(new RuntimeException("Bad"))
                .when(grpcUmsClient).assignResourceRole(anyString(), anyString(), anyString(), any());

        ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> {
            environmentServiceUnderTest.assignEnvironmentAdminRole(TestConstants.CRN, ENV_CRN);
        });

        verify(grpcUmsClient)
                .assignResourceRole(eq(TestConstants.CRN), eq(ENV_CRN), eq("crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentAdmin"), any());
        verifyNoMoreInteractions(grpcUmsClient);
    }

    @Test
    void updateTunnelByEnvironmentId() {
        ExperimentalFeatures expFeat = environment.getExperimentalFeaturesJson();
        expFeat.setTunnel(Tunnel.CCM);
        environment.setExperimentalFeaturesJson(expFeat);
        Optional<Environment> optEnv = Optional.of(environment);
        when(environmentRepository.findById(123L)).thenReturn(optEnv);
        environmentServiceUnderTest.updateTunnelByEnvironmentId(123L, Tunnel.CCMV2_JUMPGATE);
        ArgumentCaptor<Environment> envCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(envCaptor.capture());
        Environment captured = envCaptor.getValue();
        assertThat(captured.getExperimentalFeaturesJson().getTunnel()).isEqualTo(Tunnel.CCMV2_JUMPGATE);
    }

    @Test
    void updateTunnelByEnvironmentIdNotFound() {
        Optional<Environment> env = Optional.empty();
        when(environmentRepository.findById(123L)).thenReturn(env);
        assertThatThrownBy(() -> environmentServiceUnderTest.updateTunnelByEnvironmentId(123L, Tunnel.CCMV2_JUMPGATE))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Upon a provided null instance the result should be an empty set without any repository call.")
    void testGetEnvironmentsUsingTheSameNetworkWithNullInput() {
        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there is only a single entry in the database for that specific AWS network which is exactly the one environment which is going to be " +
            "terminated, then an empty set is going to return, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithAws() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setVpcId("someVpcId");
        awsNetwork.setEnvironment(environment);
        when(environmentRepository.getAwsOrMockNetwokUsages(awsNetwork.getVpcId())).thenReturn(Set.of(environment.getId()));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(awsNetwork);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(environmentRepository, times(1)).getAwsOrMockNetwokUsages(awsNetwork.getVpcId());
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there are more entries in the database for that specific AWS network then a non-empty set is going to return whereas the original " +
            "environment is going to miss from the set, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithAwsMultiple() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        environment.setResourceCrn(ENV_CRN);
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setVpcId("someVpcId");
        awsNetwork.setEnvironment(environment);
        Long firstOtherEnvId = 2L;
        Long secondOtherEnvId = 3L;
        Map<Long, Environment> otherEnvs = createSimpleEnvInstance(firstOtherEnvId, secondOtherEnvId);
        when(environmentRepository.getAwsOrMockNetwokUsages(awsNetwork.getVpcId())).thenReturn(Set.of(environment.getId(), firstOtherEnvId, secondOtherEnvId));
        when(environmentRepository.findById(firstOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(firstOtherEnvId)));
        when(environmentRepository.findById(secondOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(secondOtherEnvId)));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(awsNetwork);

        assertNotNull(result);
        assertEquals(otherEnvs.size(), result.size());
        result.forEach(env -> assertNotEquals(env.getCrn(), environment.getResourceCrn()));

        verify(environmentRepository, times(1)).getAwsOrMockNetwokUsages(awsNetwork.getVpcId());
        verify(environmentRepository, times(1)).findById(firstOtherEnvId);
        verify(environmentRepository, times(1)).findById(secondOtherEnvId);
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there is only a single entry in the database for that specific Azure network which is exactly the one environment which is going to " +
            "be terminated, then an empty set is going to return, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithAzure() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        AzureNetwork awsNetwork = new AzureNetwork();
        awsNetwork.setResourceGroupName("someResourceGroupName");
        awsNetwork.setEnvironment(environment);
        when(environmentRepository.getAzureNetwokUsages(awsNetwork.getResourceGroupName())).thenReturn(Set.of(environment.getId()));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(awsNetwork);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(environmentRepository, times(1)).getAzureNetwokUsages(awsNetwork.getResourceGroupName());
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there are more entries in the database for that specific Azure network then a non-empty set is going to return whereas the original " +
            "environment is going to miss from the set, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithAzureMultiple() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        environment.setResourceCrn(ENV_CRN);
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setResourceGroupName("someResourceGroupName");
        azureNetwork.setEnvironment(environment);
        Long firstOtherEnvId = 2L;
        Long secondOtherEnvId = 3L;
        Map<Long, Environment> otherEnvs = createSimpleEnvInstance(firstOtherEnvId, secondOtherEnvId);
        when(environmentRepository.getAzureNetwokUsages(azureNetwork.getResourceGroupName()))
                .thenReturn(Set.of(environment.getId(), firstOtherEnvId, secondOtherEnvId));
        when(environmentRepository.findById(firstOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(firstOtherEnvId)));
        when(environmentRepository.findById(secondOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(secondOtherEnvId)));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(azureNetwork);

        assertNotNull(result);
        assertEquals(otherEnvs.size(), result.size());
        result.forEach(env -> assertNotEquals(env.getCrn(), environment.getResourceCrn()));

        verify(environmentRepository, times(1)).getAzureNetwokUsages(azureNetwork.getResourceGroupName());
        verify(environmentRepository, times(1)).findById(firstOtherEnvId);
        verify(environmentRepository, times(1)).findById(secondOtherEnvId);
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there is only a single entry in the database for that specific GCP network which is exactly the one environment which is going to " +
            "be terminated, then an empty set is going to return, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithGcp() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        GcpNetwork gcpNetwork = new GcpNetwork();
        gcpNetwork.setNetworkId("someNetworkId");
        gcpNetwork.setEnvironment(environment);
        when(environmentRepository.getGcpNetwokUsages(gcpNetwork.getNetworkId())).thenReturn(Set.of(environment.getId()));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(gcpNetwork);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(environmentRepository, times(1)).getGcpNetwokUsages(gcpNetwork.getNetworkId());
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there are more entries in the database for that specific GCP network then a non-empty set is going to return whereas the original " +
            "environment is going to miss from the set, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithGCPMultiple() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        environment.setResourceCrn(ENV_CRN);
        GcpNetwork gcpNetwork = new GcpNetwork();
        gcpNetwork.setNetworkId("someNetworkId");
        gcpNetwork.setEnvironment(environment);
        long firstOtherEnvId = 2L;
        long secondOtherEnvId = 3L;
        Map<Long, Environment> otherEnvs = createSimpleEnvInstance(firstOtherEnvId, secondOtherEnvId);
        when(environmentRepository.getGcpNetwokUsages(gcpNetwork.getNetworkId())).thenReturn(Set.of(environment.getId(), firstOtherEnvId, secondOtherEnvId));
        when(environmentRepository.findById(firstOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(firstOtherEnvId)));
        when(environmentRepository.findById(secondOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(secondOtherEnvId)));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(gcpNetwork);

        assertNotNull(result);
        assertEquals(otherEnvs.size(), result.size());
        result.forEach(env -> assertNotEquals(env.getCrn(), environment.getResourceCrn()));

        verify(environmentRepository, times(1)).getGcpNetwokUsages(gcpNetwork.getNetworkId());
        verify(environmentRepository, times(1)).findById(firstOtherEnvId);
        verify(environmentRepository, times(1)).findById(secondOtherEnvId);
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there is only a single entry in the database for that specific Yarn network which is exactly the one environment which is going to " +
            "be terminated, then an empty set is going to return, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithYarn() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        YarnNetwork yarnNetwork = new YarnNetwork();
        yarnNetwork.setQueue("someQueue");
        yarnNetwork.setEnvironment(environment);
        when(environmentRepository.getYarnNetwokUsages(yarnNetwork.getQueue())).thenReturn(Set.of(environment.getId()));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(yarnNetwork);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(environmentRepository, times(1)).getYarnNetwokUsages(yarnNetwork.getQueue());
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there are more entries in the database for that specific Yarn network then a non-empty set is going to return whereas the original " +
            "environment is going to miss from the set, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithYarnMultiple() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        environment.setResourceCrn(ENV_CRN);
        YarnNetwork yarnNetwork = new YarnNetwork();
        yarnNetwork.setQueue("someQueue");
        yarnNetwork.setEnvironment(environment);
        long firstOtherEnvId = 2L;
        long secondOtherEnvId = 3L;
        Map<Long, Environment> otherEnvs = createSimpleEnvInstance(firstOtherEnvId, secondOtherEnvId);
        when(environmentRepository.getYarnNetwokUsages(yarnNetwork.getQueue())).thenReturn(Set.of(environment.getId(), firstOtherEnvId, secondOtherEnvId));
        when(environmentRepository.findById(firstOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(firstOtherEnvId)));
        when(environmentRepository.findById(secondOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(secondOtherEnvId)));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(yarnNetwork);

        assertNotNull(result);
        assertEquals(otherEnvs.size(), result.size());
        result.forEach(env -> assertNotEquals(env.getCrn(), environment.getResourceCrn()));

        verify(environmentRepository, times(1)).getYarnNetwokUsages(yarnNetwork.getQueue());
        verify(environmentRepository, times(1)).findById(firstOtherEnvId);
        verify(environmentRepository, times(1)).findById(secondOtherEnvId);
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there is only a single entry in the database for that specific Mock network which is exactly the one environment which is going to " +
            "be terminated, then an empty set is going to return, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithMock() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        MockNetwork mockNetwork = new MockNetwork();
        mockNetwork.setVpcId("vpcId");
        mockNetwork.setEnvironment(environment);
        when(environmentRepository.getAwsOrMockNetwokUsages(mockNetwork.getVpcId())).thenReturn(Set.of(environment.getId()));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(mockNetwork);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(environmentRepository, times(1)).getAwsOrMockNetwokUsages(mockNetwork.getVpcId());
        verifyNoMoreInteractions(environmentRepository);
    }

    @Test
    @DisplayName("When there are more entries in the database for that specific Mock network then a non-empty set is going to return whereas the original " +
            "environment is going to miss from the set, since we are removing the caller's environment from the list to enlist only those " +
            "environments that uses the network beside the original.")
    void testGetEnvironmentsUsingTheSameNetworkWithMockMultiple() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(1L);
        environment.setResourceCrn(ENV_CRN);
        MockNetwork mockNetwork = new MockNetwork();
        mockNetwork.setVpcId("someVpcId");
        mockNetwork.setEnvironment(environment);
        long firstOtherEnvId = 2L;
        long secondOtherEnvId = 3L;
        Map<Long, Environment> otherEnvs = createSimpleEnvInstance(firstOtherEnvId, secondOtherEnvId);
        when(environmentRepository.getAwsOrMockNetwokUsages(mockNetwork.getVpcId())).thenReturn(Set.of(environment.getId(), firstOtherEnvId, secondOtherEnvId));
        when(environmentRepository.findById(firstOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(firstOtherEnvId)));
        when(environmentRepository.findById(secondOtherEnvId)).thenReturn(Optional.of(otherEnvs.get(secondOtherEnvId)));

        Set<NameOrCrn> result = environmentServiceUnderTest.getEnvironmentsUsingTheSameNetwork(mockNetwork);

        assertNotNull(result);
        assertEquals(otherEnvs.size(), result.size());
        result.forEach(env -> assertNotEquals(env.getCrn(), environment.getResourceCrn()));

        verify(environmentRepository, times(1)).getAwsOrMockNetwokUsages(mockNetwork.getVpcId());
        verify(environmentRepository, times(1)).findById(firstOtherEnvId);
        verify(environmentRepository, times(1)).findById(secondOtherEnvId);
        verifyNoMoreInteractions(environmentRepository);
    }

    private Map<Long, Environment> createSimpleEnvInstance(Long... ids) {
        Map<Long, Environment> environments = new LinkedHashMap<>(ids.length);
        for (Long id : ids) {
            Environment env = new Environment();
            env.setId(id);
            env.setName("env_" + id + "_name");
            env.setResourceCrn("env_" + id + "_crn");
            environments.put(id, env);
        }
        return environments;
    }

    @Configuration
    @Import(EnvironmentService.class)
    static class Config {
    }

}
