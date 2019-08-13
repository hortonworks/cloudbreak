package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ACCOUNT_ID;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.CRN;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

@ExtendWith(SpringExtension.class)
class EnvironmentModificationServiceTest {

    private static final String USER_CRN = "USER_CRN";

    @Inject
    private EnvironmentModificationService environmentModificationServiceUnderTest;

    @MockBean
    private EnvironmentDtoConverter environmentDtoConverter;

    @MockBean
    private EnvironmentRepository environmentRepository;

    @MockBean
    private EnvironmentService environmentService;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private NetworkService networkService;

    @MockBean
    private AuthenticationDtoConverter authenticationDtoConverter;

    @MockBean
    private ParametersService parametersService;

    @Mock
    private EnvironmentValidatorService validatorService;

    @Mock
    private ValidationResult.ValidationResultBuilder validationResultBuilder;

    @Mock
    private ValidationResult validationResult;

    @Test
    public void editByName() {
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .build();
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto);
        verify(environmentRepository).save(any());
    }

    @Test
    public void editByNameDescriptionChange() {
        final String description = "test";
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .withDescription(description)
                .build();
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        assertEquals(description, environmentArgumentCaptor.getValue().getDescription());
    }

    @Test
    public void editByNameRegionChange() {
        final String description = "test";
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .withRegions(Set.of("r1"))
                .build();
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateRegions(any(), any(), any(), any())).thenReturn(validationResultBuilder);
        when(validationResultBuilder.build()).thenReturn(validationResult);
        environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        verify(environmentService).setRegions(any(), any(), any());
        verify(environmentService, never()).setLocation(any(), any(), any());
    }

    @Test
    public void editByNameRegionAndLocationChange() {
        final String description = "test";
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .withRegions(Set.of("r1"))
                .withLocation(LocationDto.LocationDtoBuilder.aLocationDto()
                        .withName("test")
                        .withDisplayName("test")
                        .withLatitude(0.1)
                        .withLongitude(0.1)
                        .build())
                .build();
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateRegions(any(), any(), any(), any())).thenReturn(validationResultBuilder);
        when(validationResultBuilder.build()).thenReturn(validationResult);
        environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        verify(environmentService).setRegions(any(), any(), any());
        verify(environmentService).setLocation(any(), any(), any());
    }

    @Test
    public void editByNameRegionAndLocationChangeValidationError() {
        final String description = "test";
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .withRegions(Set.of("r1"))
                .withLocation(LocationDto.LocationDtoBuilder.aLocationDto()
                        .withName("test")
                        .withDisplayName("test")
                        .withLatitude(0.1)
                        .withLongitude(0.1)
                        .build())
                .build();
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateRegions(any(), any(), any(), any())).thenReturn(validationResultBuilder);
        when(validationResultBuilder.build()).thenReturn(validationResult);
        when(validationResult.hasError()).thenReturn(Boolean.TRUE);
        assertThrows(BadRequestException.class,
                () -> environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto));

        verify(environmentRepository, never()).save(any());
        verify(environmentService, never()).setRegions(any(), any(), any());
        verify(environmentService, never()).setLocation(any(), any(), any());
    }

    @Test
    public void editByNameLocationChange() {
        final String description = "test";
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .withLocation(LocationDto.LocationDtoBuilder.aLocationDto()
                        .withName("test")
                        .withDisplayName("test")
                        .withLatitude(0.1)
                        .withLongitude(0.1)
                        .build())
                .build();
        Environment value = new Environment();
        Region region = new Region();
        region.setName("r3");
        value.setRegions(Set.of(region));
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateRegions(any(), any(), any(), any())).thenReturn(validationResultBuilder);
        when(validationResultBuilder.build()).thenReturn(validationResult);
        environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        verify(environmentService, never()).setRegions(any(), any(), any());
        verify(environmentService).setLocation(any(), any(), any());
    }

    @Test
    public void editByNameNetworkChange() {
        final String description = "test";
        NetworkDto network = NetworkDto.Builder.aNetworkDto().build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .withNetwork(network)
                .build();
        Environment value = new Environment();
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        when(networkService.findByEnvironment(any())).thenReturn(Optional.empty());
        when(networkService.saveNetwork(any(), any(), anyString(), any())).thenReturn(new AwsNetwork());
        environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        verify(networkService).saveNetwork(any(), any(), any(), any());
    }

    @Test
    public void editByNameAuthenticationChange() {
        final String description = "test";
        final EnvironmentAuthentication envAuthResult = new EnvironmentAuthentication();
        AuthenticationDto authentication = AuthenticationDto.builder().build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(authentication)
                .build();
        Environment value = new Environment();
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(envAuthResult);
        environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        assertEquals(envAuthResult, environmentArgumentCaptor.getValue().getAuthentication());
    }

    @Test
    public void editByNameSecurityAccessChange() {
        final String description = "test";
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder().withCidr("test").build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .withSecurityAccess(securityAccessDto)
                .build();
        Environment value = new Environment();
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        verify(environmentService).setSecurityAccess(eq(value), eq(securityAccessDto));
    }

    @Test
    public void editByCrn() {
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .build();
        when(environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(CRN), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        environmentModificationServiceUnderTest.editByCrn(USER_CRN, CRN, environmentDto);
        verify(environmentRepository).save(any());
    }

    @Test
    public void editByNameParameters() {
        String dynamotable = "dynamotable";
        ParametersDto parameters = ParametersDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withAwsParameters(AwsParametersDto.builder()
                        .withDynamoDbTableName(dynamotable)
                        .build())
                .build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withAccountId(ACCOUNT_ID)
                .withParameters(parameters)
                .build();
        Environment environment = new Environment();
        BaseParameters baseParameters = new AwsParameters();

        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(environment));
        when(parametersService.saveParameters(environment, parameters, ACCOUNT_ID)).thenReturn(baseParameters);

        environmentModificationServiceUnderTest.editByName(USER_CRN, ENVIRONMENT_NAME, environmentDto);

        verify(parametersService).saveParameters(environment, parameters, ACCOUNT_ID);
        assertEquals(baseParameters, environment.getParameters());
    }

    @Test
    public void changeCredentialByEnvironmentName() {
        String credentialName = "credentialName";
        final Credential value = new Credential();
        EnvironmentChangeCredentialDto environmentChangeDto = EnvironmentChangeCredentialDto.EnvironmentChangeCredentialDtoBuilder
                .anEnvironmentChangeCredentialDto()
                .withCredentialName(credentialName)
                .build();
        when(environmentRepository
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        when(credentialService.getByNameForAccountId(eq(credentialName), eq(ACCOUNT_ID))).thenReturn(value);
        environmentModificationServiceUnderTest.changeCredentialByEnvironmentName(ACCOUNT_ID, ENVIRONMENT_NAME, environmentChangeDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        assertEquals(value, environmentArgumentCaptor.getValue().getCredential());
    }

    @Test
    public void changeCredentialByEnvironmentCrn() {
        String credentialName = "credentialName";
        final Credential value = new Credential();
        EnvironmentChangeCredentialDto environmentChangeDto = EnvironmentChangeCredentialDto.EnvironmentChangeCredentialDtoBuilder
                .anEnvironmentChangeCredentialDto()
                .withCredentialName(credentialName)
                .build();
        when(environmentRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(CRN), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        when(credentialService.getByNameForAccountId(eq(credentialName), eq(ACCOUNT_ID))).thenReturn(value);

        environmentModificationServiceUnderTest.changeCredentialByEnvironmentCrn(ACCOUNT_ID, CRN, environmentChangeDto);
        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentRepository).save(environmentArgumentCaptor.capture());
        assertEquals(value, environmentArgumentCaptor.getValue().getCredential());
    }

    @Configuration
    @Import(EnvironmentModificationService.class)
    static class Config {
    }
}
