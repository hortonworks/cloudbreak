package com.sequenceiq.environment.environment.service;


import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ACCOUNT_ID;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.USER;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.getCloudRegions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

@ExtendWith(SpringExtension.class)
class EnvironmentCreationServiceTest {

    @MockBean
    private EnvironmentService environmentService;

    @MockBean
    private EnvironmentValidatorService validatorService;

    @MockBean
    private EnvironmentResourceService environmentResourceService;

    @MockBean
    private EnvironmentDtoConverter environmentDtoConverter;

    @MockBean
    private EnvironmentReactorFlowManager reactorFlowManager;

    @MockBean
    private AuthenticationDtoConverter authenticationDtoConverter;

    @MockBean
    private ParametersService parametersService;

    @MockBean
    private NetworkService networkService;

    @Inject
    private EnvironmentCreationService environmentCreationServiceUnderTest;

    @Test
    void testCreateOccupied() {
        EnvironmentCreationDto environmentCreationDto = new EnvironmentCreationDto.Builder()
                .withName(ENVIRONMENT_NAME)
                .withAccountId(ACCOUNT_ID)
                .build();

        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(true);
        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto, ACCOUNT_ID,
                USER));
        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any());
        verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    @Test
    void testCreate() {
        ParametersDto parametersDto = ParametersDto.builder().withAwsParameters(AwsParametersDto.builder().withDynamoDbTableName("dynamo").build()).build();
        final EnvironmentCreationDto environmentCreationDto = new EnvironmentCreationDto.Builder()
                .withName(ENVIRONMENT_NAME)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .withParameters(parametersDto)
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");

        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID), eq(EnvironmentTestData.USER)))
                .thenReturn(credential);
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(validatorService.validateCreation(any(), any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateTelemetryLoggingStorageLocation(eq(EnvironmentTestData.USER), any())).thenReturn(ValidationResult.builder().build());
        when(environmentService.save(any())).thenReturn(environment);
        environmentCreationServiceUnderTest
                .create(environmentCreationDto, ACCOUNT_ID, EnvironmentTestData.USER);
        verify(environmentService, times(2)).save(any());
        verify(parametersService).saveParameters(eq(environment), eq(parametersDto), eq(ACCOUNT_ID));
        verify(environmentResourceService).createAndSetNetwork(any(), any(), any(), any());
        verify(reactorFlowManager).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    @Test
    void testCreateVerificationError() {
        final EnvironmentCreationDto environmentCreationDto = new EnvironmentCreationDto.Builder()
                .withName(ENVIRONMENT_NAME)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");

        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID), eq(EnvironmentTestData.USER)))
                .thenReturn(credential);
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(validatorService.validateCreation(any(), any(), any())).thenReturn(ValidationResult.builder().error("nogood").build());
        when(validatorService.validateTelemetryLoggingStorageLocation(eq(EnvironmentTestData.USER), any())).thenReturn(ValidationResult.builder().build());
        when(environmentService.save(any())).thenReturn(environment);
        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto, ACCOUNT_ID,
                EnvironmentTestData.USER));
        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any());
        verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    @Configuration
    @Import(EnvironmentCreationService.class)
    static class Config {
    }
}
