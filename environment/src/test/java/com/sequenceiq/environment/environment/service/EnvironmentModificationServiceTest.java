package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.CRN;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.UpdateAzureResourceEncryptionDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.encryption.EnvironmentEncryptionService;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.AzureParameters;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.repository.AzureParametersRepository;
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

    @MockBean
    private EnvironmentResourceService environmentResourceService;

    @MockBean
    private EnvironmentEncryptionService environmentEncryptionService;

    @MockBean
    private AzureParametersRepository azureParametersRepository;

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
        when(networkService.saveNetwork(any(), any(), anyString(), any(), any())).thenReturn(new AwsNetwork());

        environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
    }

    @Test
    void editByNameAuthenticationChange() {
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .build();
        Environment value = new Environment();
        EnvironmentDto environmentDto = new EnvironmentDto();

        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));
        when(environmentDtoConverter.environmentToDto(value)).thenReturn(environmentDto);
        when(environmentService.save(value)).thenReturn(value);

        EnvironmentDto actual = environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentEditDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
        assertEquals(actual, environmentDto);
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

        assertEquals("sec access error", actual.getMessage());
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

        assertEquals("sec group error", actual.getMessage());
        verify(environmentService, times(0)).editSecurityAccess(eq(value), eq(securityAccessDto));
    }

    @Test
    void editByNameAwsFreeIpaSpotPercentageIsNotModified() {
        EnvironmentEditDto environmentDto = EnvironmentEditDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withParameters(ParametersDto.builder()
                        .withAwsParameters(AwsParametersDto.builder()
                                .withFreeIpaSpotPercentage(50)
                                .build())
                        .build())
                .build();
        Environment value = new Environment();
        AwsParameters awsParameters = new AwsParameters();
        int originalFreeIpaSpotPercentage = 100;
        awsParameters.setFreeIpaSpotPercentage(originalFreeIpaSpotPercentage);
        value.setParameters(awsParameters);
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(value));

        environmentModificationServiceUnderTest.editByName(ENVIRONMENT_NAME, environmentDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
        Environment result = environmentArgumentCaptor.getValue();

        AwsParameters newAwsParameters = (AwsParameters) result.getParameters();
        assertEquals(originalFreeIpaSpotPercentage, newAwsParameters.getFreeIpaSpotPercentage());
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
        when(environmentFlowValidatorService.validateParameters(any(), any())).thenReturn(validationResult);
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
        when(environmentFlowValidatorService.validateParameters(any(), any())).thenReturn(validationResult);
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
        when(credentialService.getByNameForAccountId(eq(credentialName), eq(ACCOUNT_ID), eq(ENVIRONMENT))).thenReturn(value);

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
        when(credentialService.getByNameForAccountId(eq(credentialName), eq(ACCOUNT_ID), eq(ENVIRONMENT))).thenReturn(value);

        environmentModificationServiceUnderTest.changeCredentialByEnvironmentCrn(ACCOUNT_ID, CRN, environmentChangeDto);

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);
        verify(environmentService).save(environmentArgumentCaptor.capture());
        assertEquals(value, environmentArgumentCaptor.getValue().getCredential());
    }

    @Test
    void testEditAuthenticationIfChangedWhenHasValidationError() {
        AuthenticationDto authenticationDto = AuthenticationDto.builder().build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withAuthentication(authenticationDto).build();
        Environment environment = new Environment();

        ValidationResult validationResult = ValidationResult.builder().error("Error").build();

        when(environmentResourceService.isExistingSshKeyUpdateSupported(environment)).thenReturn(true);
        when(environmentResourceService.isRawSshKeyUpdateSupported(environment)).thenReturn(false);
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateAuthenticationModification(environmentEditDto, environment)).thenReturn(validationResult);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> environmentModificationServiceUnderTest.editAuthenticationIfChanged(environmentEditDto, environment));

        assertEquals(badRequestException.getMessage(), "Error");
    }

    @Test
    void testEditAuthenticationIfChangedWhenNeedToCreateSshKey() {
        AuthenticationDto authenticationDto = AuthenticationDto.builder()
                .withPublicKey("ssh-key")
                .build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withAuthentication(authenticationDto).build();
        Environment environment = new Environment();
        EnvironmentAuthentication originalEnvironmentAuthentication = new EnvironmentAuthentication();
        originalEnvironmentAuthentication.setPublicKey("original-ssh-key");
        originalEnvironmentAuthentication.setManagedKey(false);
        environment.setAuthentication(originalEnvironmentAuthentication);

        EnvironmentAuthentication newEnvironmentAuthentication = new EnvironmentAuthentication();
        newEnvironmentAuthentication.setPublicKey("new-ssh-key");

        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateAuthenticationModification(environmentEditDto, environment)).thenReturn(validationResult);
        when(authenticationDtoConverter.dtoToAuthentication(authenticationDto)).thenReturn(newEnvironmentAuthentication);
        when(environmentResourceService.isExistingSshKeyUpdateSupported(environment)).thenReturn(true);
        when(environmentResourceService.isRawSshKeyUpdateSupported(environment)).thenReturn(false);
        when(environmentResourceService.createAndUpdateSshKey(environment)).thenReturn(true);

        environmentModificationServiceUnderTest.editAuthenticationIfChanged(environmentEditDto, environment);

        verify(environmentResourceService, times(1)).createAndUpdateSshKey(environment);
        verify(environmentResourceService, times(0)).deletePublicKey(environment, "old-public-key-id");
        assertEquals(environment.getAuthentication().getPublicKey(), "new-ssh-key");
    }

    @Test
    void testEditAuthenticationIfChangedWhenNeedToCreateSshKeyAndDeleteOldOne() {
        AuthenticationDto authenticationDto = AuthenticationDto.builder()
                .withPublicKey("ssh-key")
                .build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withAuthentication(authenticationDto).build();
        Environment environment = new Environment();
        EnvironmentAuthentication originalEnvironmentAuthentication = new EnvironmentAuthentication();
        originalEnvironmentAuthentication.setPublicKey("original-ssh-key");
        originalEnvironmentAuthentication.setManagedKey(true);
        originalEnvironmentAuthentication.setPublicKeyId("old-public-key-id");

        environment.setAuthentication(originalEnvironmentAuthentication);

        EnvironmentAuthentication newEnvironmentAuthentication = new EnvironmentAuthentication();
        newEnvironmentAuthentication.setPublicKey("new-ssh-key");

        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateAuthenticationModification(environmentEditDto, environment)).thenReturn(validationResult);
        when(authenticationDtoConverter.dtoToAuthentication(authenticationDto)).thenReturn(newEnvironmentAuthentication);
        when(environmentResourceService.isExistingSshKeyUpdateSupported(environment)).thenReturn(true);
        when(environmentResourceService.isRawSshKeyUpdateSupported(environment)).thenReturn(false);
        when(environmentResourceService.createAndUpdateSshKey(environment)).thenReturn(true);

        environmentModificationServiceUnderTest.editAuthenticationIfChanged(environmentEditDto, environment);

        verify(environmentResourceService, times(1)).createAndUpdateSshKey(environment);
        verify(environmentResourceService, times(1)).deletePublicKey(environment, "old-public-key-id");
        assertEquals(environment.getAuthentication().getPublicKey(), "new-ssh-key");
    }

    @Test
    void testEditAuthenticationIfChangedWhenNeedToDeleteOldKey() {
        AuthenticationDto authenticationDto = AuthenticationDto.builder().build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withAuthentication(authenticationDto).build();

        Environment environment = new Environment();
        EnvironmentAuthentication originalEnvironmentAuthentication = new EnvironmentAuthentication();
        originalEnvironmentAuthentication.setManagedKey(true);
        originalEnvironmentAuthentication.setPublicKeyId("old-public-key-id");
        environment.setAuthentication(originalEnvironmentAuthentication);

        EnvironmentAuthentication newEnvironmentAuthentication = new EnvironmentAuthentication();

        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(environmentResourceService.isExistingSshKeyUpdateSupported(environment)).thenReturn(true);
        when(environmentResourceService.isRawSshKeyUpdateSupported(environment)).thenReturn(false);
        when(validatorService.validateAuthenticationModification(environmentEditDto, environment)).thenReturn(validationResult);
        when(authenticationDtoConverter.dtoToAuthentication(authenticationDto)).thenReturn(newEnvironmentAuthentication);

        environmentModificationServiceUnderTest.editAuthenticationIfChanged(environmentEditDto, environment);

        verify(environmentResourceService, times(1)).deletePublicKey(environment, "old-public-key-id");
        verify(environmentResourceService, times(0)).createAndUpdateSshKey(environment);
    }

    @Test
    void testEditAuthenticationIfChangedWhenDontDeleteOldKey() {
        AuthenticationDto authenticationDto = AuthenticationDto.builder().build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withAuthentication(authenticationDto).build();

        Environment environment = new Environment();
        EnvironmentAuthentication originalEnvironmentAuthentication = new EnvironmentAuthentication();
        originalEnvironmentAuthentication.setManagedKey(false);
        originalEnvironmentAuthentication.setPublicKeyId("old-public-key-id");
        environment.setAuthentication(originalEnvironmentAuthentication);

        EnvironmentAuthentication newEnvironmentAuthentication = new EnvironmentAuthentication();

        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateAuthenticationModification(environmentEditDto, environment)).thenReturn(validationResult);
        when(environmentResourceService.isExistingSshKeyUpdateSupported(environment)).thenReturn(true);
        when(environmentResourceService.isRawSshKeyUpdateSupported(environment)).thenReturn(false);
        when(authenticationDtoConverter.dtoToAuthentication(authenticationDto)).thenReturn(newEnvironmentAuthentication);

        environmentModificationServiceUnderTest.editAuthenticationIfChanged(environmentEditDto, environment);

        verify(environmentResourceService, times(0)).deletePublicKey(environment, "old-public-key-id");
        verify(environmentResourceService, times(0)).createAndUpdateSshKey(environment);
    }

    @Test
    void testEditAuthenticationIfChangedWhenNotCreatedAndRevertToOldOne() {
        AuthenticationDto authenticationDto = AuthenticationDto.builder()
                .withPublicKey("ssh-key")
                .build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withAuthentication(authenticationDto).build();
        Environment environment = new Environment();
        EnvironmentAuthentication originalEnvironmentAuthentication = new EnvironmentAuthentication();
        originalEnvironmentAuthentication.setPublicKey("original-ssh-key");
        originalEnvironmentAuthentication.setManagedKey(false);
        environment.setAuthentication(originalEnvironmentAuthentication);

        EnvironmentAuthentication newEnvironmentAuthentication = new EnvironmentAuthentication();
        newEnvironmentAuthentication.setPublicKey("new-ssh-key");

        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateAuthenticationModification(environmentEditDto, environment)).thenReturn(validationResult);
        when(authenticationDtoConverter.dtoToAuthentication(authenticationDto)).thenReturn(newEnvironmentAuthentication);
        when(environmentResourceService.isExistingSshKeyUpdateSupported(environment)).thenReturn(true);
        when(environmentResourceService.isRawSshKeyUpdateSupported(environment)).thenReturn(false);
        when(environmentResourceService.createAndUpdateSshKey(environment)).thenReturn(false);

        environmentModificationServiceUnderTest.editAuthenticationIfChanged(environmentEditDto, environment);

        verify(environmentResourceService, times(1)).createAndUpdateSshKey(environment);
        verify(environmentResourceService, times(0)).deletePublicKey(environment, "old-public-key-id");
        assertEquals(environment.getAuthentication().getPublicKey(), "original-ssh-key");
    }

    @Test
    void testEditAuthenticationIfChangedWhenNeedToSshKeyUpdateSupportedAndNewSshKeyApplied() {
        AuthenticationDto authenticationDto = AuthenticationDto.builder()
                .withPublicKey("ssh-key")
                .build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withAuthentication(authenticationDto).build();
        Environment environment = new Environment();
        EnvironmentAuthentication originalEnvironmentAuthentication = new EnvironmentAuthentication();
        originalEnvironmentAuthentication.setPublicKey("original-ssh-key");
        originalEnvironmentAuthentication.setManagedKey(false);
        environment.setAuthentication(originalEnvironmentAuthentication);

        EnvironmentAuthentication newEnvironmentAuthentication = new EnvironmentAuthentication();
        newEnvironmentAuthentication.setPublicKey("new-ssh-key");

        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(validatorService.validateAuthenticationModification(environmentEditDto, environment)).thenReturn(validationResult);
        when(authenticationDtoConverter.dtoToAuthentication(authenticationDto)).thenReturn(newEnvironmentAuthentication);
        when(environmentResourceService.isExistingSshKeyUpdateSupported(environment)).thenReturn(false);
        when(environmentResourceService.isRawSshKeyUpdateSupported(environment)).thenReturn(true);
        when(authenticationDtoConverter.dtoToSshUpdatedAuthentication(authenticationDto)).thenReturn(newEnvironmentAuthentication);

        environmentModificationServiceUnderTest.editAuthenticationIfChanged(environmentEditDto, environment);

        assertEquals(environment.getAuthentication().getPublicKey(), "new-ssh-key");
    }

    @Test
    public void testChangeTelemetryFeaturesByEnvironmentName() {
        String accountId = "myAccountId";
        String envName = "myEnvName";
        EnvironmentFeatures featuresInput = new EnvironmentFeatures();
        featuresInput.addClusterLogsCollection(true);
        Environment environment = new Environment();
        environment.setTelemetry(new EnvironmentTelemetry());
        when(environmentService.findByNameAndAccountIdAndArchivedIsFalse(envName, accountId))
                .thenReturn(Optional.of(environment));
        when(environmentService.save(environment)).thenReturn(environment);
        when(environmentDtoConverter.environmentToDto(environment)).thenReturn(new EnvironmentDto());

        environmentModificationServiceUnderTest.changeTelemetryFeaturesByEnvironmentName(accountId, envName, featuresInput);

        verify(environmentService).save(any());
        assertTrue(environment.getTelemetry().getFeatures().getClusterLogsCollection().isEnabled());
    }

    @Test
    public void testChangeTelemetryFeaturesByEnvironmentCrn() {
        String accountId = "myAccountId";
        String envCrn = "myEnvCrn";
        EnvironmentFeatures featuresInput = new EnvironmentFeatures();
        featuresInput.addCloudStorageLogging(false);
        featuresInput.addClusterLogsCollection(true);
        Environment environment = new Environment();
        environment.setTelemetry(new EnvironmentTelemetry());
        when(environmentService.findByResourceCrnAndAccountIdAndArchivedIsFalse(envCrn, accountId))
                .thenReturn(Optional.of(environment));
        when(environmentService.save(environment)).thenReturn(environment);
        when(environmentDtoConverter.environmentToDto(environment)).thenReturn(new EnvironmentDto());

        environmentModificationServiceUnderTest.changeTelemetryFeaturesByEnvironmentCrn(accountId, envCrn, featuresInput);

        verify(environmentService).save(any());
        assertFalse(environment.getTelemetry().getFeatures().getCloudStorageLogging().isEnabled());
        assertTrue(environment.getTelemetry().getFeatures().getClusterLogsCollection().isEnabled());
    }

    @Test
    void testUpdateAzureResourceEncryptionParametersByEnvironmentName() {
        UpdateAzureResourceEncryptionDto updateAzureResourceEncryptionDto = UpdateAzureResourceEncryptionDto.builder()
                .withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.builder()
                        .withEncryptionKeyUrl("dummyKeyUrl")
                        .withEncryptionKeyResourceGroupName("dummyResourceGroupName")
                        .build())
                .build();
        CreatedDiskEncryptionSet createdDiskEncryptionSet = new CreatedDiskEncryptionSet.Builder()
                .withDiskEncryptionSetId("dummyId")
                .build();
        Environment env = new Environment();
        env.setParameters(new AzureParameters());
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(env));
        when(validatorService.validateEncryptionKeyUrl(any(String.class), any(String.class))).thenReturn(ValidationResult.builder().build());
        when(environmentDtoConverter.environmentToDto(env)).thenReturn(new EnvironmentDto());
        when(environmentEncryptionService.createEncryptionResources(any(EnvironmentDto.class))).thenReturn(createdDiskEncryptionSet);

        environmentModificationServiceUnderTest.updateAzureResourceEncryptionParametersByEnvironmentName(ACCOUNT_ID,
                ENVIRONMENT_NAME, updateAzureResourceEncryptionDto);

        ArgumentCaptor<AzureParameters> azureParametersArgumentCaptor = ArgumentCaptor.forClass(AzureParameters.class);
        verify(azureParametersRepository).save(azureParametersArgumentCaptor.capture());
        assertEquals("dummyKeyUrl", azureParametersArgumentCaptor.getValue().getEncryptionKeyUrl());
        assertEquals("dummyResourceGroupName", azureParametersArgumentCaptor.getValue().getEncryptionKeyResourceGroupName());
    }

    @Test
    void testUpdateAzureResourceEncryptionParametersByEnvironmentCrn() {
        UpdateAzureResourceEncryptionDto updateAzureResourceEncryptionDto = UpdateAzureResourceEncryptionDto.builder()
                .withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.builder()
                        .withEncryptionKeyUrl("dummyKeyUrl")
                        .withEncryptionKeyResourceGroupName("dummyResourceGroupName")
                        .build())
                .build();
        CreatedDiskEncryptionSet createdDiskEncryptionSet = new CreatedDiskEncryptionSet.Builder()
                .withDiskEncryptionSetId("dummyId")
                .build();
        Environment env = new Environment();
        env.setParameters(new AzureParameters());
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(env));
        when(validatorService.validateEncryptionKeyUrl(any(String.class), any(String.class))).thenReturn(ValidationResult.builder().build());
        when(environmentDtoConverter.environmentToDto(env)).thenReturn(new EnvironmentDto());
        when(environmentEncryptionService.createEncryptionResources(any(EnvironmentDto.class))).thenReturn(createdDiskEncryptionSet);

        environmentModificationServiceUnderTest.updateAzureResourceEncryptionParametersByEnvironmentCrn(ACCOUNT_ID,
                ENVIRONMENT_NAME, updateAzureResourceEncryptionDto);

        ArgumentCaptor<AzureParameters> azureParametersArgumentCaptor = ArgumentCaptor.forClass(AzureParameters.class);
        verify(azureParametersRepository).save(azureParametersArgumentCaptor.capture());
        assertEquals("dummyKeyUrl", azureParametersArgumentCaptor.getValue().getEncryptionKeyUrl());
        assertEquals("dummyResourceGroupName", azureParametersArgumentCaptor.getValue().getEncryptionKeyResourceGroupName());

    }

    @Test
    void testUpdateAzureResourceEncryptionParametersErrorsWhenEncryptionKeyAlreadyPresent() {
        UpdateAzureResourceEncryptionDto updateAzureResourceEncryptionDto = UpdateAzureResourceEncryptionDto.builder()
                .withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.builder()
                        .withEncryptionKeyUrl("dummyKeyUrl")
                        .withEncryptionKeyResourceGroupName("dummyResourceGroupName")
                        .build())
                .build();
        Environment env = new Environment();
        AzureParameters azureParameters = new AzureParameters();
        azureParameters.setEncryptionKeyUrl("dummyEncryptionKey");
        env.setParameters(azureParameters);

        when(environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(env));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> environmentModificationServiceUnderTest.updateAzureResourceEncryptionParametersByEnvironmentCrn(ACCOUNT_ID,
                        ENVIRONMENT_NAME, updateAzureResourceEncryptionDto));

        assertEquals(badRequestException.getMessage(), String.format("Encryption Key '%s' is already set for the environment '%s'. " +
                "Modifying the encryption key is not allowed.", "dummyEncryptionKey", ENVIRONMENT_NAME));
    }

    @Test
    void testUpdateAzureResourceEncryptionParametersNoErrorWhenSameEncryptionKeyAlreadyPresent() {
        UpdateAzureResourceEncryptionDto updateAzureResourceEncryptionDto = UpdateAzureResourceEncryptionDto.builder()
                .withAzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.builder()
                        .withEncryptionKeyUrl("dummyKeyUrl")
                        .withEncryptionKeyResourceGroupName("dummyResourceGroupName")
                        .build())
                .build();
        Environment env = new Environment();
        AzureParameters azureParameters = new AzureParameters();
        azureParameters.setEncryptionKeyUrl("dummyKeyUrl");
        env.setParameters(azureParameters);
        when(environmentService.getValidatorService()).thenReturn(validatorService);
        when(environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(Optional.of(env));
        when(validatorService.validateEncryptionKeyUrl(any(String.class), any(String.class))).thenReturn(ValidationResult.builder().build());
        when(environmentDtoConverter.environmentToDto(env)).thenReturn(new EnvironmentDto());

        environmentModificationServiceUnderTest.updateAzureResourceEncryptionParametersByEnvironmentCrn(ACCOUNT_ID,
                ENVIRONMENT_NAME, updateAzureResourceEncryptionDto);
        verify(environmentDtoConverter, times(1)).environmentToDto(env);
    }

    @Configuration
    @Import(EnvironmentModificationService.class)
    static class Config {
    }
}