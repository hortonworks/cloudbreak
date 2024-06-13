package com.sequenceiq.freeipa.service.encryption;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EncryptionKeyServiceTest {

    private static final String CROSS_ACCOUNT_ROLE = "cross-account-role";

    private static final String LOGGER_INSTANCE_PROFILE = "logger-instance-profile";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "freeipa_stack";

    private static final String STACK_CRN = "freeipa_crn";

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String ACCOUNT = "default";

    private static final String REGION = "region";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final String KEY_ARN_LUKS = "keyArnLuks";

    private static final String KEY_ARN_SECRET_MANAGER = "keyArnSecretManager";

    private static final String KEY_NAME_LUKS = "freeipa_stack-freeipa_crn-freeipa-luks";

    private static final String KEY_NAME_SECRET_MANAGER = "freeipa_stack-freeipa_crn-freeipa-cloudSecretManager";

    private static final String KEY_DESC_LUKS = "LUKS KMS Key for freeipa_stack-freeipa_crn";

    private static final String KEY_DESC_SECRET_MANAGER = "Cloud Secret Manager KMS Key for freeipa_stack-freeipa_crn";

    private static final Map<String, String> APPLICATION_TAGS = Map.of("appTag", "appTagValue");

    private static final Map<String, String> DEFAULT_TAGS = Map.of("defaultTag", "defaultTagValue");

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private CredentialService credentialService;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private EncryptionResources encryptionResources;

    @Mock
    private Stack stack;

    @Mock
    private StackService stackService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @Mock
    private Credential credential;

    @Mock
    private CloudEncryptionKey cloudEncryptionKey;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private StackEncryptionService stackEncryptionService;

    @InjectMocks
    private EncryptionKeyService underTest;

    @BeforeEach
    public void setUp() throws Exception {
        setUpEnvironment();
        when(cachedEnvironmentClientService.getByCrn(any())).thenReturn(environment);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudConnector.encryptionResources()).thenReturn(encryptionResources);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stack.getAccountId()).thenReturn(ACCOUNT);
        when(stack.getRegion()).thenReturn(REGION);
        when(stack.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        StackTags stackTags = mock(StackTags.class);
        when(stackTags.getApplicationTags()).thenReturn(APPLICATION_TAGS);
        when(stackTags.getDefaultTags()).thenReturn(DEFAULT_TAGS);
        Json json = mock(Json.class);
        when(json.get(StackTags.class)).thenReturn(stackTags);
        when(stack.getTags()).thenReturn(json);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID)).thenReturn(List.of());
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);
        when(extendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(encryptionResources.createEncryptionKey(any())).thenReturn(cloudEncryptionKey);
        when(cloudEncryptionKey.getName()).thenReturn(KEY_ARN_LUKS).thenReturn(KEY_ARN_SECRET_MANAGER);
    }

    @Test
    void testGenerateEncryptionKeysSecretEncryptionNotEnabled() {
        when(environment.isEnableSecretEncryption()).thenReturn(false);
        underTest.generateEncryptionKeys(1L);
        verify(cachedEnvironmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(credentialService, never()).getCredentialByEnvCrn(anyString());
        verify(resourceRetriever, never()).findAllByStatusAndTypeAndStack(any(), any(), any());
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(encryptionResources, never()).createEncryptionKey(any());
    }

    @Test
    void testGenerateEncryptionKeysLoggerInstanceProfileNotFound() {
        when(environment.getTelemetry()).thenReturn(null);
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.generateEncryptionKeys(STACK_ID));
        assertEquals("Unable to generate encryption keys for freeipa_stack.Logger instance profile not found", exception.getMessage());
        verify(cachedEnvironmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(credentialService, never()).getCredentialByEnvCrn(anyString());
        verify(resourceRetriever, never()).findAllByStatusAndTypeAndStack(any(), any(), any());
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(encryptionResources, never()).createEncryptionKey(any());
    }

    @Test
    void testGenerateEncryptionKeysCrossAccountRoleNotFound() {
        when(environment.getCredential()).thenReturn(null);
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.generateEncryptionKeys(STACK_ID));
        assertEquals("Unable to generate encryption keys for freeipa_stack.Cross Account role not found", exception.getMessage());
        verify(cachedEnvironmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(credentialService, never()).getCredentialByEnvCrn(anyString());
        verify(resourceRetriever, never()).findAllByStatusAndTypeAndStack(any(), any(), any());
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(encryptionResources, never()).createEncryptionKey(any());
    }

    @Test
    void testGenerateEncryptionKeysUnSupportedPlatform() {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudConnector.encryptionResources()).thenReturn(null);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.generateEncryptionKeys(STACK_ID));
        assertEquals("Unable to generate encryption keys for freeipa_stack.Unsupported cloud platform: AZURE", exception.getMessage());
        verify(credentialService).getCredentialByEnvCrn(ENVIRONMENT_CRN);
        verify(cachedEnvironmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(stackService).getStackById(STACK_ID);
        verify(resourceRetriever).findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID);
        verify(credentialService).getCredentialByEnvCrn(ENVIRONMENT_CRN);
        verify(extendedCloudCredentialConverter).convert(credential);
    }

    @Test
    void testGenerateEncryptionKeysSecretSuccess() {
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID)).thenReturn(List.of(cloudResource));
        underTest.generateEncryptionKeys(STACK_ID);
        verify(cachedEnvironmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(stackService).getStackById(STACK_ID);
        verify(resourceRetriever).findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID);
        verify(credentialService).getCredentialByEnvCrn(ENVIRONMENT_CRN);
        verify(extendedCloudCredentialConverter).convert(credential);
        ArgumentCaptor<StackEncryption> stackEncryptionArgumentCaptor = ArgumentCaptor.forClass(StackEncryption.class);
        verify(stackEncryptionService).save(stackEncryptionArgumentCaptor.capture());
        StackEncryption stackEncryption = stackEncryptionArgumentCaptor.getValue();
        assertEquals(STACK_ID, stackEncryption.getStackId());
        assertEquals(KEY_ARN_LUKS, stackEncryption.getEncryptionKeyLuks());
        assertEquals(KEY_ARN_SECRET_MANAGER, stackEncryption.getEncryptionKeyCloudSecretManager());
        assertEquals(ACCOUNT, stackEncryption.getAccountId());

        ArgumentCaptor<EncryptionKeyCreationRequest> resultCaptor = ArgumentCaptor.forClass(EncryptionKeyCreationRequest.class);
        verify(encryptionResources, times(2)).createEncryptionKey(resultCaptor.capture());
        List<EncryptionKeyCreationRequest> values = resultCaptor.getAllValues();
        assertEquals(2, values.size());
        assertTrue(values.stream().map(EncryptionKeyCreationRequest::keyName)
                .anyMatch(KEY_NAME_LUKS::equals));
        assertTrue(values.stream().map(EncryptionKeyCreationRequest::keyName)
                .anyMatch(KEY_NAME_SECRET_MANAGER::equals));
        verifyEncryptionKeyCreationRequest(values.get(0), List.of(cloudResource));
        verifyEncryptionKeyCreationRequest(values.get(1), List.of(cloudResource));
    }

    @Test
    void testGenerateEncryptionKeysPropagateException() {
        doThrow(new RuntimeException("Unable to generate Keys")).when(resourceRetriever)
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.generateEncryptionKeys(STACK_ID));
        assertEquals("Unable to generate Keys", exception.getMessage());
        verify(cachedEnvironmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(stackService).getStackById(STACK_ID);
        verify(resourceRetriever).findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID);
        verify(credentialService, never()).getCredentialByEnvCrn(ENVIRONMENT_CRN);
        verify(extendedCloudCredentialConverter, never()).convert(credential);
    }

    private void setUpEnvironment() {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = mock(S3CloudStorageV1Parameters.class);
        when(s3CloudStorageV1Parameters.getInstanceProfile()).thenReturn(LOGGER_INSTANCE_PROFILE);
        LoggingResponse loggingResponse = mock(LoggingResponse.class);
        when(loggingResponse.getS3()).thenReturn(s3CloudStorageV1Parameters);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        when(telemetryResponse.getLogging()).thenReturn(loggingResponse);
        when(environment.getTelemetry()).thenReturn(telemetryResponse);
        when(environment.isEnableSecretEncryption()).thenReturn(true);

        RoleBasedParameters roleBasedParameters = mock(RoleBasedParameters.class);
        when(roleBasedParameters.getRoleArn()).thenReturn(CROSS_ACCOUNT_ROLE);
        AwsCredentialParameters awsCredentialParameters = mock(AwsCredentialParameters.class);
        when(awsCredentialParameters.getRoleBased()).thenReturn(roleBasedParameters);
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getAws()).thenReturn(awsCredentialParameters);
        when(environment.getCredential()).thenReturn(credentialResponse);
    }

    private void verifyEncryptionKeyCreationRequest(EncryptionKeyCreationRequest encryptionKeyCreationRequest, List<CloudResource> cloudResources) {
        boolean luksKey = isLuksKey(encryptionKeyCreationRequest.keyName());
        String expectedDescription = luksKey ? KEY_DESC_LUKS : KEY_DESC_SECRET_MANAGER;
        List<String> expectedCryptographicPrincipals = luksKey ? List.of(LOGGER_INSTANCE_PROFILE) : List.of(CROSS_ACCOUNT_ROLE, LOGGER_INSTANCE_PROFILE);
        assertEquals(extendedCloudCredential, encryptionKeyCreationRequest.cloudCredential());
        assertEquals(cloudResources, encryptionKeyCreationRequest.cloudResources());
        assertEquals(location(region(REGION), availabilityZone(AVAILABILITY_ZONE)), encryptionKeyCreationRequest.cloudContext().getLocation());
        assertEquals(encryptionKeyCreationRequest.tags(), expectedTags());
        assertEquals(expectedDescription, encryptionKeyCreationRequest.description());
        assertEquals(expectedCryptographicPrincipals, encryptionKeyCreationRequest.cryptographicPrincipals());
    }

    private boolean isLuksKey(String keyName) {
        return KEY_NAME_LUKS.equals(keyName);
    }

    private Map<String, String> expectedTags() {
        Map<String, String> tags = new HashMap<>(DEFAULT_TAGS);
        tags.putAll(APPLICATION_TAGS);
        return tags;
    }
}
