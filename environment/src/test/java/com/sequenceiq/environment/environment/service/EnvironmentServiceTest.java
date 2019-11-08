package com.sequenceiq.environment.environment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.cloudera.cdp.environments.model.CreateAWSEnvironmentRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.util.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.v1.EnvironmentDtoToCreateAWSEnvironmentRequestConverter;
import com.sequenceiq.environment.environment.v1.EnvironmentRequestToCreateAWSEnvironmentRequestConverter;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    @InjectMocks
    private EnvironmentService environmentServiceUnderTest;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private EnvironmentDtoConverter environmentDtoConverter;

    @Mock
    private EnvironmentResourceDeletionService environmentResourceDeletionService;

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private EnvironmentRequestToCreateAWSEnvironmentRequestConverter environmentRequestToCreateAWSEnvironmentRequestConverter;

    @Mock
    private EnvironmentDtoToCreateAWSEnvironmentRequestConverter environmentDtoToCreateAWSEnvironmentRequestConverter;

    @Mock
    private EnvironmentValidatorService environmentValidatorService;

    private Environment environment;

    private EnvironmentDto environmentDto;

    @BeforeEach
    public void setup() {
        environment = new Environment();
        environmentDto = new EnvironmentDto();
    }

    @Test
    public void getByNameAndAccountIdNotFound() {
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> environmentServiceUnderTest
                .getByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID));
    }

    @Test
    public void getByNameAndAccountIdFound() {
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any())).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentServiceUnderTest
                .getByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID));
    }

    @Test
    public void getByCrnAndAccountIdNotFound() {
        when(environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> environmentServiceUnderTest
                .getByCrnAndAccountId(TestConstants.CRN, TestConstants.ACCOUNT_ID));
    }

    @Test
    public void getByCrnAndAccountId() {
        when(environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any())).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentServiceUnderTest
                .getByCrnAndAccountId(TestConstants.CRN, TestConstants.ACCOUNT_ID));
    }

    @Test
    public void listByAccountId() {
        Set<Environment> twoEnvironment = Set.of(new Environment(), new Environment());
        final int twoInvocations = 2;
        when(environmentRepository.findByAccountId(eq(TestConstants.ACCOUNT_ID))).thenReturn(twoEnvironment);
        environmentServiceUnderTest.listByAccountId(TestConstants.ACCOUNT_ID);
        verify(environmentRepository).findByAccountId(eq(TestConstants.ACCOUNT_ID));
        verify(environmentDtoConverter, times(twoInvocations)).environmentToDto(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByNameAndAccountIdNotFound(String valueString) {
        boolean forced = Boolean.parseBoolean(valueString);

        EnvironmentService environmentServiceWired = spy(environmentServiceUnderTest);
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> environmentServiceWired
                        .deleteByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID, TestConstants.USER,
                                forced));
        verify(environmentServiceWired, never()).delete(eq(environment), eq(TestConstants.USER), anyBoolean());
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any());
        verify(reactorFlowManager, never()).triggerForcedDeleteFlow(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByNameAndAccountId(String valueString) {
        boolean forced = Boolean.parseBoolean(valueString);

        EnvironmentService environmentServiceWired = spy(environmentServiceUnderTest);
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any())).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentServiceWired
                .deleteByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID, TestConstants.USER, forced));
        verify(environmentServiceWired).delete(eq(environment), eq(TestConstants.USER), anyBoolean());
        if (forced) {
            verify(reactorFlowManager).triggerForcedDeleteFlow(eq(environment), eq(TestConstants.USER));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
            verify(reactorFlowManager, never()).triggerForcedDeleteFlow(any(), any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByCrnAndAccountIdNotFound(String valueString) {
        boolean forced = Boolean.parseBoolean(valueString);

        EnvironmentService environmentServiceWired = spy(environmentServiceUnderTest);
        when(environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> environmentServiceWired.deleteByCrnAndAccountId(
                        TestConstants.CRN, TestConstants.ACCOUNT_ID, TestConstants.USER, forced));
        verify(environmentServiceWired, never()).delete(eq(environment), eq(TestConstants.USER), anyBoolean());
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any());
        verify(reactorFlowManager, never()).triggerForcedDeleteFlow(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByCrnAndAccountId(String valueString) {
        boolean forced = Boolean.parseBoolean(valueString);

        EnvironmentService environmentServiceWired = spy(environmentServiceUnderTest);
        when(environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any())).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentServiceWired
                .deleteByCrnAndAccountId(TestConstants.CRN, TestConstants.ACCOUNT_ID, TestConstants.USER, forced));
        verify(environmentServiceWired).delete(eq(environment), eq(TestConstants.USER), anyBoolean());
        if (forced) {
            verify(reactorFlowManager).triggerForcedDeleteFlow(eq(environment), eq(TestConstants.USER));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
            verify(reactorFlowManager, never()).triggerForcedDeleteFlow(any(), any());
        }
    }

    @Test
    public void delete() {
        assertEquals(environment, environmentServiceUnderTest.delete(environment, TestConstants.USER, false));
        verify(reactorFlowManager).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
    }

    @Test
    public void deleteFailedDistroXesAreAttached() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(eq(environment))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentServiceUnderTest.delete(environment, TestConstants.USER, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
    }

    @Test
    public void deleteFailedSdxesAreAttached() {
        when(environmentResourceDeletionService.getAttachedSdxClusterNames(eq(environment))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentServiceUnderTest.delete(environment, TestConstants.USER, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
    }

    @Test
    public void deleteFailedDataLakesAreAttached() {
        when(environmentResourceDeletionService.getDatalakeClusterNames(eq(environment))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentServiceUnderTest.delete(environment, TestConstants.USER, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
    }

    @Test
    public void deleteMultipleByNames() {
        Set<String> names = Set.of("name1", "name2");
        Set<Environment> envs = Set.of(new Environment(), new Environment());
        int expected = envs.size();
        EnvironmentService environmentServiceWired = spy(environmentServiceUnderTest);

        when(environmentRepository
                .findByNameInAndAccountIdAndArchivedIsFalse(eq(names), eq(TestConstants.ACCOUNT_ID))).thenReturn(envs);
        assertEquals(expected, environmentServiceWired
                .deleteMultipleByNames(names, TestConstants.ACCOUNT_ID, TestConstants.USER, false).size());
        verify(environmentServiceWired, times(expected)).delete(any(), eq(TestConstants.USER), anyBoolean());
    }

/*  TODO: fix deleteMultipleByNames
    @Test
    public void deleteMultipleByNamesOneEnvIsNotDeletable() {
        Set<String> names = Set.of("name1", "name2");
        Environment envIsNotDeletable = new Environment();
        Environment envIsOK = new Environment();
        envIsNotDeletable.setName("name1");
        envIsOK.setName("name2");
        Set<Environment> envs = Set.of(envIsNotDeletable, envIsOK);
        int expected = envs.size();

        EnvironmentService environmentServiceWired = spy(environmentServiceUnderTest);
        when(environmentRepository.findByNameInAndAccountId(eq(names), eq(ACCOUNT_ID))).thenReturn(envs);
        when(environmentResourceDeletionService.getAttachedSdxClusterNames(eq(envIsNotDeletable))).thenReturn(Set.of("used"));
        when(environmentResourceDeletionService.getAttachedSdxClusterNames(eq(envIsOK))).thenReturn(Set.of());
        assertEquals(1, environmentServiceWired.deleteMultipleByNames(names, ACCOUNT_ID, USER).size());
        verify(environmentServiceWired, times(expected)).delete(any(), eq(USER));
    }*/

    @Test
    public void deleteMultipleByCrns() {
        environmentServiceUnderTest
                .deleteMultipleByCrns(Set.of("crn1", "crn2"), TestConstants.ACCOUNT_ID, TestConstants.USER, false);

        Set<String> names = Set.of("crn1", "crn2");
        Set<Environment> envs = Set.of(new Environment(), new Environment());
        int expected = envs.size();
        EnvironmentService environmentServiceWired = spy(environmentServiceUnderTest);

        when(environmentRepository
                .findByResourceCrnInAndAccountIdAndArchivedIsFalse(eq(names), eq(TestConstants.ACCOUNT_ID))).thenReturn(envs);
        assertEquals(expected, environmentServiceWired.deleteMultipleByCrns(names, TestConstants.ACCOUNT_ID, TestConstants.USER,
                false).size());
        verify(environmentServiceWired, times(expected)).delete(any(), eq(TestConstants.USER), anyBoolean());
    }

    /*  TODO: fix deleteMultipleByCrns
    @Test
    public void deleteMultipleByCrnsOneEnvIsNotDeletable() {
        Set<String> names = Set.of("crn1", "crn2");
        Environment envIsNotDeletable = new Environment();
        Environment envIsOK = new Environment();
        envIsNotDeletable.setName("name1");
        envIsOK.setName("name2");
        Set<Environment> envs = Set.of(envIsNotDeletable, envIsOK);
        int expected = envs.size();

        EnvironmentService environmentServiceWired = spy(environmentServiceUnderTest);
        when(environmentRepository.findByResourceCrnInAndAccountId(eq(names), eq(ACCOUNT_ID))).thenReturn(envs);
        when(environmentResourceDeletionService.getAttachedSdxClusterNames(eq(envIsNotDeletable))).thenReturn(Set.of("used"));
        when(environmentResourceDeletionService.getAttachedSdxClusterNames(eq(envIsOK))).thenReturn(Set.of());
        assertEquals(1, environmentServiceWired.deleteMultipleByCrns(names, ACCOUNT_ID, USER).size());
        verify(environmentServiceWired, times(expected)).delete(any(), eq(USER));
    }*/

    @Test
    public void findEnvironmentByIdEmpty() {
        assertTrue(environmentServiceUnderTest.findEnvironmentById(1L).isEmpty());
        verify(environmentRepository).findById(eq(1L));
    }

    @Test
    public void findEnvironmentById() {
        when(environmentRepository.findById(eq(1L))).thenReturn(Optional.of(environment));
        assertEquals(environment, environmentServiceUnderTest.findEnvironmentById(1L).get());
        verify(environmentRepository).findById(eq(1L));
    }

    @Test
    public void findById() {
        when(environmentDtoConverter.environmentToDto(eq(environment))).thenReturn(environmentDto);
        when(environmentRepository.findById(eq(1L))).thenReturn(Optional.of(environment));
        assertEquals(environmentDto, environmentServiceUnderTest.findById(1L).get());
        verify(environmentRepository).findById(eq(1L));
    }

    @Test
    public void findByIdEmpty() {
        when(environmentRepository.findById(eq(1L))).thenReturn(Optional.empty());
        assertTrue(environmentServiceUnderTest.findById(1L).isEmpty());
        verify(environmentRepository).findById(eq(1L));
    }

    @Test
    public void setRegions() throws JsonProcessingException {
        environmentServiceUnderTest.setRegions(environment, Set.of("r1"), EnvironmentTestData.getCloudRegions());
        String resultText = environment.getRegions().getValue();
        assertTrue(resultText.contains("{\"name\":\"r1\",\"displayName\":\"region 1\"}"));
        assertFalse(resultText.contains("{\"name\":\"r2\",\"displayName\":\"region 2\"}"));
    }

    @Test
    public void setRegionsTwoRegions() throws JsonProcessingException {
        environmentServiceUnderTest.setRegions(environment, Set.of("r1", "r2"), EnvironmentTestData.getCloudRegions());
        String resultText = environment.getRegions().getValue();
        assertTrue(resultText.contains("{\"name\":\"r1\",\"displayName\":\"region 1\"}"));
        assertTrue(resultText.contains("{\"name\":\"r2\",\"displayName\":\"region 2\"}"));
    }

    @Test
    public void setLocationByCoordinates() {
        LocationDto location = new LocationDto("r1", "Somewhere else", 1.2, 1.3);
        environmentServiceUnderTest.setLocation(environment, location, EnvironmentTestData.getCloudRegions());
        assertEquals("Here", environment.getLocationDisplayName());
    }

    @Test
    public void setLocationByLocation() {
        LocationDto location = new LocationDto("r3", "Somewhere else", 1.2, 1.3);
        environmentServiceUnderTest.setLocation(environment, location, EnvironmentTestData.getCloudRegions());
        assertEquals("Somewhere else", environment.getLocationDisplayName());
    }

    @Test
    public void setLocationLocationCoordinatesAreInvalid() {
        LocationDto location = new LocationDto("r3", "Somewhere else", null, 1.3);
        assertThrows(BadRequestException.class, () -> environmentServiceUnderTest.setLocation(environment, location, EnvironmentTestData.getCloudRegions()));
    }

    @Test
    public void findAllByIdInAndStatusIn() {
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
    public void findAllByStatusIn() {
        when(environmentDtoConverter.environmentToDto(eq(environment))).thenReturn(environmentDto);
        when(environmentRepository
                .findAllByStatusInAndArchivedIsFalse(eq(Set.of(EnvironmentStatus.AVAILABLE, EnvironmentStatus.CREATION_INITIATED))))
                .thenReturn(List.of(environment));
        assertEquals(List.of(environmentDto), environmentServiceUnderTest
                .findAllByStatusIn(Set.of(EnvironmentStatus.AVAILABLE, EnvironmentStatus.CREATION_INITIATED)));

    }

    @Test
    void testGetCreateAWSEnvironmentFromEnvironmentRequestForCli() {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        CreateAWSEnvironmentRequest createAWSEnvironmentRequest = new CreateAWSEnvironmentRequest();
        when(environmentValidatorService.validateAwsEnvironmentRequest(eq(environmentRequest), anyString())).thenReturn(new ValidationResultBuilder().build());
        when(environmentRequestToCreateAWSEnvironmentRequestConverter.convert(environmentRequest)).thenReturn(createAWSEnvironmentRequest);
        CreateAWSEnvironmentRequest result = environmentServiceUnderTest.getCreateAWSEnvironmentForCli(environmentRequest, "platform");
        assertEquals(createAWSEnvironmentRequest, result);
    }

    @Test
    void testGetCreateAWSEnvironmentFromEnvironmentRequestForCliHasErrors() {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        when(environmentValidatorService.validateAwsEnvironmentRequest(eq(environmentRequest), anyString())).thenReturn(
                new ValidationResultBuilder().error("error").build());
        assertThrows(BadRequestException.class, () -> environmentServiceUnderTest.getCreateAWSEnvironmentForCli(environmentRequest, "platform"));
    }

    @Test
    void testGetCreateAWSEnvironmentFromEnvironmentDtoForCli() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        CreateAWSEnvironmentRequest createAWSEnvironmentRequest = new CreateAWSEnvironmentRequest();
        when(environmentValidatorService.validateAwsEnvironmentRequest(environmentDto)).thenReturn(new ValidationResultBuilder().build());
        when(environmentDtoToCreateAWSEnvironmentRequestConverter.convert(environmentDto)).thenReturn(createAWSEnvironmentRequest);
        CreateAWSEnvironmentRequest result = environmentServiceUnderTest.getCreateAWSEnvironmentForCli(environmentDto);
        assertEquals(createAWSEnvironmentRequest, result);
    }

    @Test
    void testGetCreateAWSEnvironmentFromEnvironmentDtoForCliHasErrors() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        when(environmentValidatorService.validateAwsEnvironmentRequest(environmentDto)).thenReturn(new ValidationResultBuilder().error("error").build());
        assertThrows(BadRequestException.class, () -> environmentServiceUnderTest.getCreateAWSEnvironmentForCli(environmentDto));
    }

    @Configuration
    @Import(EnvironmentService.class)
    static class Config {

    }

}
