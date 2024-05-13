package com.sequenceiq.cloudbreak.service.encryption;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EncryptionKeyServiceTest {

    private static final String CROSS_ACCOUNT_ROLE = "cross-account-role";

    private static final String LOGGER_INSTANCE_PROFILE = "logger-instance-profile";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "test_name";

    private static final String STACK_CRN = "test_crn";

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String ACCOUNT = "default";

    private static final String REGION = "region";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final String KEY_ARN_LUKS = "keyArnLuks";

    private static final String KEY_ARN_SECRET_MANAGER = "keyArnSecretManager";

    private static final String KEY_NAME_LUKS = "%s-%s-%s-luks";

    private static final String KEY_NAME_SECRET_MANAGER = "%s-%s-%s-cloudSecretManager";

    private static final String KEY_DESC_LUKS = "LUKS KMS Key for %s-%s-%s";

    private static final String KEY_DESC_SECRET_MANAGER = "Cloud Secret Manager KMS Key for %s-%s-%s";

    private static final Map<String, String> APPLICATION_TAGS = Map.of("appTag", "appTagValue");

    private static final Map<String, String> DEFAULT_TAGS = Map.of("defaultTag", "defaultTagValue");

    private static final List<String> CLOUD_IDENTITIES = List.of("idbroker", "logger");

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Mock
    private CredentialConverter credentialConverter;

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
    private EnvironmentClientService environmentClientService;

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @Mock
    private CredentialResponse credentialResponse;

    @Mock
    private Credential credential;

    @Mock
    private CloudEncryptionKey cloudEncryptionKey;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private StackEncryptionService stackEncryptionService;

    @Mock
    private CloudInformationDecorator cloudInformationDecorator;

    @InjectMocks
    private EncryptionKeyService underTest;

    @BeforeEach
    public void setUp() throws Exception {
        when(cloudInformationDecorator.getCredentialPrincipal(environment, stack)).thenReturn(Optional.of(CROSS_ACCOUNT_ROLE));
        when(cloudInformationDecorator.getCloudIdentities(environment, stack)).thenReturn(CLOUD_IDENTITIES);
        when(cloudInformationDecorator.getLoggerInstances(environment, stack)).thenReturn(List.of(LOGGER_INSTANCE_PROFILE));
        ReflectionTestUtils.setField(underTest, "cloudInformationDecoratorMap",
                Map.of(new CloudPlatformVariant(AwsConstants.AWS_PLATFORM, AwsConstants.AWS_DEFAULT_VARIANT), cloudInformationDecorator));
        when(environment.isEnableSecretEncryption()).thenReturn(true);
        when(environment.getCredential()).thenReturn(credentialResponse);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudConnector.encryptionResources()).thenReturn(encryptionResources);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getName()).thenReturn(STACK_NAME);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stack.getTenantName()).thenReturn(ACCOUNT);
        when(stack.getRegion()).thenReturn(REGION);
        when(stack.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(stack.getCloudPlatform()).thenReturn(CloudConstants.AWS);
        when(stack.getPlatformVariant()).thenReturn(CloudConstants.AWS);
        when(stack.getType()).thenReturn(DATALAKE);
        StackTags stackTags = mock(StackTags.class);
        when(stackTags.getApplicationTags()).thenReturn(APPLICATION_TAGS);
        when(stackTags.getDefaultTags()).thenReturn(DEFAULT_TAGS);
        Json json = mock(Json.class);
        when(json.get(StackTags.class)).thenReturn(stackTags);
        when(stack.getTags()).thenReturn(json);
        when(stackService.get(STACK_ID)).thenReturn(stack);
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID)).thenReturn(List.of());
        when(credentialConverter.convert(credentialResponse)).thenReturn(credential);
        when(extendedCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(encryptionResources.createEncryptionKey(any())).thenReturn(cloudEncryptionKey);
        when(cloudEncryptionKey.getName()).thenReturn(KEY_ARN_LUKS).thenReturn(KEY_ARN_SECRET_MANAGER);
    }

    @Test
    void testGenerateEncryptionKeysSecretEncryptionNotEnabled() {
        when(environment.isEnableSecretEncryption()).thenReturn(false);
        underTest.generateEncryptionKeys(STACK_ID);
        verify(environmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(credentialConverter, never()).convert(any(CredentialResponse.class));
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(resourceRetriever, never()).findAllByStatusAndTypeAndStack(any(), any(), any());
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(encryptionResources, never()).createEncryptionKey(any());
    }

    @Test
    void testGenerateEncryptionKeysCredentialPrincipalNotFound() {
        when(cloudInformationDecorator.getCredentialPrincipal(environment, stack)).thenReturn(Optional.empty());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.generateEncryptionKeys(STACK_ID));
        assertEquals("Unable to generate encryption keys for test_name.Credential principal not found", exception.getMessage());
        verify(environmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(credentialConverter, never()).convert(any(CredentialResponse.class));
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(resourceRetriever, never()).findAllByStatusAndTypeAndStack(any(), any(), any());
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(encryptionResources, never()).createEncryptionKey(any());
    }

    @Test
    void testGenerateEncryptionKeysPrincipalsNotFound() {
        when(cloudInformationDecorator.getCloudIdentities(environment, stack)).thenReturn(null);
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.generateEncryptionKeys(STACK_ID));
        assertEquals("Unable to generate encryption keys for test_name.Unable to determine principal Ids", exception.getMessage());
        verify(environmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(credentialConverter, never()).convert(any(CredentialResponse.class));
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(resourceRetriever, never()).findAllByStatusAndTypeAndStack(any(), any(), any());
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(encryptionResources, never()).createEncryptionKey(any());
    }

    @Test
    void testGenerateEncryptionKeysPrincipalsNotFoundUnsupportedClusterType() {
        when(stack.getType()).thenReturn(StackType.LEGACY);
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.generateEncryptionKeys(STACK_ID));
        assertEquals("Unable to generate encryption keys for test_name.Unsupported cluster type: LEGACY", exception.getMessage());
        verify(environmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(credentialConverter, never()).convert(any(CredentialResponse.class));
        verify(extendedCloudCredentialConverter, never()).convert(any());
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
        when(stack.getPlatformVariant()).thenReturn("AZURE");
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.generateEncryptionKeys(STACK_ID));
        assertEquals("Unable to generate encryption keys for test_name.Unsupported cloud platform and variant: {platform='AZURE', variant='AZURE'}",
                exception.getMessage());
        verify(environmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(credentialConverter, never()).convert(any(CredentialResponse.class));
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(resourceRetriever, never()).findAllByStatusAndTypeAndStack(any(), any(), any());
        verify(extendedCloudCredentialConverter, never()).convert(any());
        verify(encryptionResources, never()).createEncryptionKey(any());
    }

    @ParameterizedTest(name = "testGenerateEncryptionKeysSecretSuccess{0}")
    @EnumSource(value = StackType.class, names = {"WORKLOAD", "DATALAKE"}, mode = EnumSource.Mode.INCLUDE)
    void testGenerateEncryptionKeysSecretSuccess(StackType stackType) {
        when(stack.getType()).thenReturn(stackType);
        when(resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID)).thenReturn(List.of(cloudResource));
        underTest.generateEncryptionKeys(STACK_ID);
        verify(environmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(stackService).get(STACK_ID);
        verify(resourceRetriever).findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID);
        verify(credentialConverter).convert(credentialResponse);
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
        assertEquals(true, values.stream().map(EncryptionKeyCreationRequest::keyName)
                .anyMatch(keyName -> populateStackInformation(KEY_NAME_LUKS, stackType).equals(keyName)));
        assertEquals(true, values.stream().map(EncryptionKeyCreationRequest::keyName)
                .anyMatch(keyName -> populateStackInformation(KEY_NAME_SECRET_MANAGER, stackType).equals(keyName)));
        verifyEncryptionKeyCreationRequest(values.get(0), List.of(cloudResource), stackType);
        verifyEncryptionKeyCreationRequest(values.get(1), List.of(cloudResource), stackType);
    }

    @Test
    void testGenerateEncryptionKeysPropagateException() {
        doThrow(new RuntimeException("Unable to generate Keys")).when(resourceRetriever)
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.generateEncryptionKeys(STACK_ID));
        assertEquals("Unable to generate Keys", exception.getMessage());
        verify(environmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(stackService).get(STACK_ID);
        verify(resourceRetriever).findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.AWS_KMS_KEY, STACK_ID);
        verify(credentialConverter, never()).convert(credentialResponse);
        verify(extendedCloudCredentialConverter, never()).convert(credential);
    }

    private void verifyEncryptionKeyCreationRequest(EncryptionKeyCreationRequest encryptionKeyCreationRequest, List<CloudResource> cloudResources,
            StackType stackType) {
        boolean luksKey = isLuksKey(encryptionKeyCreationRequest.keyName());
        String expectedDescription = populateStackInformation(luksKey ? KEY_DESC_LUKS : KEY_DESC_SECRET_MANAGER, stackType);
        List<String> expectedPrincipalIds = luksKey ? (stackType == DATALAKE ? CLOUD_IDENTITIES : List.of(LOGGER_INSTANCE_PROFILE)) :
                List.of(CROSS_ACCOUNT_ROLE);
        assertEquals(extendedCloudCredential, encryptionKeyCreationRequest.cloudCredential());
        assertEquals(cloudResources, encryptionKeyCreationRequest.cloudResources());
        assertEquals(location(region(REGION), availabilityZone(AVAILABILITY_ZONE)), encryptionKeyCreationRequest.cloudContext().getLocation());
        assertEquals(encryptionKeyCreationRequest.tags(), expectedTags());
        assertEquals(expectedDescription, encryptionKeyCreationRequest.description());
        assertEquals(expectedPrincipalIds, encryptionKeyCreationRequest.cryptographicPrincipals());
    }

    private boolean isLuksKey(String keyName) {
        return keyName.endsWith("luks");
    }

    private String populateStackInformation(String formatString, StackType stackType) {
        return String.format(formatString, STACK_NAME, STACK_CRN, stackType.getResourceType());
    }

    private Map<String, String> expectedTags() {
        Map<String, String> tags = new HashMap<>(DEFAULT_TAGS);
        tags.putAll(APPLICATION_TAGS);
        return tags;
    }
}

