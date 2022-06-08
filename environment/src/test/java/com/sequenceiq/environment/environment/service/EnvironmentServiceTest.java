package com.sequenceiq.environment.environment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.RegionWrapper;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

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
        List<Environment> qresult = List.of(environment);
        when(environmentDtoConverter.environmentToDto(eq(environment))).thenReturn(environmentDto);
        when(environmentRepository
                .findAllByIdInAndStatusInAndArchivedIsFalse(eq(resourceIds), eq(statuses))).thenReturn(qresult);
        assertEquals(List.of(environmentDto), environmentServiceUnderTest.findAllByIdInAndStatusIn(resourceIds,
                statuses));
    }

    @Test
    void findAllByStatusIn() {
        when(environmentDtoConverter.environmentToDto(eq(environment))).thenReturn(environmentDto);
        when(environmentRepository
                .findAllByStatusInAndArchivedIsFalse(eq(Set.of(EnvironmentStatus.AVAILABLE, EnvironmentStatus.CREATION_INITIATED))))
                .thenReturn(List.of(environment));
        assertEquals(List.of(environmentDto), environmentServiceUnderTest
                .findAllByStatusIn(Set.of(EnvironmentStatus.AVAILABLE, EnvironmentStatus.CREATION_INITIATED)));

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

    @Configuration
    @Import(EnvironmentService.class)
    static class Config {

    }

}
