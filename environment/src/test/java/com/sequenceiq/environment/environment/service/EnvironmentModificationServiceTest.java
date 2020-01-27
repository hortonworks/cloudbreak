package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.CRN;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
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

    @MockBean
    private EnvironmentFlowValidatorService environmentFlowValidatorService;

    @Mock
    private EnvironmentValidatorService validatorService;

    @Mock
    private ValidationResult.ValidationResultBuilder validationResultBuilder;

    @Mock
    private ValidationResult validationResult;

    @Test
    void editByName() {
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .build();
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto);
        verify(environmentService).save(any());
    }

    @Test
    void editByNameDescriptionChange() {
        final String description = "test";
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withDescription(description)
                .build();
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
        assertEquals(description, environmentArgumentCaptor.getValue().getDescription());
    }

    @Test
    void editByNameNetworkChange() {
        NetworkDto network = NetworkDto.builder().build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withNetwork(network)
                .build();
        Environment value = new Environment();
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        when(networkService.findByEnvironment(any())).thenReturn(Optional.empty());
        when(networkService.saveNetwork(any(), any(), anyString(), any())).thenReturn(new AwsNetwork());

        environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
    }

    @Test
    void editByNameAuthenticationChange() {
        final EnvironmentAuthentication envAuthResult = new EnvironmentAuthentication();
        AuthenticationDto authentication = AuthenticationDto.builder().build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(authentication)
                .build();
        Environment value = new Environment();
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(envAuthResult);

        environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
        assertEquals(envAuthResult, environmentArgumentCaptor.getValue().getAuthentication());
    }

    @Test
    void editByNameSecurityAccessChange() {
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder().withCidr("test").build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withSecurityAccess(securityAccessDto)
                .build();
        Environment value = new Environment();
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateSecurityAccessModification(any(), any())).thenReturn(validationResult);
        when(validatorService.validateSecurityGroups(any(), any())).thenReturn(validationResult);

        environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
        verify(environmentService).editSecurityAccess(eq(value), eq(securityAccessDto));
    }

    @Test
    void editByNameSecurityAccessChangeHasSecurityAccessError() {
        ValidationResult validationResultError = ValidationResult.builder().error("sec access error").build();
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder().withCidr("test").build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withSecurityAccess(securityAccessDto)
                .build();
        Environment value = new Environment();
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateSecurityAccessModification(any(), any())).thenReturn(validationResultError);

        BadRequestException actual = assertThrows(BadRequestException.class,
                () -> environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto));

        assertEquals("1. sec access error", actual.getMessage());
        verify(environmentService, times(0)).editSecurityAccess(eq(value), eq(securityAccessDto));
    }

    @Test
    void editByNameSecurityAccessChangeHasSecurityGroupsError() {
        ValidationResult validationResultError = ValidationResult.builder().error("sec group error").build();
        SecurityAccessDto securityAccessDto = SecurityAccessDto.builder().withCidr("test").build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withSecurityAccess(securityAccessDto)
                .build();
        Environment value = new Environment();
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateSecurityAccessModification(any(), any())).thenReturn(validationResult);
        when(validatorService.validateSecurityGroups(any(), any())).thenReturn(validationResultError);

        BadRequestException actual = assertThrows(BadRequestException.class,
                () -> environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto));

        assertEquals("1. sec group error", actual.getMessage());
        verify(environmentService, times(0)).editSecurityAccess(eq(value), eq(securityAccessDto));
    }

    @Test
    void editByCrn() {
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .build();
        when(environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(CRN), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));

        environmentModificationServiceUnderTest.editByCrn(CRN, environmentDto);
        verify(environmentService).save(any());
    }

    @Test
    void editByNameParametersNotExisted() {
        String dynamotable = "dynamotable";
        ParametersDto parameters = ParametersDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withAwsParameters(AwsParametersDto.builder()
                        .withDynamoDbTableName(dynamotable)
                        .build())
                .build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withParameters(parameters)
                .build();
        Environment environment = new Environment();
        environment.setAccountId(ACCOUNT_ID);
        BaseParameters baseParameters = new AwsParameters();

        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(environment));
        when(parametersService.saveParameters(environment, parameters)).thenReturn(baseParameters);

        environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto);

        verify(parametersService).saveParameters(environment, parameters);
        assertEquals(baseParameters, environment.getParameters());
    }

    @Test
    void editByNameParametersExistedAndValid() {
        String dynamotable = "dynamotable";
        ParametersDto parameters = ParametersDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withAwsParameters(AwsParametersDto.builder()
                        .withDynamoDbTableName(dynamotable)
                        .build())
                .build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withParameters(parameters)
                .build();
        Environment environment = new Environment();
        environment.setAccountId(ACCOUNT_ID);
        AwsParameters awsParameters = new AwsParameters();
        awsParameters.setS3guardTableName("existingTable");
        BaseParameters baseParameters = awsParameters;
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(environmentFlowValidatorService.processAwsParameters(any(), any())).thenReturn(validationResult);
        when(validationResult.hasError()).thenReturn(false);
        when(parametersService.findByEnvironment(any())).thenReturn(Optional.of(baseParameters));
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(environment));
        when(parametersService.saveParameters(environment, parameters)).thenReturn(baseParameters);

        environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto);

        verify(parametersService).saveParameters(environment, parameters);
        assertEquals(baseParameters, environment.getParameters());
    }

    @Test
    void editByNameParametersExistedAndNotValid() {
        String dynamotable = "dynamotable";
        ParametersDto parameters = ParametersDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withAwsParameters(AwsParametersDto.builder()
                        .withDynamoDbTableName(dynamotable)
                        .build())
                .build();
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withParameters(parameters)
                .build();
        Environment environment = new Environment();
        environment.setAccountId(ACCOUNT_ID);
        AwsParameters awsParameters = new AwsParameters();
        awsParameters.setS3guardTableName("existingTable");
        BaseParameters baseParameters = awsParameters;
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(environmentFlowValidatorService.processAwsParameters(any(), any())).thenReturn(validationResult);
        when(validationResult.hasError()).thenReturn(true);
        when(parametersService.findByEnvironment(any())).thenReturn(Optional.of(baseParameters));
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(environment));
        when(parametersService.saveParameters(environment, parameters)).thenReturn(baseParameters);

        assertThrows(BadRequestException.class, () -> environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto));

        verify(parametersService, never()).saveParameters(environment, parameters);
    }

    @Test
    void changeCredentialByEnvironmentName() {
        String credentialName = "credentialName";
        final Credential value = new Credential();
        EnvironmentChangeCredentialDto environmentChangeDto = EnvironmentChangeCredentialDto.EnvironmentChangeCredentialDtoBuilder
                .anEnvironmentChangeCredentialDto()
                .withCredentialName(credentialName)
                .build();
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        when(credentialService.getByNameForAccountId(eq(credentialName), eq(ACCOUNT_ID))).thenReturn(value);

        environmentModificationServiceUnderTest.changeCredentialByEnvironmentName(ACCOUNT_ID, ENVIRONMENT_NAME, environmentChangeDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
        assertEquals(value, environmentArgumentCaptor.getValue().getCredential());
    }

    @Test
    void changeCredentialByEnvironmentCrn() {
        String credentialName = "credentialName";
        final Credential value = new Credential();
        EnvironmentChangeCredentialDto environmentChangeDto = EnvironmentChangeCredentialDto.EnvironmentChangeCredentialDtoBuilder
                .anEnvironmentChangeCredentialDto()
                .withCredentialName(credentialName)
                .build();
        when(environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(CRN), eq(ACCOUNT_ID))).thenReturn(Optional.of(new Environment()));
        when(credentialService.getByNameForAccountId(eq(credentialName), eq(ACCOUNT_ID))).thenReturn(value);

        environmentModificationServiceUnderTest.changeCredentialByEnvironmentCrn(ACCOUNT_ID, CRN, environmentChangeDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
        assertEquals(value, environmentArgumentCaptor.getValue().getCredential());
    }

    @Configuration
    @Import(EnvironmentModificationService.class)
    static class Config {
    }
}
