package com.sequenceiq.environment.environment.service;


import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.CRN;
import static com.sequenceiq.cloudbreak.util.TestConstants.USER;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.getCloudRegions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.AuthenticationDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.validation.EnvironmentValidatorService;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.service.LoadBalancerEntitlementService;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.service.ParametersService;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "environment.tunnel.ccm.validate.entitlement=true")
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
    private CloudNetworkService cloudNetworkService;

    @MockBean
    private LoadBalancerEntitlementService loadBalancerEntitlementService;

    @Inject
    private EnvironmentCreationService environmentCreationServiceUnderTest;

    @BeforeEach
    void setUp() {
        when(validatorService.validatePublicKey(any())).thenReturn(ValidationResult.empty());
        when(validatorService.validateTags(any())).thenReturn(ValidationResult.empty());
    }

    @Test
    void testCreateOccupied() {
        EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withAccountId(ACCOUNT_ID)
                .build();
        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));

        verify(validatorService, Mockito.times(0)).validatePublicKey(any());
        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any(), any());
        verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] tunnelingScenarios() {
        return new Object[][] {
                // tunnel                 override  CCMv2Enabled CCMv2JumpgateEnabled  valid  expectedTunnel  expectedThrowable  errorMessage
                { Tunnel.DIRECT,          false,    false,       false,                true,  Tunnel.DIRECT,          null,              null },
                { Tunnel.DIRECT,          false,    false,       true,                 true,  Tunnel.DIRECT,          null,              null },
                { Tunnel.DIRECT,          false,    true,        false,                true,  Tunnel.DIRECT,          null,              null },
                { Tunnel.DIRECT,          false,    true,        true,                 true,  Tunnel.DIRECT,          null,              null },
                { Tunnel.DIRECT,          true,     false,       false,                true,  Tunnel.DIRECT,          null,              null },
                { Tunnel.DIRECT,          true,     false,       true,                 true,  Tunnel.DIRECT,          null,              null },
                { Tunnel.DIRECT,          true,     true,        false,                true,  Tunnel.DIRECT,          null,              null },
                { Tunnel.DIRECT,          true,     true,        true,                 true,  Tunnel.DIRECT,          null,              null },

                { Tunnel.CLUSTER_PROXY,   false,    false,       false,                true,  Tunnel.CLUSTER_PROXY,   null,              null },
                { Tunnel.CLUSTER_PROXY,   false,    false,       true,                 true,  Tunnel.CLUSTER_PROXY,   null,              null },
                { Tunnel.CLUSTER_PROXY,   false,    true,        false,                true,  Tunnel.CLUSTER_PROXY,   null,              null },
                { Tunnel.CLUSTER_PROXY,   false,    true,        true,                 true,  Tunnel.CLUSTER_PROXY,   null,              null },
                { Tunnel.CLUSTER_PROXY,   true,     false,       false,                true,  Tunnel.CLUSTER_PROXY,   null,              null },
                { Tunnel.CLUSTER_PROXY,   true,     false,       true,                 true,  Tunnel.CLUSTER_PROXY,   null,              null },
                { Tunnel.CLUSTER_PROXY,   true,     true,        false,                true,  Tunnel.CLUSTER_PROXY,   null,              null },
                { Tunnel.CLUSTER_PROXY,   true,     true,        true,                 true,  Tunnel.CLUSTER_PROXY,   null,              null },

                { Tunnel.CCM,             false,    false,       false,                true,  Tunnel.CCM,             null,              null },
                { Tunnel.CCM,             false,    false,       true,                 true,  Tunnel.CCMV2_JUMPGATE,  null,              null },
                { Tunnel.CCM,             false,    true,        false,                true,  Tunnel.CCMV2,           null,              null },
                { Tunnel.CCM,             false,    true,        true,                 true,  Tunnel.CCMV2_JUMPGATE,  null,              null },
                { Tunnel.CCM,             true,     false,       false,                true,  Tunnel.CCM,             null,              null },
                { Tunnel.CCM,             true,     false,       true,                 true,  Tunnel.CCM,             null,              null },
                { Tunnel.CCM,             true,     true,        false,                true,  Tunnel.CCM,             null,              null },
                { Tunnel.CCM,             true,     true,        true,                 true,  Tunnel.CCM,             null,              null },

                { Tunnel.CCMV2,           false,    false,       false,                false, null,                   BadRequestException.class, "CCMV2 not enabled for account." },
                { Tunnel.CCMV2,           false,    false,       true,                 false, null,                   BadRequestException.class, "CCMV2 not enabled for account." },
                { Tunnel.CCMV2,           false,    true,        false,                true,  Tunnel.CCMV2,           null,              null },
                { Tunnel.CCMV2,           false,    true,        true,                 true,  Tunnel.CCMV2,           null,              null },
                { Tunnel.CCMV2,           true,     false,       false,                false, null,                   BadRequestException.class, "CCMV2 not enabled for account." },
                { Tunnel.CCMV2,           true,     false,       true,                 false, null,                   BadRequestException.class, "CCMV2 not enabled for account." },
                { Tunnel.CCMV2,           true,     true,        false,                true,  Tunnel.CCMV2,           null,              null },
                { Tunnel.CCMV2,           true,     true,        true,                 true,  Tunnel.CCMV2,           null,              null },

                { Tunnel.CCMV2_JUMPGATE,  false,    false,       false,                false, null,                   BadRequestException.class, "CCMV2 Jumpgate not enabled for account." },
                { Tunnel.CCMV2_JUMPGATE,  false,    false,       true,                 true , Tunnel.CCMV2_JUMPGATE,  null,              null },
                { Tunnel.CCMV2_JUMPGATE,  false,    true,        false,                false, null,                   BadRequestException.class, "CCMV2 Jumpgate not enabled for account." },
                { Tunnel.CCMV2_JUMPGATE,  false,    true,        true,                 true,  Tunnel.CCMV2_JUMPGATE,  null,              null },
                { Tunnel.CCMV2_JUMPGATE,  true,     false,       false,                false, null,                   BadRequestException.class, "CCMV2 Jumpgate not enabled for account." },
                { Tunnel.CCMV2_JUMPGATE,  true,     false,       true,                 true,  Tunnel.CCMV2_JUMPGATE,  null,              null },
                { Tunnel.CCMV2_JUMPGATE,  true,     true,        false,                false, null,                   BadRequestException.class, "CCMV2 Jumpgate not enabled for account." },
                { Tunnel.CCMV2_JUMPGATE,  true,     true,        true,                 true,  Tunnel.CCMV2_JUMPGATE,  null,              null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "Tunnel = {0}, Override = {1}, CCMv2Entitled = {2}, CMv2JumpgateEntitled = {3}, Valid = {4}")
    @MethodSource("tunnelingScenarios")
    void testCreateForCcmV2TunnelInitialization(Tunnel tunnel, boolean override, boolean ccmV2Entitled, boolean ccmv2JumpgateEntitled,
            boolean valid, Tunnel expectedTunnel, Class<? extends Throwable> expectedThrowable, String errorMessage) {
        EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME).withAccountId(ACCOUNT_ID).withAuthentication(AuthenticationDto.builder().build())
                .build();

        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        environment.setExperimentalFeaturesJson(ExperimentalFeatures.builder().withTunnel(tunnel).withOverrideTunnel(override).build());

        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentResourceService.getCredentialFromRequest(any(), any())).thenReturn(new Credential());
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateNetworkCreation(any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateFreeIpaCreation(any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(any())).thenReturn(getCloudRegions());
        when(environmentService.save(any(Environment.class))).thenReturn(environment);
        when(entitlementService.ccmV2Enabled(ACCOUNT_ID)).thenReturn(ccmV2Entitled);
        when(entitlementService.ccmV2JumpgateEnabled(ACCOUNT_ID)).thenReturn(ccmv2JumpgateEntitled);

        if (valid) {
            environmentCreationServiceUnderTest.create(environmentCreationDto);

            ArgumentCaptor<Environment> captor = ArgumentCaptor.forClass(Environment.class);
            verify(environmentService).save(captor.capture());
            Environment capturedEnvironment = captor.getValue();
            assertEquals(expectedTunnel, capturedEnvironment.getExperimentalFeaturesJson().getTunnel(), "Tunnel should be " + expectedTunnel);
        } else {
            assertThatThrownBy(() -> environmentCreationServiceUnderTest.create(environmentCreationDto))
                    .isInstanceOf(expectedThrowable)
                    .hasMessage(errorMessage);

            verify(environmentService, never()).save(any());
            verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any(), any());
            verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
        }
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
        when(environmentResourceService.getCredentialFromRequest(any(), any()))
                .thenReturn(credential);
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateNetworkCreation(any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateFreeIpaCreation(any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(environmentService.save(any())).thenReturn(environment);
        when(entitlementService.azureEnabled(eq(ACCOUNT_ID))).thenReturn(false);

        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));

        verify(validatorService, Mockito.times(1)).validatePublicKey(any());
        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any(), any());
        verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    @Test
    void testCreate() {
        ParametersDto parametersDto = ParametersDto.builder().withAwsParameters(AwsParametersDto.builder().withDynamoDbTableName("dynamo").build()).build();
        String environmentCrn = "crn";
        final EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withCreator(CRN)
                .withAccountId(ACCOUNT_ID)
                .withCrn(environmentCrn)
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
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID)))
                .thenReturn(credential);
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateNetworkCreation(any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateFreeIpaCreation(any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(environmentService.save(any())).thenReturn(environment);

        environmentCreationServiceUnderTest.create(environmentCreationDto);

        verify(validatorService, Mockito.times(1)).validatePublicKey(any());
        verify(environmentService).save(any());
        verify(parametersService).saveParameters(eq(environment), eq(parametersDto));
        verify(environmentResourceService).createAndSetNetwork(any(), any(), any(), any(), any());
        verify(reactorFlowManager).triggerCreationFlow(eq(1L), eq(ENVIRONMENT_NAME), eq(CRN), anyString());
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
                .withCrn("aCrn")
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
        String parentEnvironmentResourceCrn = "ParentEnvironmentResourceCrn";
        parentEnvironment.setResourceCrn(parentEnvironmentResourceCrn);

        Credential credential = new Credential();
        credential.setCloudPlatform("platform");

        ArgumentCaptor<Environment> environmentArgumentCaptor = ArgumentCaptor.forClass(Environment.class);

        when(environmentService.findByNameAndAccountIdAndArchivedIsFalse(any(), eq(ACCOUNT_ID))).thenReturn(Optional.of(parentEnvironment));
        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID)))
                .thenReturn(credential);
        when(validatorService.validateNetworkCreation(any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateFreeIpaCreation(any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(environmentService.save(environmentArgumentCaptor.capture())).thenReturn(environment);

        environmentCreationServiceUnderTest.create(environmentCreationDto);

        verify(validatorService, Mockito.times(1)).validatePublicKey(any());
        verify(environmentService).save(any());
        verify(parametersService).saveParameters(eq(environment), eq(parametersDto));
        verify(environmentResourceService).createAndSetNetwork(any(), any(), any(), any(), any());
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

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        validationResultBuilder.error("error");
        when(validatorService.validateNetworkCreation(any(), any())).thenReturn(validationResultBuilder);
        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID)))
                .thenReturn(credential);
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateFreeIpaCreation(any())).thenReturn(ValidationResult.builder().build());
        when(validationResult.merge(any())).thenReturn(ValidationResult.builder().error("nogood"));
        when(environmentService.save(any())).thenReturn(environment);

        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));

        verify(validatorService, Mockito.times(1)).validatePublicKey(any());
        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any(), any());
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

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        validationResultBuilder.error("error");

        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), eq(ACCOUNT_ID)))
                .thenReturn(credential);
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(validatorService.validateNetworkCreation(any(), any())).thenReturn(validationResultBuilder);
        when(environmentService.getRegionsByEnvironment(eq(environment))).thenReturn(getCloudRegions());
        when(environmentDtoConverter.environmentToLocationDto(any())).thenReturn(LocationDto.builder().withName("loc").build());
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateFreeIpaCreation(any())).thenReturn(ValidationResult.builder().build());
        when(validationResult.merge(any())).thenReturn(ValidationResult.builder().error("nogood"));
        when(environmentService.save(any())).thenReturn(environment);

        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));

        verify(validatorService, Mockito.times(1)).validatePublicKey(any());
        verify(environmentService, never()).save(any());
        verify(environmentResourceService, never()).createAndSetNetwork(any(), any(), any(), any(), any());
        verify(reactorFlowManager, never()).triggerCreationFlow(anyLong(), eq(ENVIRONMENT_NAME), eq(USER), anyString());
    }

    @Test
    void testEncryptionKeyValidationError() {
        final EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform("GCP")
                .withCreator(CRN)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        Credential credential = new Credential();
        credential.setCloudPlatform("GCP");

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        validationResultBuilder.error("error");
        when(validatorService.validateEncryptionKeyUrl(any(), any())).thenReturn(validationResultBuilder.build());

        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), any())).thenReturn(credential);
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateNetworkCreation(any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateFreeIpaCreation(any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(entitlementService.azureEnabled(eq(ACCOUNT_ID))).thenReturn(true);
        when(environmentService.save(any())).thenReturn(environment);

        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));
    }

    @Test
    void testEncryptionKeyArnValidationError() {
        final EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform("AWS")
                .withCreator(CRN)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .withParameters(ParametersDto.builder()
                        .withAwsParameters(AwsParametersDto.builder()
                                .withAwsDiskEncryptionParameters(AwsDiskEncryptionParametersDto.builder()
                                        .withEncryptionKeyArn("dummy-key-arn")
                                        .build())
                                .build())
                        .build())
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        Credential credential = new Credential();
        credential.setCloudPlatform("AWS");

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        validationResultBuilder.error("error");
        when(validatorService.validateEncryptionKeyArn(any(), any())).thenReturn(validationResultBuilder.build());

        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), any())).thenReturn(credential);
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateNetworkCreation(any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateFreeIpaCreation(any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(environmentService.save(any())).thenReturn(environment);

        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));
    }

    @Test
    void testEncryptionKeyUrlValidationError() {
        final EnvironmentCreationDto environmentCreationDto = EnvironmentCreationDto.builder()
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform("AZURE")
                .withCreator(CRN)
                .withAccountId(ACCOUNT_ID)
                .withAuthentication(AuthenticationDto.builder().build())
                .withParameters(ParametersDto.builder()
                        .withAzureParameters(AzureParametersDto.builder()
                                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                        .withEncryptionKeyUrl("dummy-key-url")
                                        .build())
                                .build())
                        .build())
                .build();
        final Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setId(1L);
        environment.setAccountId(ACCOUNT_ID);
        Credential credential = new Credential();
        credential.setCloudPlatform("AZURE");

        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        validationResultBuilder.error("error");
        when(validatorService.validateEncryptionKeyUrl(any(), any())).thenReturn(validationResultBuilder.build());

        when(environmentService.isNameOccupied(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(false);
        when(environmentDtoConverter.creationDtoToEnvironment(eq(environmentCreationDto))).thenReturn(environment);
        when(environmentResourceService.getCredentialFromRequest(any(), any())).thenReturn(credential);
        when(validatorService.validateParentChildRelation(any(), any())).thenReturn(ValidationResult.builder().build());
        when(validatorService.validateNetworkCreation(any(), any())).thenReturn(ValidationResult.builder());
        when(validatorService.validateFreeIpaCreation(any())).thenReturn(ValidationResult.builder().build());
        when(authenticationDtoConverter.dtoToAuthentication(any())).thenReturn(new EnvironmentAuthentication());
        when(entitlementService.azureEnabled(eq(ACCOUNT_ID))).thenReturn(true);
        when(environmentService.save(any())).thenReturn(environment);

        assertThrows(BadRequestException.class, () -> environmentCreationServiceUnderTest.create(environmentCreationDto));
    }

    @Configuration
    @Import(EnvironmentCreationService.class)
    static class Config {
    }

}
