package com.sequenceiq.environment.environment.service;


import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.CRN;
import static com.sequenceiq.cloudbreak.util.TestConstants.USER;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.getCloudRegions;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

import java.util.Optional;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "environment.tunnel.ccm.validate.entitlement=true",
})
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
    private EntitlementService entitlementService;

    @MockBean
    private NetworkService networkService;

    @Mock
    private ValidationResultBuilder validationResult;

    @MockBean
    private GrpcUmsClient grpcUmsClient;

    @MockBean
    private VirtualGroupService virtualGroupService;

    @Inject
    private EnvironmentCreationService environmentCreationServiceUnderTest;

    @Test
    void testCreateOccupied() {
        EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withAccountId(ACCOUNT_ID)
                .build();
        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(true);
        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));

        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any());
        verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    @Test
    void testCreateAzureDisabled() {
        ParametersDto parametersDto = ParametersDto.builder().withAwsParameters(AwsParametersDto.builder().withDynamoDbTableName("dynamo").build()).build();
        final EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform("AZURE")
                .withCreator(CRN)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .withParameters(parametersDto)
                .withLocation(LocationDto.builder()
                        .withName("test")
                        .withDisplayName("test")
                        .withLatitude(0.1)
                        .withLongitude(0.1)
                        .build())
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        Credential credential = new Credential();
        credential.setCloudPlatform("AZURE");
        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), any(), any()))
                .thenReturn(credential);
        when(validatorService.validateRegionsAndLocation(any(), any(), any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateNetworkCreation(any(), any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(environmentService.save(any())).thenReturn(environment);
        when(entitlementService.azureEnabled(eq(CRN), eq(ACCOUNT_ID))).thenReturn(false);
        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));

        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any());
        verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    @Test
    void testCreate() {
        ParametersDto parametersDto = ParametersDto.builder().withAwsParameters(AwsParametersDto.builder().withDynamoDbTableName("dynamo").build()).build();
        final EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withCreator(CRN)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .withParameters(parametersDto)
                .withLocation(LocationDto.builder()
                        .withName("test")
                        .withDisplayName("test")
                        .withLatitude(0.1)
                        .withLongitude(0.1)
                        .build())
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");
        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID), eq(CRN)))
                .thenReturn(credential);
        when(validatorService.validateRegionsAndLocation(any(), any(), any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateNetworkCreation(any(), any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(environmentService.save(any())).thenReturn(environment);

        environmentCreationServiceUnderTest.create(environmentCreationDto);

        verify(environmentService, times(2)).save(any());
        verify(parametersService).saveParameters(eq(environment), eq(parametersDto));
        verify(environmentResourceService).createAndSetNetwork(any(), any(), any(), any());
        verify(reactorFlowManager).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(CRN), anyString());
    }

    @Test
    void testCreateWithParentEnvironment() {
        ParametersDto parametersDto = ParametersDto.builder().withAwsParameters(AwsParametersDto.builder().withDynamoDbTableName("dynamo").build()).build();
        final EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withCreator(CRN)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .withParameters(parametersDto)
                .withLocation(LocationDto.builder()
                        .withName("test")
                        .withDisplayName("test")
                        .withLatitude(0.1)
                        .withLongitude(0.1)
                        .build())
                .withParentEnvironmentName("parentCrn")
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        final Environment parentEnvironment = new Environment();
        parentEnvironment.setName(ENVIRONMENT_NAME);
        parentEnvironment.setId(2L);
        parentEnvironment.setAccountId(ACCOUNT_ID);

        Credential credential = new Credential();
        credential.setCloudPlatform("platform");

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);

        when(environmentService.findByNameAndAccountIdAndArchivedIsFalse(any(), eq(ACCOUNT_ID))).thenReturn(Optional.of(parentEnvironment));
        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID), eq(CRN)))
                .thenReturn(credential);
        when(validatorService.validateRegionsAndLocation(any(), any(), any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateNetworkCreation(any(), any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(environmentService.save(environmentArgumentCaptor.capture())).thenReturn(environment);

        environmentCreationServiceUnderTest.create(environmentCreationDto);

        verify(environmentService, times(2)).save(any());
        verify(parametersService).saveParameters(eq(environment), eq(parametersDto));
        verify(environmentResourceService).createAndSetNetwork(any(), any(), any(), any());
        verify(reactorFlowManager).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(CRN), anyString());
        assertEquals(environmentArgumentCaptor.getValue().getParentEnvironment(), parentEnvironment);
    }

    @Test
    void testCreationVerificationError() {
        ParametersDto parametersDto = ParametersDto.builder().withAwsParameters(AwsParametersDto.builder().withDynamoDbTableName("dynamo").build()).build();
        final EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .withCreator(CRN)
                .withAccountId(ACCOUNT_ID)
                .withParameters(parametersDto)
                .withLocation(LocationDto.builder()
                        .withName("test")
                        .withDisplayName("test")
                        .withLatitude(0.1)
                        .withLongitude(0.1)
                        .build())
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");
        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID), eq(CRN)))
                .thenReturn(credential);
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(validatorService.validateRegionsAndLocation(any(), any(), any(), any())).thenReturn(validationResult);
        when(validatorService.validateNetworkCreation(any(), any(), any())).thenReturn(validationResult);
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validationResult.merge(any())).thenReturn(ValidationResult.builder().error("nogood"));
        when(environmentService.save(any())).thenReturn(environment);

        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));

        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any());
        verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    @Test
    void testParameterVerificationError() {
        ParametersDto parametersDto = ParametersDto.builder().withAwsParameters(AwsParametersDto.builder().withDynamoDbTableName("dynamo").build()).build();
        final EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .withCreator(CRN)
                .withAccountId(ACCOUNT_ID)
                .withParameters(parametersDto)
                .withLocation(LocationDto.builder()
                        .withName("test")
                        .withDisplayName("test")
                        .withLatitude(0.1)
                        .withLongitude(0.1)
                        .build())
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        Credential credential = new Credential();
        credential.setCloudPlatform("platform");
        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID), eq(CRN)))
                .thenReturn(credential);
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(environmentDtoConverter.environmentToLocationDto(any())).thenReturn(LocationDto.builder().withName("loc").build());
        when(validatorService.validateRegionsAndLocation(any(), any(), any(), any())).thenReturn(validationResult);
        when(validatorService.validateNetworkCreation(any(), any(), any())).thenReturn(validationResult);
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validationResult.merge(any())).thenReturn(ValidationResult.builder().error("nogood"));
        when(environmentService.save(any())).thenReturn(environment);

        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));

        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any());
        verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    @Configuration
    @Import(EnvironmentCreationService.class)
    static class Config {
    }
}
